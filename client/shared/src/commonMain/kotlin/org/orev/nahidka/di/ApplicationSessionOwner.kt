package org.orev.nahidka.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.*
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource

class ApplicationSessionOwner internal constructor(
    val graph: ApplicationSessionGraph,
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
                graph.financialGateway.close()
            } finally {
                closeScope.cancel()
            }
        }
    }
}

@Composable
fun rememberApplicationSession(
    financialSessionConfig: FinancialSessionConfig,
    settingsLocalDataSource: SettingsLocalDataSource,
): ApplicationSessionOwner {
    val applicationSessionOwner = remember(financialSessionConfig, settingsLocalDataSource) {
        ApplicationSessionOwner(
            createGraphFactory<ApplicationSessionGraph.Factory>().create(financialSessionConfig, settingsLocalDataSource),
        )
    }
    DisposableEffect(applicationSessionOwner) {
        onDispose { applicationSessionOwner.close() }
    }
    return applicationSessionOwner
}

@Composable
inline fun <reified SessionViewModel : ViewModel> ApplicationSessionOwner.sessionViewModel(
    noinline viewModelCreation: ApplicationSessionGraph.() -> SessionViewModel,
): SessionViewModel =
    viewModel(
        viewModelStoreOwner = this,
        key = SessionViewModel::class.simpleName,
    ) {
        graph.viewModelCreation()
    }
