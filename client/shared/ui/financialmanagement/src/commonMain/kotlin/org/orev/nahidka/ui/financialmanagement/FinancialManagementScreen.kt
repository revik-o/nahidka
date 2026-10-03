package org.orev.nahidka.ui.financialmanagement

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.feature.financial.calculation.formatMoneyInput
import org.orev.nahidka.feature.financial.calculation.parseMoneyText
import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput
import org.orev.nahidka.feature.financial.command.PlanningRowInput
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.dto.PlanningConfigured
import org.orev.nahidka.feature.financial.dto.PlanningNotConfigured
import org.orev.nahidka.feature.financial.dto.SavingsGuidelinePolicy
import org.orev.nahidka.ui.models.TransactionEntity

@Composable
fun FinancialManagementScreen(
    viewModel: FinancialManagementViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var selectMode by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<FinancialOperation?>(null) }
    var editingCategory by remember { mutableStateOf<FinancialCategory?>(null) }
    var showCreateCategory by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Finance manager", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("Back") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.SelectMonth(state.selectedMonth.previousMonth())) }) { Text("‹") }
                Text(state.selectedMonth.toString(), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.SelectMonth(state.selectedMonth.nextMonth())) }) { Text("›") }
                Spacer(Modifier.weight(1f))
                ChoiceField(
                    label = "Asset",
                    selected = state.selectedAssetId,
                    options = viewModel.supportedAssets.map { it.id to it.displayCode },
                    onSelect = { viewModel.handleEvent(FinancialManagementEvent.SelectAsset(it)) },
                )
            }
            state.feedback?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val snapshot = state.snapshot
            if (state.isLoading || snapshot == null) {
                Text("Loading this month’s financial data…")
            } else {
                FinancialTotals(viewModel, snapshot.planning)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.handleEvent(FinancialManagementEvent.OpenAddOperation) }) { Text("Add operation") }
                    OutlinedButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.OpenPlanningEditor) }) { Text("Edit monthly plan") }
                    OutlinedButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.OpenCategoryEditor) }) { Text("Categories") }
                    TextButton(onClick = { selectMode = !selectMode }) { Text(if (selectMode) "Done selecting" else "Select") }
                }

                Text("Operations", style = MaterialTheme.typography.titleLarge)
                val rows = snapshot.operations.map { it.toTransactionEntity(viewModel, snapshot.categories) }
                if (rows.isEmpty()) Text("No posted operations for this asset and month.")
                else FinancialManagementTable(
                    transactions = rows,
                    selectedIds = state.selectedIds,
                    selectMode = selectMode,
                    onToggleSelect = { viewModel.handleEvent(FinancialManagementEvent.ToggleSelection(it.id)) },
                    onEdit = { viewModel.handleEvent(FinancialManagementEvent.EditOperation(it.id)) },
                    onDelete = { row -> deleteCandidate = snapshot.operations.firstOrNull { it.id == row.id } },
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                )

                Text("Monthly plan", style = MaterialTheme.typography.titleLarge)
                PlanningTable(viewModel, snapshot.planning, snapshot.period.endExclusive <= viewModel.now())
                Text("Categories", style = MaterialTheme.typography.titleLarge)
                snapshot.categories.forEach { category ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(category.name + if (category.archived) " · archived" else "", modifier = Modifier.weight(1f))
                        TextButton(onClick = { editingCategory = category }) { Text("Manage") }
                    }
                }
                Text("Payment methods record the asset used on each posted operation. Account balances, card liabilities, transfers and currency conversion are outside this ledger.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    state.operationDraft?.let { draft ->
        OperationEditor(viewModel, state, draft)
    }
    if (state.isCategoryEditorOpen) {
        CategoryEditor(
            state = state,
            onCreate = { name, icon -> viewModel.handleEvent(FinancialManagementEvent.CreateCategory(name, icon)) },
            onManage = { editingCategory = it },
            onClose = { viewModel.handleEvent(FinancialManagementEvent.CloseCategoryEditor) },
            onCreateOpen = { showCreateCategory = true },
        )
    }
    if (state.isPlanningEditorOpen) PlanningEditor(viewModel, state)
    if (showCreateCategory) {
        var name by remember { mutableStateOf("") }
        var icon by remember { mutableStateOf("") }
        val openingToken = remember { state.categorySaveToken }
        LaunchedEffect(state.categorySaveToken) { if (state.categorySaveToken != openingToken) showCreateCategory = false }
        AlertDialog(
            onDismissRequest = { showCreateCategory = false },
            title = { Text("New category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(icon, { icon = it }, label = { Text("Icon name (optional)") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.CreateCategory(name, icon.takeIf(String::isNotBlank))) }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreateCategory = false }) { Text("Cancel") } },
        )
    }
    editingCategory?.let { category ->
        var name by remember(category.id, category.version) { mutableStateOf(category.name) }
        val openingToken = remember(category.id) { state.categorySaveToken }
        LaunchedEffect(state.categorySaveToken, category.id) { if (state.categorySaveToken != openingToken) editingCategory = null }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Manage ${category.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Category name") }, singleLine = true)
                    if (!category.archived) TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.ArchiveCategory(category.id, category.version)) }) { Text("Archive") }
                    TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.DeleteCategory(category.id, category.version)) }) { Text("Delete if unused") }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.RenameCategory(category.id, category.version, name)) }) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text("Close") } },
        )
    }
    deleteCandidate?.let { operation ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Remove operation?") },
            text = { Text(operation.description ?: "This operation will be removed from the ledger.") },
            confirmButton = { TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.RemoveOperation(operation.id)); deleteCandidate = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun FinancialTotals(viewModel: FinancialManagementViewModel, planning: org.orev.nahidka.feature.financial.dto.PlanningState) {
    when (planning) {
        is PlanningConfigured -> {
            val totals = planning.table.totals
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Funds available this month: ${viewModel.formatMoney(totals.fundsAvailableThisMonth)}")
                Text("Posted expenses: ${viewModel.formatMoney(totals.grossExpenses)} · refunds: ${viewModel.formatMoney(totals.refunds)}")
                Text("Current available: ${viewModel.formatMoney(totals.currentAvailable)}")
                Text("Included budget: ${viewModel.formatMoney(totals.includedBudget)} · remaining reservation: ${viewModel.formatMoney(totals.remainingReservation)}")
                Text("Projected available after planning: ${viewModel.formatMoney(totals.projectedAvailableAfterPlanning)}")
                Text("Unplanned net expense: ${viewModel.formatMoney(totals.unplannedNetExpense)}")
                Text(totals.suggestedSavings?.let { "Savings guideline: ${viewModel.formatMoney(it)}" } ?: "Savings guideline unavailable · set a reserve and allocation rate")
            }
        }
        is PlanningNotConfigured -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Plan and opening amount not configured")
            Text("Posted income: ${viewModel.formatMoney(planning.income)}")
            Text("Gross expenses: ${viewModel.formatMoney(planning.grossExpenses)} · refunds: ${viewModel.formatMoney(planning.refunds)}")
            Text("Net expenses: ${viewModel.formatMoney(planning.netExpense)}")
            Text("Available funds and forecast remain unavailable until you create a monthly plan.")
        }
    }
}

