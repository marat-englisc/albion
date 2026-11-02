package com.marat.albion.data.translation

import java.io.File
import java.io.RandomAccessFile

/** Bounded metadata reader. Reject incompatible models before allocating inference memory. */
object GgufFile {
    fun requireQwen35(file: File) {
        RandomAccessFile(file, "r").use { input ->
            fun long(): Long = java.lang.Long.reverseBytes(input.readLong()).also { require(it >= 0) { "Повреждён заголовок GGUF." } }
            fun int() = Integer.reverseBytes(input.readInt())
            fun skip(bytes: Long) {
                require(bytes >= 0 && bytes <= input.length() - input.filePointer) { "Файл GGUF обрезан." }
                input.seek(input.filePointer + bytes)
            }
            fun string(): String {
                val size = long()
                require(size <= 1_048_576 && size <= input.length() - input.filePointer) { "Повреждены метаданные GGUF." }
                return ByteArray(size.toInt()).also(input::readFully).toString(Charsets.UTF_8)
            }
            fun fixedSize(type: Int): Long = when (type) {
                0, 1, 7 -> 1; 2, 3 -> 2; 4, 5, 6 -> 4; 10, 11, 12 -> 8
                else -> error("Неизвестный тип метаданных GGUF.")
            }
            fun skipValue(type: Int) {
                when (type) {
                    8 -> skip(long())
                    9 -> {
                        val element = int()
                        val count = long()
                        require(count <= 2_000_000) { "Слишком большой массив метаданных GGUF." }
                        if (element == 8) repeat(count.toInt()) { skip(long()) }
                        else skip(Math.multiplyExact(count, fixedSize(element)))
                    }
                    else -> skip(fixedSize(type))
                }
            }
            require(input.length() >= 24 && input.readInt() == 0x47475546) { "Выберите файл модели в формате GGUF." }
            require(int() in 2..3) { "Неподдерживаемая версия GGUF." }
            require(long() > 0) { "GGUF не содержит весов модели." }
            val metadata = long()
            require(metadata in 1..1_000_000) { "Повреждены метаданные GGUF." }
            repeat(metadata.coerceAtMost(256).toInt()) {
                val key = string()
                val type = int()
                if (key == "general.architecture") {
                    require(type == 8 && string() == "qwen35") { "Нужна текстовая GGUF-модель Qwen3.5 (0.8B или 2B), без mmproj." }
                    return
                }
                skipValue(type)
            }
            error("В GGUF не найдена архитектура Qwen3.5.")
        }
    }
}
