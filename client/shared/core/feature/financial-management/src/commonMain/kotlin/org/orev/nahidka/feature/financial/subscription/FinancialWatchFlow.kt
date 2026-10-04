package org.orev.nahidka.feature.financial.subscription

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.orev.nahidka.feature.financial.store.awaitRelease

internal fun <T> openWatch(source: WatchSource<T>): Flow<WatchMessage<T>> {
    val baseline = source.currentFrame()

    return flow {
        baseline.awaitRelease()

        if (baseline.closed) {
            throw FinancialSessionClosedException()
        }

        emit(WatchMessage.Snapshot(source.snapshot(baseline)))

        var cursor = baseline.revision

        source.frames.collect { frame ->
            frame.awaitRelease()

            if (frame.closed) {
                throw FinancialSessionClosedException()
            }

            if (frame.revision > cursor) {
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
}