@Composable
private fun PlanningTable(viewModel: FinancialManagementViewModel, planning: org.orev.nahidka.feature.financial.dto.PlanningState, monthComplete: Boolean) {
    val configured = planning as? PlanningConfigured ?: return
    val table = configured.table
    val savedLabel = if (monthComplete) "Category underspend" else "Remaining budget / provisional savings"
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row {
            TableHeader("Category / topic", 150.dp)
            TableHeader("Monthly planned cost", 145.dp)
            TableHeader("Take into account", 130.dp)
            TableHeader("Already spent", 170.dp)
            TableHeader(savedLabel, 180.dp)
            TableHeader("Payment made", 210.dp)
        }
        table.rows.forEach { row ->
            val payment = PaymentMethod.entries.joinToString(" · ") { method ->
                val actual = row.actualByPaymentMethod[method]?.netSpent
                "${method.displayName()} ${actual?.let(viewModel::formatMoney) ?: viewModel.formatMoney(Money(row.input.plannedAmount.assetId, 0))}"
            }
            Row(verticalAlignment = Alignment.Top) {
                TableValue(row.categoryName, 150.dp)
                TableValue(viewModel.formatMoney(row.input.plannedAmount), 145.dp)
                TableValue(if (row.input.included) "Included" else "Excluded", 130.dp)
                TableValue("${viewModel.formatMoney(row.netSpent)}\nGross ${viewModel.formatMoney(row.grossSpent)} · refunds ${viewModel.formatMoney(row.refunded)}", 170.dp)
                TableValue(viewModel.formatMoney(row.remainingBudget), 180.dp)
                TableValue("${row.input.preferredPaymentMethod?.displayName() ?: "Any method"}\n$payment", 210.dp)
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(text, Modifier.width(width).padding(8.dp), style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun TableValue(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(text, Modifier.width(width).padding(8.dp), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun OperationEditor(viewModel: FinancialManagementViewModel, state: FinancialManagementState, initial: FinancialOperationDraft) {
    val draft = initial
    val categories = state.snapshot?.categories.orEmpty()
    val selectedAsset = viewModel.assetDefinition(draft.assetId)
    AlertDialog(
        onDismissRequest = { viewModel.handleEvent(FinancialManagementEvent.CloseOperationEditor) },
        title = { Text(if (draft.original == null) "Add financial operation" else "Edit financial operation") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedAsset != null) {
                    OutlinedTextField(
                        draft.amountText,
                        { viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(amountText = it))) },
                        label = { Text("Amount · ${selectedAsset.displayCode}") },
                        supportingText = { Text("Positive exact amount; up to ${selectedAsset.fractionDigits} decimal places") },
                        singleLine = true,
                    )
                }
                ChoiceField("Asset", draft.assetId, viewModel.supportedAssets.map { it.id to it.displayCode }) {
                    viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(assetId = it, amountText = if (it == draft.original?.amount?.assetId) viewModel.amountInput(draft.original.amount) else "")))
                }
                ChoiceField("Kind", draft.kind, OperationKind.entries.map { it to it.displayName() }) {
                    viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(kind = it, refundOfOperationId = if (it == OperationKind.REFUND) draft.refundOfOperationId else null)))
                }
                ChoiceField(
                    "Category",
                    draft.categoryId,
                    (categories.filter { !it.archived || it.id == draft.categoryId }.map { it.id to (it.name + if (it.archived) " · archived" else "") } + listOf(null to "No category")),
                ) { viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(categoryId = it))) }
                if (categories.none { !it.archived || it.id == draft.categoryId }) {
                    Text("Create a category before adding an expense.")
                    TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.OpenCategoryEditor) }) { Text("Manage categories") }
                }
                ChoiceField("Payment method", draft.paymentMethod, PaymentMethod.entries.map { it to it.displayName() }) {
                    viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(paymentMethod = it)))
                }
                if (draft.kind == OperationKind.REFUND) {
                    val refundOptions = state.operations.filter { it.kind == OperationKind.EXPENSE && it.amount.assetId == draft.assetId }
                    ChoiceField("Refund of", draft.refundOfOperationId, refundOptions.map { it.id to "${it.description ?: it.id} · ${viewModel.formatMoney(it.amount)}" }) {
                        viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(refundOfOperationId = it)))
                    }
                }
                OutlinedTextField(
                    draft.occurredAtText,
                    { viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(occurredAtText = it))) },
                    label = { Text("Occurrence date and time (ISO-8601 with offset)") },
                    supportingText = { Text("Example: 2026-10-03T14:30:00+03:00") },
                    singleLine = true,
                )
                OutlinedTextField(
                    draft.descriptionText,
                    { viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(descriptionText = it))) },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                )
                state.feedback?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.conflictingOperation?.let {
                    Text("The ledger changed while you were editing. Reload its current values or review and retry your edits against version ${it.version}.", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.ReloadConflictingOperation) }) { Text("Reload current values") }
                    TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.RetryConflictingOperation) }) { Text("Keep my edits and retry") }
                }
            }
        },
        confirmButton = { TextButton(enabled = !state.isSavingOperation, onClick = { viewModel.handleEvent(FinancialManagementEvent.SaveOperation) }) { Text(if (state.isSavingOperation) "Saving…" else "Save") } },
        dismissButton = { TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.CloseOperationEditor) }) { Text("Cancel") } },
    )
}

