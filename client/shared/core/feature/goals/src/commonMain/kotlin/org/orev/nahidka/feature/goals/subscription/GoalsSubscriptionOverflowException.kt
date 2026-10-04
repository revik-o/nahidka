package org.orev.nahidka.feature.goals.subscription

class GoalsSubscriptionOverflowException(
    val firstMissedRevision: Long
) : IllegalStateException("Goal subscription queue overflowed at revision $firstMissedRevision")
