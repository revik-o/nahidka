package org.orev.nahidka

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.newFullscreenControls
import org.jetbrains.jewel.window.styling.TitleBarStyle
import org.jetbrains.jewel.intui.window.styling.dark
import java.util.Locale

fun isMacOs(): Boolean {
    val osName = System.getProperty("os.name")
        .lowercase(Locale.getDefault())

    return osName.contains("mac") || osName.contains("darwin")
}

fun main() = application {
    val isMac = isMacOs()

    if (isMac) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Nahidka",
        ) {
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)

            App(titleBar = {
                Box(modifier = Modifier.fillMaxWidth().height(28.dp)) { }
            })
        }
    } else {
        val themeDefinition = JewelTheme.darkThemeDefinition()

        IntUiTheme(
            theme = themeDefinition,
            styling = ComponentStyling.default().decoratedWindow(
                titleBarStyle = TitleBarStyle.dark()
            ),
            swingCompatMode = false
        ) {
            DecoratedWindow(
                onCloseRequest = ::exitApplication,
                title = "Nahidka",
            ) {
                App(titleBar = {
                    TitleBar(Modifier.newFullscreenControls()) {
                        Text("Nahidka")
                    }
                })
            }
        }
    }
}
