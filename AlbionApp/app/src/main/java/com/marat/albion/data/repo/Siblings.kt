package com.marat.albion.data.repo

import com.marat.albion.data.db.CardEntity

/**
 * «Сиблинги» — карточки одного и того же слова с разными значениями (take off: «взлетать»,
 * «снимать одежду», «стать успешным»). Если показать их в один день, второе значение
 * подсказывает первое, и оценка FSRS становится нечестной. Как в Anki: в день показывается
 * только одно значение слова, остальные ждут следующего дня.
 */
object Siblings {

    fun key(card: CardEntity) = card.term.trim().lowercase()

    /**
     * Повторения и новые на сегодня без сиблингов.
     * [learning] — учебные шаги (их не откладываем: они уже начаты);
     * [answeredToday] — слова, на которые сегодня уже отвечали в этой колоде.
     */
    fun select(
        learning: List<CardEntity>,
        reviews: List<CardEntity>,
        news: List<CardEntity>,
        answeredToday: Set<String>,
        reviewLimit: Int,
        newLimit: Int,
    ): Pair<List<CardEntity>, List<CardEntity>> {
        val taken = HashSet(answeredToday)
        learning.forEach { taken += key(it) }
        val pickedReviews = reviews.filter { taken.add(key(it)) }.take(reviewLimit)
        // Отложенные повторения не должны «освобождать» слово для новой карточки.
        reviews.forEach { taken += key(it) }
        val pickedNews = news.filter { taken.add(key(it)) }.take(newLimit)
        return pickedReviews to pickedNews
    }
}
