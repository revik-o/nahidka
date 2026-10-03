package org.orev.nahidka.feature.financial.platform

import kotlin.js.JsModule

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

private val financialWasmTimeZones = JsJodaTimeZoneModule
