package org.orev.nahidka.feature.goals.subscription

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class GoalsSubscriptionOwnership : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<GoalsSubscriptionOwnership>
}