@Composable
private fun CategoryEditor(
    state: FinancialManagementState,
    onCreate: (String, String?) -> Unit,
    onManage: (FinancialCategory) -> Unit,
    onClose: () -> Unit,
    onCreateOpen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Financial categories") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onCreateOpen) { Text("Create category") }
                state.snapshot?.categories.orEmpty().forEach { category ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(category.name + if (category.archived) " · archived" else "", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onManage(category) }) { Text("Manage") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
    )
}

@Composable
private fun <T> ChoiceField(label: String, selected: T, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: "Choose"
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = { expanded = true }) {
            Text(selectedLabel)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, title) ->
                    DropdownMenuItem(text = { Text(title) }, onClick = { expanded = false; onSelect(value) })
                }
            }
        }
    }
}

@Composable
private fun PlanningEditor(viewModel: FinancialManagementViewModel, state: FinancialManagementState) {
    val configured = (state.snapshot?.planning as? PlanningConfigured)?.table
    val existing = configured?.document?.input
    val categories = state.snapshot?.categories.orEmpty().filter { !it.archived || existing?.rows?.any { row -> row.categoryId == it.id } == true }
    val asset = viewModel.assetDefinition(state.selectedAssetId) ?: return
    val documentVersion = configured?.document?.version
    var opening by remember(state.selectedMonth, state.selectedAssetId, documentVersion) {
        mutableStateOf(existing?.openingAvailable?.let { formatMoneyInput(it, asset) } ?: "")
    }
    var reserve by remember(state.selectedMonth, state.selectedAssetId, documentVersion) {
        mutableStateOf(existing?.savingsPolicy?.reserve?.let { formatMoneyInput(it, asset) } ?: "")
    }
    var allocationRate by remember(state.selectedMonth, state.selectedAssetId, documentVersion) {
        mutableStateOf(existing?.savingsPolicy?.allocationBasisPoints?.let { formatBasisPoints(it) } ?: "")
    }
    val initialRows = remember(state.selectedMonth, state.selectedAssetId, documentVersion, categories.map { it.id }) {
        existing?.rows?.associateBy { it.categoryId }.orEmpty()
    }
    val amounts = remember(state.selectedMonth, state.selectedAssetId, documentVersion, categories.map { it.id }) {
        mutableStateMapOf<String, String>().apply {
            categories.forEach { category -> put(category.id, initialRows[category.id]?.plannedAmount?.let { formatMoneyInput(it, asset) } ?: "0") }
        }
    }
    val included = remember(state.selectedMonth, state.selectedAssetId, documentVersion, categories.map { it.id }) {
        mutableStateMapOf<String, Boolean>().apply { categories.forEach { put(it.id, initialRows[it.id]?.included ?: true) } }
    }
    val methods = remember(state.selectedMonth, state.selectedAssetId, documentVersion, categories.map { it.id }) {
        mutableStateMapOf<String, PaymentMethod?>().apply { categories.forEach { put(it.id, initialRows[it.id]?.preferredPaymentMethod) } }
    }
    var validationError by remember(state.selectedMonth, documentVersion) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { viewModel.handleEvent(FinancialManagementEvent.ClosePlanningEditor) },
        title = { Text("Monthly plan · ${state.selectedMonth}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(opening, { opening = it }, label = { Text("Opening available · ${asset.displayCode}") }, singleLine = true)
                OutlinedTextField(reserve, { reserve = it }, label = { Text("Savings reserve (optional)") }, singleLine = true)
                OutlinedTextField(allocationRate, { allocationRate = it }, label = { Text("Savings allocation rate % (optional)") }, singleLine = true)
                Text("Category budgets", style = MaterialTheme.typography.titleSmall)
                categories.forEach { category ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(category.name + if (category.archived) " · archived" else "")
                        OutlinedTextField(amounts[category.id].orEmpty(), { amounts[category.id] = it }, label = { Text("Monthly planned cost") }, singleLine = true)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = included[category.id] ?: true, onCheckedChange = { included[category.id] = it })
                            Text("Take into account")
                        }
                        ChoiceField("Preferred payment", methods[category.id], listOf(null to "Any method") + PaymentMethod.entries.map { it to it.displayName() }) {
                            methods[category.id] = it
                        }
                    }
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.feedback?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !state.isSavingPlanning, onClick = {
                val openingMoney = parseMoneyText(opening, asset)
                if (openingMoney == null) {
                    validationError = "Enter an opening amount with up to ${asset.fractionDigits} decimal places"
                    return@TextButton
                }
                val reserveMoney = if (reserve.isBlank()) null else parseMoneyText(reserve, asset)
                if (reserve.isNotEmpty() && (reserveMoney == null || reserveMoney.units < 0)) {
                    validationError = "Savings reserve must be a non-negative amount"
                    return@TextButton
                }
                val basisPoints = if (allocationRate.isBlank()) null else parseBasisPoints(allocationRate)
                if (allocationRate.isNotBlank() && basisPoints == null) {
                    validationError = "Allocation rate must be between 0 and 100 with up to two decimals"
                    return@TextButton
                }
                if ((reserveMoney == null) != (basisPoints == null)) {
                    validationError = "Enter both a reserve and an allocation rate, or leave both blank"
                    return@TextButton
                }
                val rows = mutableListOf<PlanningRowInput>()
                for (category in categories) {
                    val planned = parseMoneyText(amounts[category.id].orEmpty(), asset)
                    if (planned == null || planned.units < 0) {
                        validationError = "${category.name}: enter a non-negative planned amount"
                        return@TextButton
                    }
                    rows += PlanningRowInput(
                        id = initialRows[category.id]?.id ?: viewModel.newId(),
                        categoryId = category.id,
                        plannedAmount = planned,
                        included = included[category.id] ?: true,
                        preferredPaymentMethod = methods[category.id],
                    )
                }
                val input = FinancialPlanningTableInput(
                    id = existing?.id ?: viewModel.newId(),
                    month = state.selectedMonth,
                    assetId = asset.id,
                    openingAvailable = openingMoney,
                    savingsPolicy = if (reserveMoney != null && basisPoints != null) SavingsGuidelinePolicy(reserveMoney, basisPoints) else null,
                    rows = rows.toPersistentList(),
                )
                validationError = null
                viewModel.handleEvent(FinancialManagementEvent.SavePlanningTable(input, documentVersion))
            }) { Text(if (state.isSavingPlanning) "Saving…" else "Save plan") }
        },
        dismissButton = { TextButton(onClick = { viewModel.handleEvent(FinancialManagementEvent.ClosePlanningEditor) }) { Text("Cancel") } },
    )
}

