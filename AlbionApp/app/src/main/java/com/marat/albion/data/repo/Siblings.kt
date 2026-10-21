package com.marat.albion.data.repo

import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.db.StudyCandidate

/**
 * «Сиблинги» — карточки одного и того же слова с разными значениями (take off: «взлетать»,
 * «снимать одежду», «стать успешным»). Если показать их в один день, второе значение
 * подсказывает первое, и оценка FSRS становится нечестной. Как в Anki: в день показывается
 * только одно значение слова, остальные ждут следующего дня.
 */
object Siblings {

    fun key(card: CardEntity) = key(card.term)
    fun key(term: String) = term.trim().lowercase()

    /**
     * Повторения и новые на сегодня без сиблингов.
     * [learning] — учебные шаги (их не откладываем: они уже начаты);
     * [answeredToday] — слова, на которые сегодня уже отвечали в этой колоде.
     */
    fun select(
        learning: List<StudyCandidate>,
        reviews: List<StudyCandidate>,
        news: List<StudyCandidate>,
        answeredToday: Set<String>,
        reviewLimit: Int,
        newLimit: Int,
    ): Pair<List<StudyCandidate>, List<StudyCandidate>> {
        val taken = HashSet(answeredToday)
        learning.forEach { taken += key(it.term) }
        val pickedReviews = reviews.filter { taken.add(key(it.term)) }.take(reviewLimit)
        // Отложенные повторения не должны «освобождать» слово для новой карточки.
        reviews.forEach { taken += key(it.term) }
        val pickedNews = news.filter { taken.add(key(it.term)) }.take(newLimit)
        return pickedReviews to pickedNews
    }
}
