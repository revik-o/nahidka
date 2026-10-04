package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentList

data class FinancialSessionConfig(
    val sessionIdentity: String,
    val workspaceIdentity: String,
    val assets: PersistentList<AssetDefinition>,
    val defaultAssetIdentifier: String,
    val reportingTimeZone: String,
    val journalCapacity: Int = 256,
    val commandReceiptCapacity: Int = 512,
    val maxOperations: Int = 100_000,
    val maxCategories: Int = 512,
    val maxPlanningTables: Int = 1_000,
    val maxPlanningRows: Int = 500,
    val maxDescriptionLength: Int = 2_000,
    val maxCategoryNameLength: Int = 80,
) {

    init {
        require(sessionIdentity.isNotBlank())
        require(workspaceIdentity.isNotBlank())
        require(assets.isNotEmpty())
        require(assets.map { it.identifier }.toSet().size == assets.size)
        require(assets.all { it.identifier.isNotBlank() && it.displayCode.isNotBlank() && it.fractionDigits in 0..18 })
        require(assets.any { it.identifier == defaultAssetIdentifier })
        require(journalCapacity > 0)
        require(commandReceiptCapacity > 0)
        require(maxOperations > 0 && maxCategories > 0 && maxPlanningTables > 0 && maxPlanningRows > 0)
        require(maxDescriptionLength > 0 && maxCategoryNameLength > 0)
    }
}
