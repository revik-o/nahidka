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
        applicationNavigator.open(ApplicationDestination.TASKS)
        applicationNavigator.open(ApplicationDestination.NOTIFICATIONS)

        assertTrue(applicationNavigator.canNavigateBack)
        assertEquals(ApplicationDestination.TASKS, applicationNavigator.navigationDestination)

        applicationNavigator.navigateBack()

        assertEquals(ApplicationDestination.TASKS, applicationNavigator.currentDestination)
        assertFalse(applicationNavigator.canNavigateBack)
    }

    @Test
    fun navigationDestinationReplacesOpenedSecondaryDestinations() {
        applicationNavigator.open(ApplicationDestination.FINANCE)
        applicationNavigator.open(ApplicationDestination.GOALS)
        applicationNavigator.navigateBack()

        assertEquals(ApplicationDestination.GOALS, applicationNavigator.currentDestination)
    }
}
