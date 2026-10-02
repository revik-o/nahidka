package org.orev.nahidka.window

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.orev.nahidka.ui.common.theme.NahidkaBackground
import org.orev.nahidka.ui.common.theme.NahidkaError
import org.orev.nahidka.ui.common.theme.NahidkaOnBackground
import org.orev.nahidka.ui.common.theme.NahidkaPrimary
import org.orev.nahidka.ui.common.theme.NahidkaSurface

@Composable
internal fun NahidkaTitleBar(
    controller: DesktopWindowController,
    chrome: WindowChrome,
) {
    SideEffect { chrome.refreshState() }
    val density = LocalDensity.current.density
    val enabledControls = chrome.enabledControls
    val active = chrome.isActive
    val titleColor = NahidkaOnBackground.copy(alpha = if (active) 1f else 0.6f)
    Row(
        Modifier.fillMaxWidth().height(40.dp)
            .background(NahidkaBackground)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(NahidkaOnBackground.copy(alpha = 0.10f),
                    Offset(0f, size.height - stroke / 2),
                    Offset(size.width, size.height - stroke / 2), stroke)
            }
            .onGloballyPositioned {
                chrome.updateHeader(it.boundsInWindow(), density)
            }
            .padding(start = chrome.leftInset, end = chrome.rightInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chrome.Caption(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart) {
                Text("Nahidka", color = titleColor, fontSize = 13.sp)
            }
        }
        if (!chrome.controlsAreNative) {
            WindowControlButton(WindowControl.Minimize, "Minimize", false,
                chrome, WindowControl.Minimize in enabledControls, controller::minimize)
            WindowControlButton(WindowControl.Maximize,
                if (controller.isMaximized) "Restore" else "Maximize",
                controller.isMaximized, chrome, WindowControl.Maximize in enabledControls,
                controller::toggleMaximize)
            WindowControlButton(WindowControl.Close, "Close", false,
                chrome, WindowControl.Close in enabledControls, controller::close)
        }
    }
}

@Composable
private fun WindowControlButton(
    control: WindowControl,
    label: String,
    restoredIcon: Boolean,
    chrome: WindowChrome,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val native = chrome.interaction
    val isHovered = hovered || native.hovered == control
    val isPressed = pressed || native.pressed == control
    val allowPointerTooltip = !(chrome.usesNativeMaximizeHover &&
        control == WindowControl.Maximize)
    val tooltipEligible = enabled && chrome.isActive && !chrome.nativeMenuOpen &&
        ((isHovered && allowPointerTooltip) || (focused && !isHovered))
    var showTooltip by remember { mutableStateOf(false) }
    LaunchedEffect(tooltipEligible, isPressed, label) {
        showTooltip = false
        if (tooltipEligible && !isPressed) {
            delay(600)
            showTooltip = true
        }
    }
    val tooltipOffset = with(LocalDensity.current) { 44.dp.roundToPx() }
    val accent = if (control == WindowControl.Close) NahidkaError else NahidkaPrimary
    val background = when {
        !enabled -> NahidkaBackground
        isPressed -> accent.copy(alpha = 0.35f)
        isHovered -> accent.copy(alpha = 0.20f)
        else -> NahidkaBackground
    }
    DisposableEffect(chrome, control) {
        onDispose { chrome.updateControl(control, null) }
    }
    Box(
        Modifier.width(44.dp).height(40.dp)
            .onGloballyPositioned {
                chrome.updateControl(control, it.boundsInWindow())
            }
            .drawBehind { drawRect(background, size = Size(size.width, size.height - 1.dp.toPx())) }
            .then(if (focused) Modifier.border(1.dp, NahidkaPrimary) else Modifier)
            .semantics { contentDescription = label }
            .clickable(
                enabled = enabled,
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showTooltip) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, tooltipOffset),
                properties = PopupProperties(focusable = false),
            ) {
                Text(label, color = NahidkaOnBackground, fontSize = 12.sp,
                    modifier = Modifier.background(NahidkaSurface)
                        .border(1.dp, NahidkaOnBackground.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Canvas(Modifier.size(14.dp)) {
            val ink = NahidkaOnBackground.copy(alpha = when {
                !enabled -> 0.35f
                !chrome.isActive -> 0.6f
                else -> 1f
            })
            val stroke = 1.25.dp.toPx()
            when (control) {
                WindowControl.Minimize -> drawLine(
                    ink, Offset(2.dp.toPx(), size.height / 2),
                    Offset(size.width - 2.dp.toPx(), size.height / 2), stroke,
                )
                WindowControl.Maximize -> {
                    if (restoredIcon) {
                        drawRect(ink, Offset(5.dp.toPx(), 2.dp.toPx()),
                            Size(7.dp.toPx(), 7.dp.toPx()), style = Stroke(stroke))
                    }
                    drawRect(background, Offset(2.dp.toPx(), 5.dp.toPx()),
                        Size(7.dp.toPx(), 7.dp.toPx()))
                    drawRect(ink,
                        Offset(2.dp.toPx(), if (restoredIcon) 5.dp.toPx() else 2.dp.toPx()),
                        Size(if (restoredIcon) 7.dp.toPx() else 10.dp.toPx(),
                            if (restoredIcon) 7.dp.toPx() else 10.dp.toPx()),
                        style = Stroke(stroke))
                }
                WindowControl.Close -> {
                    drawLine(ink, Offset(3.dp.toPx(), 3.dp.toPx()),
                        Offset(11.dp.toPx(), 11.dp.toPx()), stroke)
                    drawLine(ink, Offset(11.dp.toPx(), 3.dp.toPx()),
                        Offset(3.dp.toPx(), 11.dp.toPx()), stroke)
                }
            }
        }
    }
}
