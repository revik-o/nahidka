package org.orev.nahidka.navigation

import org.orev.nahidka.ui.common.navigation.ApplicationDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationNavigatorTest {

    private val applicationNavigator = ApplicationNavigator()

    @Test
    fun secondaryDestinationReturnsToTheDestinationThatOpenedIt() {
        listOf(ApplicationDestination.TASKS, ApplicationDestination.FINANCE).forEach { navigationDestination ->
            applicationNavigator.open(navigationDestination)
            applicationNavigator.open(ApplicationDestination.NOTIFICATIONS)

            assertTrue(applicationNavigator.canNavigateBack)
            assertEquals(navigationDestination, applicationNavigator.navigationDestination)

            applicationNavigator.navigateBack()

            assertEquals(navigationDestination, applicationNavigator.currentDestination)
            assertFalse(applicationNavigator.canNavigateBack)
        }
    }

    @Test
    fun navigationDestinationReplacesOpenedSecondaryDestinations() {
        applicationNavigator.open(ApplicationDestination.NOTIFICATIONS)
        applicationNavigator.open(ApplicationDestination.GOALS)
        applicationNavigator.navigateBack()

        assertEquals(ApplicationDestination.GOALS, applicationNavigator.currentDestination)
    }
}
