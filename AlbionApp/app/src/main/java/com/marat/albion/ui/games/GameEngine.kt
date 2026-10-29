package com.marat.albion.ui.games

import kotlinx.coroutines.flow.map

import com.marat.albion.ui.theme.paperSurface
import android.os.SystemClock

import androidx.activity.compose.BackHandler
import com.marat.albion.ui.theme.QuietAnimatedContent as AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marat.albion.AppContainer
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.settings.PracticeOptions
import com.marat.albion.data.games.SentencePuzzle
import com.marat.albion.data.games.TenseDrill
import com.marat.albion.data.games.TenseTask
import com.marat.albion.ui.components.AlbionLogo
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

enum class Phase { Setup, Loading, Playing, Summary }

/** Общее состояние мини-игры: настройка → вопросы → итог. На FSRS не влияет. */
class GameViewModel(private val c: AppContainer, val game: Game,
    private val initialDeck: Deck? = null, private val focusMistakes: Boolean = false, private val initialCount: Int? = null,
) : ViewModel() {
    var phase by mutableStateOf(Phase.Loading)
        private set
    var deck by mutableStateOf(game.decks.first())
    var count by mutableIntStateOf(if (game == Game.IRREGULAR) 20 else 10)
    /** Режим игры (для «Тренажёра времён»: 0 — выбор, 1 — ввод). */
    var mode by mutableIntStateOf(0)
    /** Играть только слова, на которых вы ошибались в этой игре. */
    var onlyMistakes by mutableStateOf(false)
    private val needAudio get() = game == Game.SHADOWING || game == Game.PRONUNCIATION

    /** Сколько ошибок накоплено в выбранной колоде — для экрана настройки. */
    fun observeAvailable(deck: com.marat.albion.data.content.Deck) = c.study.observeSummaries().map { list -> list.firstOrNull { it.deck == deck }?.total ?: 0 }
    fun observeMistakes(deck: com.marat.albion.data.content.Deck) = c.study.observeGameMistakes(deck, game.name, needAudio)
    var items by mutableStateOf<List<CardEntity>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var correct by mutableIntStateOf(0)
        private set
    val mistakes = mutableStateListOf<Mistake>()
    private val missed = mutableListOf<CardEntity>()
    private var answered = false
    private var questionStartedAt = 0L

    init {
        viewModelScope.launch {
            val saved = c.settings.practiceOptions(game.name, defaultCount = count).first()
            deck = game.decks.firstOrNull { it.id == saved.deckId } ?: game.decks.first()
            val counts = if (game == Game.IRREGULAR) listOf(10, 20, Int.MAX_VALUE) else listOf(10, 20, 30)
            count = saved.count.takeIf { it in counts } ?: 10
            mode = saved.mode.coerceIn(0, (game.modes.size - 1).coerceAtLeast(0))
            initialDeck?.takeIf { it in game.decks }?.let { deck = it }
            initialCount?.takeIf { it in counts }?.let { count = it }
            onlyMistakes = focusMistakes
            phase = Phase.Setup
        }
    }

    fun saveOptions(markAsLast: Boolean = false) {
        val options = PracticeOptions(deckId = deck.id, count = count, mode = mode)
        c.appScope.launch(Dispatchers.Main.immediate) { c.settings.savePracticeOptions(game.name, options, markAsLast) }
    }

    fun start(retry: List<CardEntity>? = null) {
        if (retry == null) saveOptions(markAsLast = true)
        phase = Phase.Loading
        // Новая игра — новые примеры; повтор ошибок — те же задания.
        if (retry == null) { tenses.clear(); tenseOptions.clear(); puzzles.clear(); shuffledTokens.clear() }
        viewModelScope.launch {
            val pool = retry?.shuffled() ?: when (game) {
                // Нужны карточки, у которых есть пример подходящей длины.
                Game.SENTENCE -> c.study.gamePool(deck, game.name, (count * 4).coerceAtMost(500), onlyMistakes = onlyMistakes).filter { puzzle(it) != null }.take(count)
                Game.TENSES -> c.study.gamePool(deck, game.name, (count * 2).coerceAtMost(500), onlyMistakes = onlyMistakes).filter { tense(it) != null }.take(count)
                else -> c.study.gamePool(deck, game.name, count.coerceAtMost(500), withAudio = needAudio, onlyMistakes = onlyMistakes)
            }
            items = pool
            index = 0
            correct = 0
            mistakes.clear()
            missed.clear()
            answered = false
            questionStartedAt = SystemClock.elapsedRealtime()
            phase = if (pool.isEmpty()) Phase.Summary else Phase.Playing
        }
    }

    fun retryMistakes() = start(missed.toList())

    /** Засчитать текущий вопрос. */
    fun record(ok: Boolean, mistake: Mistake? = null) {
        if (phase != Phase.Playing || answered) return
        val card = items.getOrNull(index) ?: return
        answered = true
        val duration = SystemClock.elapsedRealtime() - questionStartedAt
        val answeredAt = System.currentTimeMillis()
        if (ok) correct++ else {
            mistake?.let { mistakes += it }
            missed += card
        }
        // Запоминаем и между играми: ошибки попадутся первыми в следующий раз.
        c.appScope.launch { c.study.recordGame(card.id, game.name, ok, duration, answeredAt) }
    }

    fun next() {
        if (phase != Phase.Playing || !answered) return
        c.audio.stop()
        if (index + 1 >= items.size) phase = Phase.Summary else index++
        answered = false
        questionStartedAt = SystemClock.elapsedRealtime()
    }

    fun backToSetup() {
        c.audio.stop()
        phase = Phase.Setup
    }

    fun irregular(card: CardEntity) = c.study.irregular(card)

    private val puzzles = HashMap<String, SentencePuzzle?>()

    /** Предложение карточки для «Собери предложение» (одно и то же на всю игру). */
    fun puzzle(card: CardEntity): SentencePuzzle? = puzzles.getOrPut(card.id) {
        // Идиома и коллокация остаются одной плашкой: задача — встроить выражение в предложение.
        val chunk = card.term.takeIf { card.deck == Deck.IDIOMS.id || card.deck == Deck.COLLOCATIONS.id }
        SentencePuzzle.fromExamples(listOf(card.example1, card.example2).shuffled(), chunk)
    }

    private val tenses = HashMap<String, TenseTask?>()

    /** Задание тренажёра времён по глаголу — случайная форма и пример, одно на всю игру. */
    fun tense(card: CardEntity): TenseTask? = tenses.getOrPut(card.id) {
        irregular(card)?.let { TenseDrill.tasks(it).randomOrNull() }
    }

    private val tenseOptions = HashMap<String, List<String>>()

    fun tenseOptions(card: CardEntity, task: TenseTask): List<String> = tenseOptions.getOrPut(card.id) { task.options.shuffled() }

    private val shuffledTokens = HashMap<String, List<Int>>()

    /** Перемешанный порядок плашек; не совпадает с правильным, если это возможно. */
    fun shuffledOrder(card: CardEntity, size: Int): List<Int> = shuffledTokens.getOrPut(card.id) {
        val identity = (0 until size).toList()
        var order = identity.shuffled()
        repeat(5) { if (order == identity && size > 1) order = identity.shuffled() }
        order
    }
}

