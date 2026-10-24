package com.marat.albion.data.repo

import androidx.room.withTransaction
import com.marat.albion.data.content.Deck
import com.marat.albion.data.content.IrregularData
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.db.DayCount
import com.marat.albion.data.db.DeckCounts
import com.marat.albion.data.db.GameAnswerLogEntity
import com.marat.albion.data.db.GameTotals
import com.marat.albion.data.db.ReviewLogEntity
import com.marat.albion.data.db.ReviewStateEntity
import com.marat.albion.data.db.StudyCandidate
import com.marat.albion.data.fsrs.CardState
import com.marat.albion.data.fsrs.Fsrs
import com.marat.albion.data.fsrs.Memory
import com.marat.albion.data.fsrs.Rating
import com.marat.albion.data.settings.NewCardOrder
import com.marat.albion.data.settings.Settings
import com.marat.albion.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.roundToInt

/** Сводка по колоде для главного экрана. */
data class DeckSummary(
    val deck: Deck,
    val total: Int = 0,
    val started: Int = 0,
    val due: Int = 0,
    val learning: Int = 0,
    val mature: Int = 0,
    val newAvailable: Int = 0,
) {
    val toStudy get() = due + newAvailable
    val progress get() = if (total == 0) 0f else started.toFloat() / total
}

/** Очередь занятия: сначала учебные шаги, затем повторения, затем новые. */
data class SessionQueue(
    val learning: List<CardEntity>,
    val reviews: List<CardEntity>,
    val news: List<CardEntity>,
)

private data class QueuePlan(
    val learning: List<CardEntity>,
    val reviews: List<StudyCandidate>,
    val news: List<StudyCandidate>,
)

