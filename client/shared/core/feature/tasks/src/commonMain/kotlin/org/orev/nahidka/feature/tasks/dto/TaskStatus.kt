package org.orev.nahidka.feature.tasks.dto

enum class TaskStatus(val acceptsRating: Boolean) {
    TO_DO(acceptsRating = false),
    IN_PROGRESS(acceptsRating = false),
    DONE(acceptsRating = true)
}
