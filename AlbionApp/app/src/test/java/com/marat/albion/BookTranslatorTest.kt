package com.marat.albion

import com.marat.albion.data.library.BookTranslationBackend
import com.marat.albion.data.library.BookTranslationPhase
import com.marat.albion.data.library.BookTranslator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BookTranslatorTest {
    private class Backend : BookTranslationBackend {
        var preparations = 0
        var calls = mutableListOf<String>()
        var prepare: suspend () -> Unit = {}
        var answer: (String) -> String = { "перевод: $it" }
        override suspend fun prepare() { preparations++; prepare.invoke() }
        override suspend fun translate(text: String): String { calls += text; return answer(text) }
    }

    @Test fun concurrentRequestsShareModelPreparationAndCacheTheSelectedText(): Unit = runBlocking {
        val started = CompletableDeferred<Unit>()
        val downloaded = CompletableDeferred<Unit>()
        val backend = Backend().apply { prepare = { started.complete(Unit); downloaded.await() } }
        val translator = BookTranslator(this, backend)
        val word = async { translator.translate(" hello ") }
        started.await()
        val phrase = async { translator.translate("a little every day") }
        downloaded.complete(Unit)
        assertEquals("перевод: hello", word.await())
        assertEquals("перевод: a little every day", phrase.await())
        assertEquals("перевод: hello", translator.translate("hello"))
        assertEquals(1, backend.preparations)
        assertEquals(listOf("hello", "a little every day"), backend.calls)
        assertTrue(translator.state.value.ready)
    }

    @Test fun failedFirstDownloadCanBeRetried(): Unit = runBlocking {
        val backend = Backend().apply { prepare = { error("No network") } }
        val translator = BookTranslator(this, backend)
        try { translator.translate("hello"); fail("Download failure was ignored") }
        catch (_: IllegalStateException) { }
        assertEquals(BookTranslationPhase.Failed, translator.state.value.phase)
        assertTrue(backend.calls.isEmpty())
        backend.prepare = {}
        assertEquals("перевод: hello", translator.translate("hello"))
        assertEquals(2, backend.preparations)
        assertTrue(translator.state.value.ready)
    }

    @Test fun leavingTheReaderDuringPreparationDoesNotPoisonTheNextRequest(): Unit = runBlocking {
        val started = CompletableDeferred<Unit>()
        val backend = Backend().apply { prepare = { started.complete(Unit); CompletableDeferred<Unit>().await() } }
        val translator = BookTranslator(this, backend)
        val abandoned = async { translator.translate("hello") }
        started.await()
        abandoned.cancelAndJoin()
        assertNotEquals(BookTranslationPhase.Failed, translator.state.value.phase)
        backend.prepare = {}
        assertEquals("перевод: goodbye", translator.translate("goodbye"))
        assertEquals(listOf("goodbye"), backend.calls)
    }

    @Test fun invalidSelectionsDoNotStartADownload(): Unit = runBlocking {
        val backend = Backend()
        val translator = BookTranslator(this, backend)
        for (text in listOf("  ", "a".repeat(BookTranslator.MAX_SELECTION + 1))) {
            try { translator.translate(text); fail("Invalid selection accepted") }
            catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, backend.preparations)
    }

    @Test fun emptyOrFailedTranslationsAreNeverCached(): Unit = runBlocking {
        val backend = Backend().apply { answer = { " " } }
        val translator = BookTranslator(this, backend)
        try { translator.translate("hello"); fail("Empty result accepted") }
        catch (_: IllegalStateException) { }
        backend.answer = { error("Translation failed") }
        try { translator.translate("hello"); fail("Failure accepted") }
        catch (_: IllegalStateException) { }
        backend.answer = { "привет" }
        assertEquals("привет", translator.translate("hello"))
        assertEquals(3, backend.calls.size)
        assertEquals(1, backend.preparations)
    }
}
