package org.orev.nahidka.ui.goal.component

import org.orev.nahidka.feature.goals.dto.GoalPicture

internal val GOAL_EMOJI_PICTURES: List<GoalPicture.Emoji> =
    listOf("🎯", "🏃", "📚", "💪", "💰", "🧳", "🏠", "🎓", "🎨", "🎸", "🧘", "🌱")
        .map(GoalPicture::Emoji)
