package org.orev.nahidka

/** Opt-in diagnostics; the benchmark runner also measures time before main() starts. */
internal class DesktopStartupTrace {
    private val started = System.nanoTime()
    private val enabled = System.getenv("NAHIDKA_STARTUP_TRACE") == "1"

    init {
        if (enabled) println("NAHIDKA_STARTUP main")
    }

    fun onFirstFrame() {
        if (enabled) {
            val elapsedMs = (System.nanoTime() - started) / 1_000_000.0
            println("NAHIDKA_STARTUP dashboard_frame main_ms=$elapsedMs")
        }
    }
}
