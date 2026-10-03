package org.orev.nahidka

internal class DesktopStartupTrace {
    private val started = System.nanoTime()
    private val enabled = System.getenv("NAHIDKA_STARTUP_TRACE") == "1"

    fun onFirstFrame() {
        if (enabled) {
            val elapsedMs = (System.nanoTime() - started) / 1_000_000.0
            println("NAHIDKA_STARTUP dashboard_frame main_ms=$elapsedMs")
        }
    }
}
