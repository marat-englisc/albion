package com.marat.albion

import com.marat.albion.data.translation.GgufFile
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

class GgufFileTest {
    private fun fixture(architecture: String, modify: (ByteArray) -> ByteArray = { it }): File {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            fun int(value: Int) = out.writeInt(Integer.reverseBytes(value))
            fun long(value: Long) = out.writeLong(java.lang.Long.reverseBytes(value))
            fun string(value: String) { val encoded = value.toByteArray(); long(encoded.size.toLong()); out.write(encoded) }
            out.writeBytes("GGUF"); int(3); long(1); long(2)
            string("general.alignment"); int(4); int(32)
            string("general.architecture"); int(8); string(architecture)
        }
        return File.createTempFile("albion-model-", ".gguf").apply { writeBytes(modify(bytes.toByteArray())); deleteOnExit() }
    }
    @Test fun denseQwen35MetadataIsAccepted() { GgufFile.requireQwen35(fixture("qwen35")) }
    @Test fun otherModelsAndProjectorFilesAreRejected() {
        for (architecture in listOf("llama", "qwen3", "qwen35moe", "clip")) {
            try { GgufFile.requireQwen35(fixture(architecture)); fail("Accepted $architecture") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun truncatedAndMalformedHeadersAreRejected() {
        for (file in listOf(fixture("qwen35") { it.copyOf(12) }, fixture("qwen35") { it.copyOf(it.size - 2) },
            fixture("qwen35") { it.apply { this[4] = 99 } })) {
            try { GgufFile.requireQwen35(file); fail("Malformed model accepted") }
            catch (_: Exception) { }
        }
    }
    @Test fun oversizedStringsAreRejectedBeforeAllocation() {
        val file = fixture("qwen35") { it.apply { for (i in 24..31) this[i] = 127 } }
        try { GgufFile.requireQwen35(file); fail("Oversized metadata accepted") }
        catch (_: IllegalArgumentException) { }
    }
}
