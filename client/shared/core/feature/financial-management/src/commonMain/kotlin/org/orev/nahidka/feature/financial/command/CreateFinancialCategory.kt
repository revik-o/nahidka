package org.orev.nahidka.feature.financial.command

data class CreateFinancialCategory(
    val meta: CommandMeta,
    val identifier: String,
    val name: String,
    val iconName: String? = null,
)