private fun FinancialOperation.toTransactionEntity(viewModel: FinancialManagementViewModel, categories: List<FinancialCategory>) =
    TransactionEntity(
        id = id,
        version = version,
        title = description?.takeIf { it.isNotBlank() } ?: "${kind.displayName()} · ${categories.firstOrNull { it.id == categoryId }?.name ?: "Uncategorized"}",
        formattedAmount = (if (kind == OperationKind.EXPENSE) "−" else "+") + viewModel.formatMoney(amount),
        isOutflow = kind == OperationKind.EXPENSE,
        iconName = categories.firstOrNull { it.id == categoryId }?.iconName ?: "money",
    )

private fun OperationKind.displayName(): String = when (this) {
    OperationKind.INCOME -> "Income"
    OperationKind.EXPENSE -> "Expense"
    OperationKind.REFUND -> "Refund"
}

private fun PaymentMethod.displayName(): String = when (this) {
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.CARD -> "Card"
    PaymentMethod.CRYPTOCURRENCY -> "Cryptocurrency"
}

private fun parseBasisPoints(text: String): Int? {
    val parts = text.trim().split('.')
    if (parts.size > 2 || parts[0].isEmpty() || parts[0].any { it !in '0'..'9' }) return null
    val fraction = parts.getOrElse(1) { "" }
    if (fraction.length > 2 || fraction.any { it !in '0'..'9' }) return null
    val whole = parts[0].toIntOrNull() ?: return null
    val hundredths = (fraction + "00").take(2).toIntOrNull() ?: return null
    val result = whole * 100 + hundredths
    return result.takeIf { it in 0..10_000 }
}

private fun formatBasisPoints(value: Int): String = "${value / 100}.${(value % 100).toString().padStart(2, '0')}"

private fun kotlinx.datetime.YearMonth.previousMonth() = if (month.ordinal == 0) kotlinx.datetime.YearMonth(year - 1, 12) else kotlinx.datetime.YearMonth(year, month.ordinal)
private fun kotlinx.datetime.YearMonth.nextMonth() = if (month.ordinal == 11) kotlinx.datetime.YearMonth(year + 1, 1) else kotlinx.datetime.YearMonth(year, month.ordinal + 2)