@Composable
fun GameHost(game: Game, navigator: Navigator, initialDeck: Deck? = null, onlyMistakes: Boolean = false, initialCount: Int? = null) {
    if (game.isGrammar) {
        GrammarGameHost(game, navigator)
        return
    }
    val c = LocalAppContainer.current
    val vm = viewModel(key = "game-${game.name}-${initialDeck?.id}-$onlyMistakes-$initialCount") {
        GameViewModel(c, game, initialDeck, onlyMistakes, initialCount)
    }
    when (game) {
        Game.TYPE_ANSWER -> GameFrame(vm, navigator) { card -> TypeAnswerRound(vm, card) }
        Game.IRREGULAR -> GameFrame(vm, navigator, counts = listOf(10, 20, Int.MAX_VALUE)) { card -> IrregularRound(vm, card) }
        Game.SHADOWING -> GameFrame(vm, navigator, scoreLabel = "похоже") { card -> ShadowingRound(vm, card) }
        Game.PRONUNCIATION -> GameFrame(vm, navigator, scoreLabel = "узнано") { card -> PronunciationRound(vm, card) }
        Game.SENTENCE -> GameFrame(vm, navigator, scoreLabel = "без ошибок") { card -> SentenceRound(vm, card) }
        Game.TENSES -> GameFrame(vm, navigator) { card -> TenseRound(vm, card) }
        Game.GRAMMAR_CHOICE, Game.GRAMMAR_GAP -> Unit
    }
}

