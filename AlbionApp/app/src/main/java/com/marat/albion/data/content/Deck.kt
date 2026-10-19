package com.marat.albion.data.content

import kotlinx.serialization.Serializable

/**
 * Колода карточек. [file] — имя таблицы в collections/, [audioDir] — папка в audio/.
 * [kind] показывается на карточке вместо части речи, если в таблице её нет.
 */
@Serializable
enum class Deck(
    val id: String,
    val file: String,
    val audioDir: String,
    val title: String,
    val subtitle: String,
    val kind: String,
) {
    WORDS("words", "words.xlsx", "words", "Слова", "Лексика A1–C1 с уровнями CEFR", "word"),
    PHRASAL("phrasal_verbs", "phrasal_verbs.xlsx", "phrasal_verbs", "Фразовые глаголы", "Глагол + частица", "phrasal verb"),
    IDIOMS("idioms", "idiomas.xlsx", "idiomas", "Идиомы", "Образные выражения", "idiom"),
    COLLOCATIONS("collocations", "collocations.xlsx", "collocations", "Коллокации", "Устойчивые сочетания", "collocation"),
    IRREGULAR("irregular_verbs", "irregular_verbs.xlsx", "irregular_verbs", "Неправильные глаголы", "Три формы с озвучкой", "irregular verb");

    companion object {
        fun byId(id: String): Deck? = entries.firstOrNull { it.id == id }
    }
}
