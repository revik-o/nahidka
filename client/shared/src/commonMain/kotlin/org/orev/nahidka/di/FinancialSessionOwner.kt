package org.orev.nahidka.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.*
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig

class FinancialSessionOwner internal constructor(
    val graph: FinancialSessionGraph,
) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private val closeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var closed = false

    fun close() {
        if (closed) return
        closed = true
        viewModelStore.clear()
        closeScope.launch {
            try {
                graph.gateway.close()
            } finally {
                closeScope.cancel()
            }
        }
    }
}

@Composable
fun rememberFinancialSession(config: FinancialSessionConfig): FinancialSessionOwner {
    val owner = remember(config) {
        FinancialSessionOwner(
            createGraphFactory<FinancialSessionGraph.Factory>().create(config),
        )
    }
    DisposableEffect(owner) {
        onDispose { owner.close() }
    }
    return owner
}
