package org.orev.nahidka.feature.settings.subscription

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class SettingsSubscriptionOwnership : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SettingsSubscriptionOwnership>
}