/** Каркас игры: фазы и анимированная смена вопросов (новый въезжает справа). */
@Composable
private fun GameFrame(
    vm: GameViewModel,
    navigator: Navigator,
    counts: List<Int> = listOf(10, 20, 30),
    scoreLabel: String = "верно",
    allowRetry: Boolean = true,
    round: @Composable (CardEntity) -> Unit,
) {
    val reduced = com.marat.albion.ui.theme.LocalReducedMotion.current
    BackHandler(enabled = vm.phase == Phase.Playing || vm.phase == Phase.Summary) { vm.backToSetup() }
    val available by remember(vm.deck) { vm.observeAvailable(vm.deck) }.collectAsStateWithLifecycle(0)
    val mistakeCount by remember(vm.deck) { vm.observeMistakes(vm.deck) }.collectAsStateWithLifecycle(-1)
    // Ошибок не осталось — режим «только ошибки» выключаем сам.
    LaunchedEffect(mistakeCount) { if (mistakeCount == 0) vm.onlyMistakes = false }

    AnimatedContent(
        targetState = vm.phase,
        transitionSpec = { practiceTransition(reduced) },
        label = "phase",
    ) { phase ->
        when (phase) {
            Phase.Setup -> GameSetup(
                game = vm.game,
                deck = vm.deck,
                count = vm.count,
                onDeck = { vm.deck = it; vm.saveOptions() },
                onCount = { vm.count = it; vm.saveOptions() },
                onStart = { vm.start() },
                onBack = navigator::back,
                counts = counts,
                mode = vm.mode,
                onMode = { vm.mode = it; vm.saveOptions() },
                mistakes = mistakeCount,
                available = if (vm.onlyMistakes) mistakeCount.coerceAtLeast(0) else available,
                onlyMistakes = vm.onlyMistakes,
                onOnlyMistakes = { vm.onlyMistakes = it },
            )
            Phase.Loading -> Box(Modifier.fillMaxSize().paperSurface(), contentAlignment = Alignment.Center) {
                com.marat.albion.ui.components.LoadingState("Готовим упражнение")
            }
            Phase.Summary -> GameSummary(
                game = vm.game,
                correct = vm.correct,
                total = vm.items.size,
                mistakes = vm.mistakes.toList(),
                onRestart = { vm.start() },
                onRetryMistakes = if (allowRetry) ({ vm.retryMistakes() }) else null,
                onDone = navigator::back,
                scoreLabel = scoreLabel,
            )
            Phase.Playing -> Column(
                Modifier.fillMaxSize().paperSurface()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            ) {
                GameTopRow(vm.game, vm.index, vm.items.size, onClose = { vm.backToSetup() })
                AnimatedContent(
                    targetState = vm.index,
                    transitionSpec = { practiceTransition(reduced) },
                    modifier = Modifier.weight(1f),
                    label = "round",
                ) { i ->
                    vm.items.getOrNull(i)?.let { round(it) }
                }
            }
        }
    }
}
