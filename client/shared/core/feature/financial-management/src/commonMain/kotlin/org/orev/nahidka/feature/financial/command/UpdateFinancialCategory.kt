package org.orev.nahidka.feature.financial.command

import org.orev.nahidka.core.common.NullablePatch

data class UpdateFinancialCategory(
    val meta: CommandMeta,
    val identifier: String,
    val expectedVersion: Long,
    val name: String? = null,
    val iconName: NullablePatch<String> = NullablePatch.Keep,
)
