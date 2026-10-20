package com.marat.albion.data.grammar

/** Главы — только списки тем с флажками. Все остальные материалы остаются в «О курсе». */
object GrammarContents {
    data class Result(val chapters: List<Chapter>, val intro: MdDocument)

    fun extract(doc: MdDocument, topics: List<Topic>): Result {
        val byId = topics.associateBy { it.id }
        val chapters = ArrayList<Chapter>()
        val intro = ArrayList<MdBlock>()
        var heading: MdHeading? = null
        val section = ArrayList<MdBlock>()

        fun finishSection() {
            val linked = ArrayList<Topic>()
            val remaining = section.filter { block ->
                val list = block as? MdList
                val lessons = list?.items?.mapNotNull { item ->
                    if (item.checked == null || item.blocks.size != 1) return@mapNotNull null
                    val text = (item.blocks.single() as? MdParagraph)?.text ?: return@mapNotNull null
                    GrammarRepository.WIKILINK.find(text)?.groupValues?.get(1)
                        ?.substringBefore('#')?.trim()?.let(byId::get)?.takeIf { it.kind == TopicKind.Lesson }
                }.orEmpty()
                val isChapterList = heading != null && list != null && !list.ordered &&
                    lessons.isNotEmpty() && lessons.size == list.items.size
                if (isChapterList) linked += lessons
                !isChapterList
            }
            if (linked.isNotEmpty()) chapters += Chapter(heading!!.text, linked.distinctBy { it.id })
            // Не оставляем пустой заголовок главы, но сохраняем любой сопроводительный текст.
            if (linked.isEmpty() || remaining.any { it != MdRule }) {
                heading?.let { intro += it }
                intro += remaining
            }
            section.clear()
        }

        for (block in doc.blocks) {
            if (block is MdHeading && block.level == 2) {
                finishSection()
                heading = block
            } else section += block
        }
        finishSection()
        return Result(chapters, doc.copy(blocks = intro))
    }
}
