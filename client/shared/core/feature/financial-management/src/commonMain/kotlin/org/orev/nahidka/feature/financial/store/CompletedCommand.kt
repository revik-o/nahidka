package org.orev.nahidka.feature.financial.store

import kotlinx.coroutines.CompletableDeferred
import org.orev.nahidka.feature.financial.dto.MutationResult

internal data class CompletedCommand(
    val result: MutationResult<CommandReceipt>,
    val released: CompletableDeferred<Unit>?,
)
