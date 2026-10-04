package org.orev.nahidka.ui.financialmanagement

import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.ui.financialmanagement.common.FinancialContentState
import org.orev.nahidka.ui.financialmanagement.common.availableContentOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class FinancialViewModelTest {

    protected val financialManagementTestGraph = createGraph<FinancialManagementTestGraph>()

    protected val identifierGenerator = RandomIdentifierGenerator()

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    protected suspend fun createCategory(categoryName: String): FinancialCategory {
        val categoryCreation = financialManagementTestGraph.financialGateway.createCategory(
            CreateFinancialCategory(
                meta = CommandMeta(identifierGenerator.next()),
                identifier = identifierGenerator.next(),
                name = categoryName,
                iconName = "🍔",
            ),
        )

        return (categoryCreation as MutationResult.Committed).value
    }

    protected suspend fun <Content> StateFlow<FinancialContentState<Content>>.awaitContent(
        contentExpectation: (Content) -> Boolean,
    ): Content = checkNotNull(
        first { contentState -> contentState.availableContentOrNull()?.let(contentExpectation) == true }
            .availableContentOrNull(),
    )
}
