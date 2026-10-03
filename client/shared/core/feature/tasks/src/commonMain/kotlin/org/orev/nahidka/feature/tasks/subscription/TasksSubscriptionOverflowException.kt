package org.orev.nahidka.feature.tasks.subscription

class TasksSubscriptionOverflowException(
    firstMissedRevision: Long
) : IllegalStateException(
    "Task subscription buffer overflowed at revision $firstMissedRevision"
)
