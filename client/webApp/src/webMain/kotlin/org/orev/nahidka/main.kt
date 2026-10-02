package org.orev.nahidka

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import web.dom.document
import web.dom.ElementId
import web.performance.performance

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    performance.mark("nahidka-main")
    ComposeViewport(viewportContainerId = "app") {
        App(onFirstFrame = {
            document.getElementById(ElementId("launch-splash"))?.remove()
            performance.mark("nahidka-dashboard-frame")
        })
    }
}
