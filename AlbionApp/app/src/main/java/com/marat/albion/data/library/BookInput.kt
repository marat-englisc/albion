package com.marat.albion.data.library

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

class BookImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Общие ограничения проверяются во время чтения, а не после выделения всей памяти. */
internal object BookInput {
    const val MAX_FILE_BYTES = 30 * 1024 * 1024
    const val MAX_TEXT_CHARS = 5_000_000
    const val MAX_PDF_PAGES = 2000
    const val VERSION = 2

    fun read(input: InputStream, limit: Int = MAX_FILE_BYTES): ByteArray =
        readBounded(input, limit, "Файл слишком большой: поддерживаются книги до 30 МБ")

    fun readBounded(input: InputStream, limit: Int, message: String): ByteArray {
        val out = ByteArrayOutputStream(minOf(limit, 8192))
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            // Читаем не больше одного лишнего байта, даже если размер файла неизвестен.
            val n = input.read(buffer, 0, minOf(buffer.size, limit - total + 1))
            if (n < 0) break
            if (n == 0) {
                val byte = input.read()
                if (byte < 0) break
                if (++total > limit) throw BookImportException(message)
                out.write(byte)
            } else {
                total += n
                if (total > limit) throw BookImportException(message)
                out.write(buffer, 0, n)
            }
        }
        return out.toByteArray()
    }

    fun decodeText(bytes: ByteArray): String {
        val (charset, offset) = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE to 2
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE to 2
            else -> Charsets.UTF_8 to 0
        }
        val text = try {
            charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, offset, bytes.size - offset)).toString().removePrefix("\uFEFF")
        } catch (e: CharacterCodingException) {
            throw BookImportException("Не удалось прочитать кодировку текста. Сохраните книгу в UTF-8 или UTF-16 с BOM", e)
        }
        validateText(text)
        return text
    }

    fun validateText(text: String) {
        if (text.length > MAX_TEXT_CHARS) throw BookImportException("В книге слишком много текста: предел — 5 млн символов")
        if (text.any { it.isISOControl() && it !in "\t\r\n\u000C" }) {
            throw BookImportException("Файл не похож на текстовую книгу. Поддерживаются TXT, MD, EPUB и текстовый PDF")
        }
    }

    fun fingerprint(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun title(fileName: String) = fileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim().ifEmpty { "Без названия" }
}
