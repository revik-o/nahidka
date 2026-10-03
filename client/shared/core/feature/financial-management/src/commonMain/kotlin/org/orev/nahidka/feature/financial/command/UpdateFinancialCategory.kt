package org.orev.nahidka.feature.financial.command

data class UpdateFinancialCategory(
    val meta: CommandMeta,
    val id: String,
    val expectedVersion: Long,
    val name: String? = null,
    val iconName: NullablePatch<String> = NullablePatch.Keep,
)
