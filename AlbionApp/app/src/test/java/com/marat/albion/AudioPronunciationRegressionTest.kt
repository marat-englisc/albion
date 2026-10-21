package com.marat.albion

import com.marat.albion.data.content.AudioIndex
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Проверяет именно содержимое записей: правильного имени файла недостаточно. */
class AudioPronunciationRegressionTest {
    private val audio = File("../../audio").canonicalFile

    private fun bytes(name: String): ByteArray {
        val file = File(audio, name)
        assumeTrue("Audio content is not available: $file", file.exists())
        return file.readBytes()
    }

    @Test fun sunContainsTheConfirmedSunSonPronunciation() {
        assertArrayEquals(bytes("words/son.mp3"), bytes("words/sun.mp3"))
        assertFalse(bytes("words/sun.mp3").contentEquals(bytes("words/Sunday.mp3")))
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes("words/sun.mp3"))
            .joinToString("") { "%02x".format(it) }
        assertEquals("12024b2103893effbbfb17bc834314b5ed791b3771e4549e947f617487ea9a6d", digest)
    }

    @Test fun pastFormsUseConfirmedPronunciationsWithoutSpokenSlashes() {
        assertArrayEquals(bytes("words/red.mp3"), bytes("irregular_verbs/read (past).mp3"))
        assertFalse(bytes("irregular_verbs/ate (eit).mp3").contentEquals(bytes("irregular_verbs/ate.mp3")))
        val index = AudioIndex(listOf("read.mp3", "read (past).mp3", "ate.mp3", "ate (eit).mp3"))
        assertEquals(listOf("read (past).mp3"), index.forVerbForm("read", isPast = true))
        assertEquals(listOf("read.mp3"), index.forVerbForm("read", isPast = false))
    }

    @Test fun wordLookupNeverUsesAFileForALongerWord() {
        val index = AudioIndex(listOf("Sunday.mp3", "sunny.mp3", "Saturday.mp3", "Wednesday.mp3"))
        assertNull(index.forTerm("sun"))
        assertTrue(index.forVerbForm("sat", isPast = true).isEmpty())
        assertTrue(index.forVerbForm("wed", isPast = false).isEmpty())
    }

    @Test fun phonemeRepairsCannotRevertToExpandedWeekdaysOrSpokenSlashes() {
        val approved = mapOf(
            "irregular_verbs/sat.mp3" to "3136db4b0d8bd6059d38655a2ed32f27b40cffb92a98276fd259bde8d60575bf",
            "irregular_verbs/wed.mp3" to "109953157919ea8524ca3788f9ded4552d2d6259d212847678217e9bf2cb9187",
            "irregular_verbs/ate.mp3" to "c8937c8e4085f816b4e24a1ad3cae206829dccac4b45abac88810988bfbbc06b",
            "words/art.mp3" to "634776ee01c2246184918cc857cb480699bc94797b9a6cd4be705b82f34ed1f3",
            "words/mass.mp3" to "12169e7a9333f9f562917b290e7007f0943c411c376354a9d6c664e3e3417b12",
            "words/apart.mp3" to "017b2a085f67246faddd242d9db1d5d25482f80b6ab95261e77364c3f9212543",
            "words/assist.mp3" to "0669139bc692e44de663b7489e666494f6ea126fedcf2070557ad962ad902d2c",
        )
        approved.forEach { (name, expected) ->
            val actual = MessageDigest.getInstance("SHA-256").digest(bytes(name))
                .joinToString("") { "%02x".format(it) }
            assertEquals("Approved phoneme recording: $name", expected, actual)
        }
    }
}
