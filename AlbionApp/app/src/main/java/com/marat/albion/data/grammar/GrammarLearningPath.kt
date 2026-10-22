package com.marat.albion.data.grammar

/** Номера — постоянные адреса уроков; маршрут сначала раскрывает их предпосылки. */
object GrammarLearningPath {
    fun ordered(topics: List<Topic>): List<Topic> {
        val lessons = topics.filter { it.kind == TopicKind.Lesson }
        val byNumber = lessons.associateBy { it.number }
        val visited = HashSet<String>()
        val ordered = ArrayList<Topic>()
        fun visit(topic: Topic) {
            if (!visited.add(topic.id)) return
            topic.prerequisites.mapNotNull(byNumber::get).forEach(::visit)
            ordered += topic
        }
        lessons.forEach(::visit)
        return ordered
    }

    fun recommend(topics: List<Topic>, mastered: Set<String>, level: String? = null, lastId: String? = null): Topic? {
        val ordered = ordered(topics)
        val eligible = ordered.filter { level == null || it.level == level }
        val target = eligible.firstOrNull { it.id == lastId && it.id !in mastered }
            ?: eligible.firstOrNull { it.id !in mastered } ?: return null
        val byNumber = ordered.associateBy { it.number }
        val seen = HashSet<String>()
        fun firstMissing(topic: Topic): Topic {
            if (!seen.add(topic.id)) return topic
            val prerequisite = topic.prerequisites.mapNotNull(byNumber::get).firstOrNull { it.id !in mastered }
            return prerequisite?.let(::firstMissing) ?: topic
        }
        return firstMissing(target)
    }
}
