package org.orev.nahidka.feature.socialbattery.subscription

class SocialBatterySubscriptionOverflowException(
    val firstMissedRevision: Long
) : IllegalStateException("Social battery subscription overflow at revision $firstMissedRevision")
