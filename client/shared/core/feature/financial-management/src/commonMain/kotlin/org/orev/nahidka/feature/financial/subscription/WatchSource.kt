package org.orev.nahidka.feature.financial.subscription

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.financial.dto.EntitySnapshot
import org.orev.nahidka.feature.financial.dto.FinancialChangeBatch
import org.orev.nahidka.feature.financial.store.FinancialCommit
import org.orev.nahidka.feature.financial.store.InternalFrame
import org.orev.nahidka.feature.financial.support.FinancialErrorReporter

internal interface WatchSource<T> {
    val frames: StateFlow<InternalFrame>
    val sessionLifetime: Job
    val errorReporter: FinancialErrorReporter
    fun currentFrame(): InternalFrame
    fun snapshot(frame: InternalFrame): EntitySnapshot<T>
    fun project(commit: FinancialCommit): FinancialChangeBatch<T>?
}