class StudyRepository(
    private val db: AlbionDatabase,
    private val settings: SettingsRepository,
    private val json: Json,
) {
    private val cards = db.cards()
    private val reviews = db.reviews()
    private val games = db.gameActivity()

    // ---- время учебного дня ----

    fun dayStart(now: Long, hour: Int): Long {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        if (c.get(Calendar.HOUR_OF_DAY) < hour) c.add(Calendar.DAY_OF_YEAR, -1)
        c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** Сдвиг для группировки по дням в SQL: day = (t + shift) / сутки. */
    fun dayShift(hour: Int, now: Long = System.currentTimeMillis()): Long =
        TimeZone.getDefault().getOffset(now).toLong() - hour * 3_600_000L

    fun studyDay(t: Long, hour: Int) = (t + dayShift(hour, t)) / Fsrs.DAY

    /** Тик раз в минуту — чтобы счётчики «к повторению» обновлялись сами. */
    val minuteTicker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    // ---- главный экран ----

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSummaries(): Flow<List<DeckSummary>> =
        combine(minuteTicker, settings.settings) { now, s -> now to s }
            .flatMapLatest { (now, s) ->
                val since = dayStart(now, s.dayStartHour)
                combine(cards.observeCounts(now), reviews.observeReviewsSince(since)) { counts, _ -> counts }
                    .map { counts -> buildSummaries(counts, s, now) }
            }

    private suspend fun buildSummaries(counts: List<DeckCounts>, s: Settings, now: Long): List<DeckSummary> = db.withTransaction {
        val byId = counts.associateBy { it.deck }
        Deck.entries.map { deck ->
            val c = byId[deck.id]
            if (c == null) DeckSummary(deck) else {
                val plan = planQueue(deck, s, now)
                DeckSummary(
                    deck = deck,
                    total = c.total,
                    started = c.started,
                    due = plan.reviews.size + plan.learning.size,
                    learning = c.learning,
                    mature = c.mature,
                    newAvailable = plan.news.size,
                )
            }
        }
    }

    // ---- занятие ----

    suspend fun buildQueue(deck: Deck, now: Long = System.currentTimeMillis()): SessionQueue {
        val s = settings.current()
        return db.withTransaction {
            val plan = planQueue(deck, s, now)
            val selected = (plan.reviews + plan.news).map { it.id }
                .chunked(500).flatMap { cards.getMany(it) }.associateBy { it.id }
            SessionQueue(
                learning = plan.learning,
                reviews = plan.reviews.map { selected.getValue(it.id) },
                news = plan.news.map { selected.getValue(it.id) },
            )
        }
    }

    /**
     * Общее занятие по всем колодам. Лимиты и сиблинги — по каждой колоде, как в отдельных
     * занятиях, поэтому числа совпадают со сводкой на главном экране. Повторения перемешаны,
     * новые чередуются между колодами.
     */
    suspend fun buildQueueAll(now: Long = System.currentTimeMillis()): SessionQueue {
        val s = settings.current()
        return db.withTransaction {
            val plans = Deck.entries.map { planQueue(it, s, now) }
            val selected = plans.flatMap { it.reviews + it.news }.map { it.id }
                .chunked(500).flatMap { cards.getMany(it) }.associateBy { it.id }
            SessionQueue(
                learning = plans.flatMap { it.learning },
                reviews = plans.flatMap { p -> p.reviews.map { selected.getValue(it.id) } }.shuffled(),
                news = roundRobin(plans.map { p -> p.news.map { selected.getValue(it.id) } }),
            )
        }
    }

    /** Берёт по одной карточке из каждой колоды по очереди. */
    private fun <T> roundRobin(lists: List<List<T>>): List<T> {
        val total = lists.sumOf { it.size }
        val result = ArrayList<T>(total)
        var i = 0
        while (result.size < total) {
            lists.forEach { list -> list.getOrNull(i)?.let(result::add) }
            i++
        }
        return result
    }

    /** Единый отбор для занятия, счётчиков колод и напоминаний. */
    private suspend fun planQueue(deck: Deck, s: Settings, now: Long): QueuePlan {
        val since = dayStart(now, s.dayStartHour)
        val newLeft = max(0, s.newPerDay - reviews.countSince(deck.id, CardState.NEW, since))
        val reviewLeft = max(0, s.reviewsPerDay - reviews.countSince(deck.id, CardState.REVIEW, since))
        val learning = reviews.learningDue(deck.id, now + LEARN_AHEAD)
        // Лимиты применяем после исключения сиблингов: доступное слово может стоять
        // после любого числа отложенных значений. Пока читаем только id и термин.
        val dueReviews = if (reviewLeft > 0) reviews.reviewCandidates(deck.id, now).shuffled() else emptyList()
        val newCandidates = if (newLeft > 0) cards.newCandidates(
            deck.id,
            byLevel = s.newOrder == NewCardOrder.LEVEL,
            random = s.newOrder == NewCardOrder.RANDOM,
        ) else emptyList()
        val answeredToday = reviews.termsAnsweredSince(deck.id, since).map(Siblings::key).toSet()
        val (picked, fresh) = Siblings.select(
            learning.map { StudyCandidate(it.id, it.term) }, dueReviews, newCandidates,
            answeredToday, reviewLeft, newLeft,
        )
        return QueuePlan(learning = learning, reviews = picked, news = fresh)
    }

    /** Карточки учебных шагов, которые подойдут в ближайшие [LEARN_AHEAD] мс. */
    suspend fun learningDue(deck: Deck, now: Long = System.currentTimeMillis()) =
        reviews.learningDue(deck.id, now + LEARN_AHEAD)

    suspend fun memory(cardId: String): Memory = reviews.state(cardId)?.toMemory() ?: Memory()

    suspend fun scheduler(): Fsrs = schedulerFor(settings.current())

    /** Планировщик с персональными параметрами, если пользователь их подобрал. */
    private fun schedulerFor(s: Settings) = Fsrs(
        desiredRetention = s.desiredRetention.toDouble(),
        w = Fsrs.decodeParams(s.fsrsParams) ?: Fsrs.DEFAULT_W,
    )

    /** Подбирает параметры FSRS по истории ответов и сохраняет их, если прогноз стал точнее. */
    suspend fun optimizeParams(onProgress: (Float) -> Unit): com.marat.albion.data.fsrs.OptimizeOutcome =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val current = settings.current()
            val events = reviews.allEvents()
            val initial = Fsrs.decodeParams(current.fsrsParams) ?: Fsrs.DEFAULT_W
            val outcome = com.marat.albion.data.fsrs.FsrsOptimizer(events, initial).optimize(onProgress = onProgress)
            if (outcome is com.marat.albion.data.fsrs.OptimizeOutcome.Done && outcome.improved) {
                val applied = settings.setFsrsParamsIfUnchanged(
                    current.fsrsParams, current.fsrsUpdatedAt,
                    Fsrs.encodeParams(outcome.params), outcome.reviews, outcome.improvementPercent.toFloat(),
                )
                if (!applied) return@withContext com.marat.albion.data.fsrs.OptimizeOutcome.Superseded
            }
            outcome
        }

    suspend fun resetParams() = settings.setFsrsParams(null, 0, 0f)

    /** Для напоминаний: сколько ответов сегодня (карточки и практика) и сколько карточек ждут повторения. */
    suspend fun todayStatus(now: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val s = settings.current()
        val since = dayStart(now, s.dayStartHour)
        // «Ждут повторения» — с учётом дневных лимитов, как на главном экране.
        val due = buildSummaries(cards.observeCounts(now).first(), s, now).sumOf { it.due }
        return (reviews.countAllSince(since) + games.countAnswersSince(since)) to due
    }

    /** Сохраняет ответ. Возвращает id записи журнала (для отмены) и новую память. */
    suspend fun answer(card: CardEntity, rating: Rating, durationMs: Long, now: Long = System.currentTimeMillis()): Pair<Long, Memory> {
        val s = settings.current()
        val fsrs = schedulerFor(s)
        // Чтение прежнего состояния и обе записи — одна атомарная операция.
        return db.withTransaction {
            val prev = reviews.state(card.id)
            val before = prev?.toMemory() ?: Memory()
            var next = fsrs.review(before, rating, now)
            if (next.state == CardState.REVIEW) {
                // Интервалы в днях привязываем к началу учебного дня, как в Anki.
                val days = max(1, ((next.due - now).toDouble() / Fsrs.DAY).roundToInt())
                next = next.copy(due = dayStart(now, s.dayStartHour) + days * Fsrs.DAY)
            }
            reviews.upsertState(next.toEntity(card.id, card.deck))
            val logId = reviews.insertLog(
                ReviewLogEntity(
                    cardId = card.id, deck = card.deck, rating = rating.value, reviewedAt = now,
                    stateBefore = before.state, durationMs = durationMs.coerceIn(0, MAX_DURATION),
                    prevStability = prev?.stability, prevDifficulty = prev?.difficulty, prevDue = prev?.due,
                    prevLastReview = prev?.lastReview, prevStep = prev?.step, prevReps = prev?.reps, prevLapses = prev?.lapses,
                )
            )
            logId to next
        }
    }

    /** Отменяет ответ: восстанавливает прежнее состояние карточки. */
    suspend fun undo(logId: Long): CardEntity? = db.withTransaction {
        val log = reviews.log(logId) ?: return@withTransaction null
        if (log.stateBefore == CardState.NEW || log.prevStability == null) {
            reviews.deleteState(log.cardId)
        } else {
            reviews.upsertState(
                ReviewStateEntity(
                    cardId = log.cardId, deck = log.deck, state = log.stateBefore, step = log.prevStep ?: 0,
                    stability = log.prevStability, difficulty = log.prevDifficulty ?: 5.0,
                    due = log.prevDue ?: log.reviewedAt, lastReview = log.prevLastReview ?: 0,
                    reps = log.prevReps ?: 0, lapses = log.prevLapses ?: 0,
                )
            )
        }
        reviews.deleteLog(logId)
        cards.get(log.cardId)
    }

    // ---- просмотр ----

    fun browse(deck: Deck, query: String, filter: Int): Flow<List<CardEntity>> =
        cards.browse(deck.id, query.trim(), filter, System.currentTimeMillis())

    /** Карточки для мини-игр; повторяющиеся термины (разные значения) — по одному. */
    /**
     * Карточки для мини-игры: сначала слова, на которых вы ошибались в этой игре, затем начатые,
     * затем новые. Повторяющиеся термины (разные значения) — по одному.
     */
    suspend fun gamePool(
        deck: Deck,
        game: String,
        count: Int,
        withAudio: Boolean = false,
        onlyMistakes: Boolean = false,
    ): List<CardEntity> =
        cards.gamePool(deck.id, game, count * 3, withAudio, onlyMistakes).distinctBy { it.term.lowercase() }.take(count)

    fun observeGameMistakes(deck: Deck, game: String, withAudio: Boolean = false) = cards.observeGameMistakes(deck.id, game, withAudio)

    /** Запоминает ответ в игре: ошибка ставит слово в начало следующих игр, два верных подряд — снимают. */
    suspend fun recordGame(
        cardId: String, game: String, ok: Boolean, durationMs: Long = 0,
        now: Long = System.currentTimeMillis(),
    ) = db.withTransaction {
        val old = cards.gameStat(cardId, game)
        cards.upsertGameStat(
            com.marat.albion.data.db.GameCardStatEntity(
                cardId = cardId,
                game = game,
                correct = (old?.correct ?: 0) + if (ok) 1 else 0,
                wrong = (old?.wrong ?: 0) + if (ok) 0 else 1,
                streak = if (ok) (old?.streak ?: 0) + 1 else 0,
                lastSeen = now,
            )
        )
        games.insert(GameAnswerLogEntity(game = game, itemId = cardId, correct = ok, answeredAt = now,
            durationMs = durationMs.coerceIn(0, MAX_GAME_DURATION)))
    }

    /** Грамматические задания для игры; level = "" — все уровни. */
    suspend fun grammarPool(kind: String, level: String, count: Int) =
        db.grammarTasks().pool(kind, level, count)

    fun observeGrammarTaskCount(kind: String) = db.grammarTasks().observeCount(kind)

    /** «Проверь себя» после темы учебника. */
    suspend fun topicPool(topic: Int) = com.marat.albion.data.grammar.GrammarQuizSelection.select(db.grammarTasks().topicPool(topic))

    fun observeTopicTaskCount(topic: Int) = db.grammarTasks().observeTopicCount(topic)

    suspend fun recordGrammar(
        taskId: String, ok: Boolean, kind: String, durationMs: Long = 0,
        now: Long = System.currentTimeMillis(),
    ) = db.withTransaction {
        val dao = db.grammarTasks()
        val old = dao.stat(taskId)
        dao.upsertStat(
            com.marat.albion.data.db.GrammarTaskStatEntity(
                taskId = taskId,
                correct = (old?.correct ?: 0) + if (ok) 1 else 0,
                wrong = (old?.wrong ?: 0) + if (ok) 0 else 1,
                lastSeen = now,
            )
        )
        games.insert(GameAnswerLogEntity(game = if (kind == "input") "GRAMMAR_GAP" else "GRAMMAR_CHOICE",
            itemId = taskId, correct = ok, answeredAt = now, durationMs = durationMs.coerceIn(0, MAX_GAME_DURATION)))
    }

    fun observeCard(id: String) = cards.observe(id)
    fun observeState(id: String) = reviews.observeState(id)

    suspend fun resetCard(id: String) = reviews.deleteState(id)
    suspend fun resetDeck(deck: Deck) = reviews.resetDeck(deck.id)

    fun irregular(card: CardEntity): IrregularData? =
        if (card.deck != Deck.IRREGULAR.id) null
        else card.extra?.let { runCatching { json.decodeFromString(IrregularData.serializer(), it) }.getOrNull() }

    // ---- статистика ----

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeStats(): Flow<Stats> = combine(minuteTicker, settings.settings.map { it.dayStartHour }.distinctUntilChanged()) { now, hour ->
        now to hour
    }.flatMapLatest { (now, hour) ->
        val shift = dayShift(hour, now)
        val today = (now + shift) / Fsrs.DAY
        val todayStart = dayStart(now, hour)
        val monthAgo = todayStart - 29 * Fsrs.DAY
        combine(
            combine(reviews.observeDaily(monthAgo, shift), games.observeDaily(monthAgo, shift)) { cards, games ->
                (cards + games).groupBy { it.day }.map { (day, counts) -> DayCount(day, counts.sumOf { it.count }) }.sortedBy { it.day }
            },
            reviews.observeForecast(todayStart + 15 * Fsrs.DAY, shift),
            combine(reviews.observeActiveDays(shift), games.observeActiveDays(shift)) { cards, games ->
                (cards + games).distinct().sortedDescending()
            },
            combine(reviews.observeReviewAnswers(monthAgo), reviews.observeReviewPassed(monthAgo)) { a, p -> a to p },
            combine(
                reviews.observeTotalReviews(),
                combine(reviews.observeTimeSince(todayStart), games.observeTimeSince(todayStart)) { cards, games -> cards + games },
                reviews.observeReviewsSince(todayStart),
                cards.observeCounts(now),
                combine(games.observeTotals(), games.observeAnswersSince(todayStart)) { totals, todayCount -> totals to todayCount },
            ) { total, time, todayCount, counts, games -> StatsSummary(total, time, todayCount, counts, games.first, games.second) },
        ) { daily, forecast, activeDays, retention, misc ->
            Stats(
                today = today,
                daily = daily,
                forecast = forecast.map { if (it.day < today) DayCount(today, it.count) else it }
                    .groupBy { it.day }.map { (d, l) -> DayCount(d, l.sumOf { it.count }) },
                streak = streak(activeDays, today),
                lastActiveDay = activeDays.firstOrNull(),
                retention = if (retention.first == 0) null else retention.second.toFloat() / retention.first,
                retentionSample = retention.first,
                totalReviews = misc.total,
                timeTodayMs = misc.time,
                reviewsToday = misc.today,
                counts = misc.counts,
                games = misc.games,
                gameAnswersToday = misc.gamesToday,
            )
        }
    }

    private fun streak(daysDesc: List<Long>, today: Long): Int {
        if (daysDesc.isEmpty()) return 0
        var expected = if (daysDesc.first() == today) today else today - 1
        var n = 0
        for (d in daysDesc) {
            if (d == expected) { n++; expected-- } else if (d < expected) break
        }
        return n
    }

    companion object {
        /** Как в Anki: учебные шаги, наступающие в ближайшие 20 минут, показываем сразу. */
        const val LEARN_AHEAD = 20 * 60_000L
        private const val MAX_DURATION = 60_000L
        private const val MAX_GAME_DURATION = 10 * 60_000L
    }
}

