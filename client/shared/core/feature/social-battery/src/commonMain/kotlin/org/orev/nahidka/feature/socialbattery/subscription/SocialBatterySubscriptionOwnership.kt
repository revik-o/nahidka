package org.orev.nahidka.feature.socialbattery.subscription

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class SocialBatterySubscriptionOwnership : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SocialBatterySubscriptionOwnership>
}
