package org.orev.nahidka.feature.financial.store

import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.ArchiveFinancialCategory
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.DeleteFinancialCategory
import org.orev.nahidka.feature.financial.command.DeletePlanningTable
import org.orev.nahidka.feature.financial.command.RemoveFinancialOperation
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.command.UpdateFinancialCategory
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation

internal sealed interface FinancialCommand {
    val meta: CommandMeta

    data class AddOperation(val command: AddFinancialOperation) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }

    data class UpdateOperation(val command: UpdateFinancialOperation) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }

    data class RemoveOperation(val command: RemoveFinancialOperation) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }

    data class SavePlanning(val command: SavePlanningTable) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }

    data class DeletePlanning(val command: DeletePlanningTable) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }

    data class CreateCategory(val command: CreateFinancialCategory) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }

    data class UpdateCategory(val command: UpdateFinancialCategory) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }

    data class ArchiveCategory(val command: ArchiveFinancialCategory) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }

    data class DeleteCategory(val command: DeleteFinancialCategory) : FinancialCommand {
        override val meta: CommandMeta get() = command.meta
    }
}