private data class StatsSummary(
    val total: Int, val time: Long, val today: Int, val counts: List<DeckCounts>,
    val games: List<GameTotals>, val gamesToday: Int,
)

data class Stats(
    val today: Long,
    val daily: List<DayCount>,
    val forecast: List<DayCount>,
    val streak: Int,
    /** Последний учебный день с ответами (в тех же единицах, что [today]); null — ещё не занимались. */
    val lastActiveDay: Long?,
    val retention: Float?,
    val retentionSample: Int,
    val totalReviews: Int,
    val timeTodayMs: Long,
    val reviewsToday: Int,
    val counts: List<DeckCounts>,
    val games: List<GameTotals>,
    val gameAnswersToday: Int,
) {
    val answersToday get() = reviewsToday + gameAnswersToday
    val totalGameAnswers get() = games.sumOf { it.answers }
    val gameAccuracy get() = if (totalGameAnswers == 0) null else games.sumOf { it.correct }.toFloat() / totalGameAnswers
}

fun ReviewStateEntity.toMemory() = Memory(state, step, stability, difficulty, due, lastReview, reps, lapses)

fun Memory.toEntity(cardId: String, deck: String) = ReviewStateEntity(
    cardId = cardId, deck = deck, state = state, step = step, stability = stability,
    difficulty = difficulty, due = due, lastReview = lastReview, reps = reps, lapses = lapses,
)
