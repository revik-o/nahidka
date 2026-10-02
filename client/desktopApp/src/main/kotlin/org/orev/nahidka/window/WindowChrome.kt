package org.orev.nahidka.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import java.util.Locale

internal enum class DesktopPlatform {
    Windows, MacOS, Linux;

    companion object {
        fun current(): DesktopPlatform {
            val name = System.getProperty("os.name").lowercase(Locale.ROOT)
            return when {
                name.startsWith("windows") -> Windows
                name.startsWith("mac") -> MacOS
                name.startsWith("linux") -> Linux
                else -> error("Unsupported desktop platform: $name")
            }
        }
    }
}

internal enum class WindowControl { Minimize, Maximize, Close }

internal data class NativeControlInteraction(
    val hovered: WindowControl? = null,
    val pressed: WindowControl? = null,
)

internal interface WindowChrome : AutoCloseable {
    val controlsAreNative: Boolean
    val isActive: Boolean
    val enabledControls: Set<WindowControl>
    val nativeMenuOpen: Boolean
    val usesNativeMaximizeHover: Boolean
    val leftInset: Dp
    val rightInset: Dp
    val contentInset: Dp
    val interaction: NativeControlInteraction

    fun refreshState()
    fun updateHeader(bounds: Rect, density: Float)
    fun updateControl(control: WindowControl, bounds: Rect?)

    @Composable
    fun Caption(modifier: Modifier, content: @Composable () -> Unit)

    @Composable
    fun FrameOverlay()

    override fun close()
}

internal interface WindowChromeFactory {
    fun prepare(window: ComposeWindow, controller: DesktopWindowController): WindowChrome
}

internal fun windowChromeFactory(platform: DesktopPlatform): WindowChromeFactory =
    when (platform) {
        DesktopPlatform.Windows -> WindowsWindowChromeFactory()
        DesktopPlatform.MacOS -> MacWindowChromeFactory()
        DesktopPlatform.Linux -> LinuxWindowChromeFactory()
    }

internal class WindowChromeHost(
    private val factory: WindowChromeFactory,
    private val controller: DesktopWindowController,
) : AutoCloseable {
    private var installed: WindowChrome? = null
    val chrome: WindowChrome get() = checkNotNull(installed)

    fun prepare(window: ComposeWindow) {
        check(installed == null)
        installed = factory.prepare(window, controller)
    }

    override fun close() {
        val current = installed ?: return
        installed = null
        current.close()
    }
}
