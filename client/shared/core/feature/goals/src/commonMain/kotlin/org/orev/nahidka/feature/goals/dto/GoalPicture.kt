package org.orev.nahidka.feature.goals.dto

sealed interface GoalPicture {

    data class Emoji(val symbol: String) : GoalPicture

    class Photo(val content: ByteArray) : GoalPicture {

        override fun equals(other: Any?): Boolean = other is Photo && content.contentEquals(other.content)

        override fun hashCode(): Int = content.contentHashCode()
    }
}
