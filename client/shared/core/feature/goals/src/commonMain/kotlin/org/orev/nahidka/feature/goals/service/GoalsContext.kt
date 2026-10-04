package org.orev.nahidka.feature.goals.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.dto.GoalsDeleted
import org.orev.nahidka.feature.goals.dto.GoalsInserted
import org.orev.nahidka.feature.goals.dto.GoalsMutationResult
import org.orev.nahidka.feature.goals.dto.GoalsNotification
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot
import org.orev.nahidka.feature.goals.dto.GoalsUpdated
import org.orev.nahidka.feature.goals.subscription.GoalsSubscriptionBuilder
import org.orev.nahidka.feature.goals.subscription.GoalsSubscriptionCallbacks
import org.orev.nahidka.feature.goals.subscription.GoalsSubscriptionOverflowException

class GoalsContext(initialGoals: List<GoalRecord> = emptyList()) {
    private val stateMutex = Mutex()
    private var goalsByIdentifier = linkedMapOf<String, GoalRecord>()
    private var revision = 0L
    private val subscribers = mutableSetOf<Channel<GoalsNotification>>()

    init {
        initialGoals.forEach { goal ->
            validateGoal(goal)
            require(goal.goalIdentifier !in goalsByIdentifier) {
                "Duplicate initial goal identifier: ${goal.goalIdentifier}"
            }
            goalsByIdentifier[goal.goalIdentifier] = goal
        }
    }

    fun subscribe(
        subscriptionBufferCapacity: Int = DEFAULT_SUBSCRIPTION_BUFFER_CAPACITY
    ): GoalsSubscriptionBuilder {
        require(subscriptionBufferCapacity in 1 until Channel.UNLIMITED) {
            "Subscription buffer capacity must be positive and bounded"
        }
        return GoalsSubscriptionBuilder(this, subscriptionBufferCapacity)
    }

    suspend fun currentSnapshot(): GoalsSnapshot = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        GoalsSnapshot(revision, goalsByIdentifier.values.toList())
    }

    internal suspend fun insertGoal(
        goalCreationRequest: GoalCreationRequest
    ): GoalsMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        val goal = GoalRecord(goalCreationRequest.goalIdentifier, goalCreationRequest.title)
        validateGoal(goal)
        require(goal.goalIdentifier !in goalsByIdentifier) {
            "Goal already exists: ${goal.goalIdentifier}"
        }
        val nextGoalsByIdentifier = LinkedHashMap(goalsByIdentifier)
        nextGoalsByIdentifier[goal.goalIdentifier] = goal
        val nextRevision = nextRevision()
        val notification = GoalsInserted(nextRevision, goal)
        val result = GoalsMutationResult(nextRevision, goal, true)
        currentCoroutineContext().ensureActive()
        commitGoals(nextGoalsByIdentifier, notification)
        result
    }

    internal suspend fun removeGoal(goalIdentifier: String): GoalsMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        require(goalIdentifier.isNotBlank()) { "Goal identifier must not be blank" }
        val goal = requireNotNull(goalsByIdentifier[goalIdentifier]) {
            "Unknown goal identifier: $goalIdentifier"
        }
        val nextGoalsByIdentifier = LinkedHashMap(goalsByIdentifier)
        nextGoalsByIdentifier.remove(goalIdentifier)
        val nextRevision = nextRevision()
        val notification = GoalsDeleted(nextRevision, goal)
        val result = GoalsMutationResult(nextRevision, goal, true)
        currentCoroutineContext().ensureActive()
        commitGoals(nextGoalsByIdentifier, notification)
        result
    }

    internal suspend fun applyGoalUpdate(
        goalUpdateRequest: GoalUpdateRequest
    ): GoalsMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        require(goalUpdateRequest.goalIdentifier.isNotBlank()) { "Goal identifier must not be blank" }
        val previousGoal = requireNotNull(goalsByIdentifier[goalUpdateRequest.goalIdentifier]) {
            "Unknown goal identifier: ${goalUpdateRequest.goalIdentifier}"
        }
        val currentGoal = previousGoal.copy(title = goalUpdateRequest.title ?: previousGoal.title)
        validateGoal(currentGoal)
        if (previousGoal == currentGoal) {
            return@withLock GoalsMutationResult(revision, currentGoal, false)
        }
        val nextGoalsByIdentifier = LinkedHashMap(goalsByIdentifier)
        nextGoalsByIdentifier[currentGoal.goalIdentifier] = currentGoal
        val nextRevision = nextRevision()
        val notification = GoalsUpdated(nextRevision, previousGoal, currentGoal)
        val result = GoalsMutationResult(nextRevision, currentGoal, true)
        currentCoroutineContext().ensureActive()
        commitGoals(nextGoalsByIdentifier, notification)
        result
    }

    internal suspend fun collectNotifications(
        notifications: Channel<GoalsNotification>,
        callbacks: GoalsSubscriptionCallbacks,
        registrationCompleted: CompletableDeferred<Unit>
    ) {
        var subscriptionFailure: Throwable? = null
        try {
            stateMutex.withLock {
                currentCoroutineContext().ensureActive()
                check(notifications.trySend(GoalsSnapshot(revision, goalsByIdentifier.values.toList())).isSuccess)
                subscribers.add(notifications)
            }
            registrationCompleted.complete(Unit)
            yield()
            for (notification in notifications) {
                stateMutex.withLock {
                    currentCoroutineContext().ensureActive()
                }
                when (notification) {
                    is GoalsSnapshot -> callbacks.onSnapshot(notification)
                    is GoalsInserted -> callbacks.onInsert(notification)
                    is GoalsUpdated -> callbacks.onUpdate(notification)
                    is GoalsDeleted -> callbacks.onDelete(notification)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            registrationCompleted.completeExceptionally(failure)
            subscriptionFailure = failure
        } finally {
            withContext(NonCancellable) {
                stateMutex.withLock { subscribers.remove(notifications) }
                notifications.cancel()
            }
        }
        subscriptionFailure?.let { failure ->
            currentCoroutineContext().ensureActive()
            callbacks.onFailure(failure)
        }
    }

    private fun commitGoals(
        nextGoalsByIdentifier: LinkedHashMap<String, GoalRecord>,
        notification: GoalsNotification
    ) {
        goalsByIdentifier = nextGoalsByIdentifier
        revision = notification.revision
        val subscribersIterator = subscribers.iterator()
        while (subscribersIterator.hasNext()) {
            val notifications = subscribersIterator.next()
            val deliveryResult = notifications.trySend(notification)
            if (deliveryResult.isFailure) {
                subscribersIterator.remove()
                if (!deliveryResult.isClosed) {
                    notifications.close(GoalsSubscriptionOverflowException(notification.revision))
                }
            }
        }
    }

    private fun nextRevision(): Long {
        check(revision < Long.MAX_VALUE) { "Goal revision limit reached" }
        return revision + 1
    }

    private fun validateGoal(goal: GoalRecord) {
        require(goal.goalIdentifier.isNotBlank()) { "Goal identifier must not be blank" }
        require(goal.title.isNotBlank()) { "Goal title must not be blank" }
    }

    private companion object {
        const val DEFAULT_SUBSCRIPTION_BUFFER_CAPACITY = 64
    }
}
