package org.orev.nahidka.feature.financial.subscription

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

internal fun <T> openWatch(source: WatchSource<T>): Flow<WatchMessage<T>> {
    val baseline = source.currentFrame()
    return flow {
        baseline.released.await()
        if (baseline.closed) throw FinancialSessionClosedException()
        emit(WatchMessage.Snapshot(source.snapshot(baseline)))
        var cursor = baseline.revision
        source.frames.collect { frame ->
            frame.released.await()
            if (frame.closed) throw FinancialSessionClosedException()
            if (frame.revision <= cursor) return@collect
            val firstRetainedRevision = frame.journal.firstOrNull()?.revision
            if (firstRetainedRevision == null || cursor < firstRetainedRevision - 1) {
                emit(WatchMessage.Resync(source.snapshot(frame)))
                cursor = frame.revision
            } else {
                for (commit in frame.journal) {
                    if (commit.revision > cursor) {
                        source.project(commit)?.let { emit(WatchMessage.Batch(it)) }
                        cursor = commit.revision
                    }
                }
            }
        }
    }
}
