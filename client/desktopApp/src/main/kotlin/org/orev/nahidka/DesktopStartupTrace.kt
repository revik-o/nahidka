package org.orev.nahidka

private const val NANOSECONDS_PER_MILLISECOND = 1_000_000.0

internal class DesktopStartupTrace {
    private val startedNanoseconds = System.nanoTime()
    private val enabled = System.getenv("NAHIDKA_STARTUP_TRACE") == "1"

    fun onSplashFrame() = report("splash_frame")

    fun onDashboardFrame() = report("dashboard_frame")

    private fun report(milestone: String) {
        if (enabled) {
            val elapsedMilliseconds = (System.nanoTime() - startedNanoseconds) / NANOSECONDS_PER_MILLISECOND
            println("NAHIDKA_STARTUP $milestone main_ms=$elapsedMilliseconds")
        }
    }
}
