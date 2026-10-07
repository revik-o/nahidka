package org.orev.nahidka.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

class ApplicationNavigator {

    private var backStack: PersistentList<ApplicationDestination> by mutableStateOf(persistentListOf(ApplicationDestination.DASHBOARD))

    val currentDestination: ApplicationDestination
        get() = backStack.last()

    val navigationDestination: ApplicationDestination
        get() = backStack.last(ApplicationDestination::shownInNavigation)

    val canNavigateBack: Boolean
        get() = !currentDestination.shownInNavigation

    fun open(destination: ApplicationDestination) {
        if (destination == currentDestination) {
            return
        }

        backStack = if (destination.shownInNavigation) {
            persistentListOf(destination)
        } else {
            backStack.adding(destination)
        }
    }

    fun navigateBack() {
        if (canNavigateBack) {
            backStack = backStack.removingAt(backStack.lastIndex)
        }
    }
}
