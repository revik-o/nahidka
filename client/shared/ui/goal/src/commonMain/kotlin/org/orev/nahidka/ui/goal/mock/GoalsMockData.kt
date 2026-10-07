package org.orev.nahidka.ui.goal.mock

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.feature.goals.service.GoalsManager

object GoalsMockData {

    suspend fun seed(goalsManager: GoalsManager) {
        listOf(
            GoalCreationRequest(
                identifier = "demo-half-marathon",
                title = "Run a half marathon",
                description = "Train three times a week and finish 21 km in under two hours.",
                picture = GoalPicture.Emoji("🏃"),
                progressPercentage = 40f,
                deadlineDate = LocalDate(2027, 4, 18),
            ),
            GoalCreationRequest(
                identifier = "demo-reading",
                title = "Read 24 books this year",
                description = "Two books a month, alternating fiction and non-fiction.",
                picture = GoalPicture.Emoji("📚"),
                progressPercentage = 75f,
                deadlineDate = LocalDate(2026, 12, 31),
            ),
            GoalCreationRequest(
                identifier = "demo-japan",
                title = "Save for a trip to Japan",
                description = "Put money aside every month for flights and two weeks of travel.",
                picture = GoalPicture.Emoji("🧳"),
                progressPercentage = 30f,
                deadlineDate = LocalDate(2027, 3, 1),
            ),
            GoalCreationRequest(
                identifier = "demo-guitar",
                title = "Learn to play the guitar",
                description = "Practice twenty minutes a day and learn five favorite songs.",
                picture = GoalPicture.Emoji("🎸"),
                progressPercentage = 100f,
            ),
            GoalCreationRequest(
                identifier = "demo-cooking",
                title = "Cook something new every week",
                description = "Try a recipe from a different cuisine each weekend.",
                progressPercentage = 10f,
            ),
        ).forEach { goalCreationRequest -> goalsManager.createGoal(goalCreationRequest) }
    }
}
