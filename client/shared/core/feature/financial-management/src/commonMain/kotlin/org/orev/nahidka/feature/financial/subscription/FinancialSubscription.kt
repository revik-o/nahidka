package org.orev.nahidka.feature.financial.subscription

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.financial.dto.EntityChange
import org.orev.nahidka.feature.financial.dto.EntitySnapshot
import org.orev.nahidka.feature.financial.dto.FinancialChangeBatch

class FinancialSubscription<T> internal constructor(
    private val source: WatchSource<T>,
    private val handlers: Handlers<T> = Handlers(),
) {
    internal data class Handlers<T>(
        val onSnapshot: (suspend (EntitySnapshot<T>) -> Unit)? = null,
        val onResync: (suspend (EntitySnapshot<T>) -> Unit)? = null,
        val onBatch: (suspend (FinancialChangeBatch<T>) -> Unit)? = null,
        val onInsert: (suspend (EntityChange.Insert<T>) -> Unit)? = null,
        val onUpdate: (suspend (EntityChange.Update<T>) -> Unit)? = null,
        val onDelete: (suspend (EntityChange.Delete<T>) -> Unit)? = null,
        val onError: (suspend (Throwable) -> Unit)? = null,
    )

    fun onSnapshot(handler: suspend (EntitySnapshot<T>) -> Unit): FinancialSubscription<T> =
        copy(handlers.copy(onSnapshot = handler))

    fun onResync(handler: suspend (EntitySnapshot<T>) -> Unit): FinancialSubscription<T> =
        copy(handlers.copy(onResync = handler))

    fun onBatch(handler: suspend (FinancialChangeBatch<T>) -> Unit): FinancialSubscription<T> =
        copy(handlers.copy(onBatch = handler))

    fun onInsert(handler: suspend (EntityChange.Insert<T>) -> Unit): FinancialSubscription<T> =
        copy(handlers.copy(onInsert = handler))

    fun onUpdate(handler: suspend (EntityChange.Update<T>) -> Unit): FinancialSubscription<T> =
        copy(handlers.copy(onUpdate = handler))

    fun onDelete(handler: suspend (EntityChange.Delete<T>) -> Unit): FinancialSubscription<T> =
        copy(handlers.copy(onDelete = handler))

    fun onError(handler: suspend (Throwable) -> Unit): FinancialSubscription<T> =
        copy(handlers.copy(onError = handler))

    fun launchIn(scope: CoroutineScope): Job {
        val configured = handlers
        val replace = requireNotNull(configured.onSnapshot) { "Configure onSnapshot before launchIn" }
        val messages = openWatch(source)
        val parent = requireNotNull(scope.coroutineContext[Job]) { "A subscription requires a lifecycle-owned scope Job" }
        val supervisor = SupervisorJob(parent)
        val uncaught = CoroutineExceptionHandler { _, failure -> source.errorReporter.report(failure) }
        val isolatedScope = CoroutineScope(scope.coroutineContext + supervisor + uncaught)
        val job = isolatedScope.launch(start = CoroutineStart.LAZY) {
            try {
                messages.collect { message ->
                    when (message) {
                        is WatchMessage.Snapshot -> replace(message.snapshot)
                        is WatchMessage.Resync -> (configured.onResync ?: replace)(message.snapshot)
                        is WatchMessage.Batch -> {
                            configured.onBatch?.invoke(message.batch)
                            for (change in message.batch.changes) {
                                when (change) {
                                    is EntityChange.Insert -> configured.onInsert?.invoke(change)
                                    is EntityChange.Update -> configured.onUpdate?.invoke(change)
                                    is EntityChange.Delete -> configured.onDelete?.invoke(change)
                                }
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val report = configured.onError
                if (report == null) throw failure
                try {
                    report(failure)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (reportFailure: Exception) {
                    source.errorReporter.report(reportFailure)
                }
            }
        }
        val sessionHook = source.sessionLifetime.invokeOnCompletion { cause ->
            job.cancel(CancellationException("Financial session closed", cause))
        }
        job.invokeOnCompletion {
            sessionHook.dispose()
            supervisor.complete()
        }
        job.start()
        return job
    }

    private fun copy(next: Handlers<T>): FinancialSubscription<T> = FinancialSubscription(source, next)
}
