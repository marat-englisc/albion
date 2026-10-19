package com.marat.albion.data.content

import kotlinx.serialization.Serializable

/** Неправильный глагол: три формы, у каждой — варианты (abided / abode) со своей транскрипцией и озвучкой. */
@Serializable
data class IrregularData(
    val forms: List<VerbForm>,
    val note: String = "",
)

@Serializable
data class VerbForm(
    /** "Infinitive", "Past Simple", "Past Participle". */
    val label: String,
    val variants: List<FormVariant>,
    val examples: List<String>,
)

@Serializable
data class FormVariant(
    val text: String,
    val ipa: String,
    /** Файлы озвучки; несколько — если есть альтернативное произношение (ate / ate (eit)). */
    val audio: List<String>,
)
