package org.orev.nahidka.ui.financialmanagement

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig

@Inject
@SingleIn(FinancialSessionScope::class)
class FinancialAssetSelection(financialSessionConfig: FinancialSessionConfig) {

    val availableAssets: PersistentList<AssetDefinition> = financialSessionConfig.assets

    private val mutableSelectedAsset = MutableStateFlow(
        availableAssets.first { asset -> asset.identifier == financialSessionConfig.defaultAssetIdentifier },
    )

    val selectedAsset: StateFlow<AssetDefinition> = mutableSelectedAsset.asStateFlow()

    fun select(asset: AssetDefinition) {
        mutableSelectedAsset.value = asset
    }
}
