package org.orev.nahidka.ui.financialmanagement.common

import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.feature.financial.command.CommandMeta

internal fun IdentifierGenerator.nextCommandMeta(): CommandMeta = CommandMeta(next())
