package org.orev.nahidka.feature.settings.subscription

class SettingsSubscriptionOverflowException(
    val firstMissedRevision: Long
) : IllegalStateException("Settings subscription overflow at revision $firstMissedRevision")
