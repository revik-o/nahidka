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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

private val WINDOW_CONTROL_WIDTH = 44.dp
private val WINDOW_CONTROL_ICON_SIZE = 14.dp
private const val WINDOW_CONTROL_TOOLTIP_DELAY_MILLISECONDS = 600L

@Composable
internal fun WindowControlButton(
    windowControl: WindowControl,
    accessibilityLabel: String,
    showRestoreIcon: Boolean,
    windowChrome: WindowChrome,
    onControlClick: () -> Unit,
) {
    val controlInteractionSource = remember { MutableInteractionSource() }
    val pointerHovered by controlInteractionSource.collectIsHoveredAsState()
    val pointerPressed by controlInteractionSource.collectIsPressedAsState()
    val keyboardFocused by controlInteractionSource.collectIsFocusedAsState()
    val isEnabled = windowControl in windowChrome.enabledControls
    val isHovered = pointerHovered || windowChrome.interaction.hovered == windowControl
    val isPressed = pointerPressed || windowChrome.interaction.pressed == windowControl
    val allowPointerTooltip = !(windowChrome.usesNativeMaximizeHover && windowControl == WindowControl.Maximize)
    val tooltipEligible = isEnabled && windowChrome.isActive && !windowChrome.nativeMenuOpen &&
        ((isHovered && allowPointerTooltip) || (keyboardFocused && !isHovered))
    var tooltipVisible by remember { mutableStateOf(false) }

    LaunchedEffect(tooltipEligible, isPressed, accessibilityLabel) {
        tooltipVisible = false
        if (tooltipEligible && !isPressed) {
            delay(WINDOW_CONTROL_TOOLTIP_DELAY_MILLISECONDS)
            tooltipVisible = true
        }
    }

    val tooltipVerticalOffset = with(LocalDensity.current) { (WINDOW_TITLE_BAR_HEIGHT + 4.dp).roundToPx() }
    val controlColorScheme = MaterialTheme.colorScheme
    val controlAccentColor = if (windowControl == WindowControl.Close) controlColorScheme.error else controlColorScheme.primary
    val controlBackgroundColor = when {
        !isEnabled -> controlColorScheme.background
        isPressed -> controlAccentColor.copy(alpha = 0.35f)
        isHovered -> controlAccentColor.copy(alpha = 0.20f)
        else -> controlColorScheme.background
    }

    DisposableEffect(windowChrome, windowControl) {
        onDispose { windowChrome.updateControl(windowControl, null) }
    }

    Box(
        modifier = Modifier
            .width(WINDOW_CONTROL_WIDTH)
            .height(WINDOW_TITLE_BAR_HEIGHT)
            .onGloballyPositioned { controlCoordinates ->
                windowChrome.updateControl(windowControl, controlCoordinates.boundsInWindow())
            }
            .drawBehind {
                drawRect(controlBackgroundColor)
            }
            .then(if (keyboardFocused) Modifier.border(1.dp, controlColorScheme.primary) else Modifier)
            .semantics { contentDescription = accessibilityLabel }
            .clickable(
                enabled = isEnabled,
                interactionSource = controlInteractionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = accessibilityLabel,
                onClick = onControlClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (tooltipVisible) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, tooltipVerticalOffset),
                properties = PopupProperties(focusable = false),
            ) {
                Text(
                    text = accessibilityLabel,
                    color = controlColorScheme.onBackground,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(controlColorScheme.surface)
                        .border(1.dp, controlColorScheme.onBackground.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Canvas(Modifier.size(WINDOW_CONTROL_ICON_SIZE)) {
            val controlIconColor = controlColorScheme.onBackground.copy(
                alpha = when {
                    !isEnabled -> 0.35f
                    !windowChrome.isActive -> 0.6f
                    else -> 1f
                },
            )
            val controlIconStrokeWidth = 1.25.dp.toPx()
            when (windowControl) {
                WindowControl.Minimize -> drawLine(
                    color = controlIconColor,
                    start = Offset(2.dp.toPx(), size.height / 2),
                    end = Offset(size.width - 2.dp.toPx(), size.height / 2),
                    strokeWidth = controlIconStrokeWidth,
                )
                WindowControl.Maximize -> {
                    if (showRestoreIcon) {
                        drawRect(
                            color = controlIconColor,
                            topLeft = Offset(5.dp.toPx(), 2.dp.toPx()),
                            size = Size(7.dp.toPx(), 7.dp.toPx()),
                            style = Stroke(controlIconStrokeWidth),
                        )
                    }
                    drawRect(
                        color = controlBackgroundColor,
                        topLeft = Offset(2.dp.toPx(), 5.dp.toPx()),
                        size = Size(7.dp.toPx(), 7.dp.toPx()),
                    )
                    drawRect(
                        color = controlIconColor,
                        topLeft = Offset(2.dp.toPx(), if (showRestoreIcon) 5.dp.toPx() else 2.dp.toPx()),
                        size = if (showRestoreIcon) Size(7.dp.toPx(), 7.dp.toPx()) else Size(10.dp.toPx(), 10.dp.toPx()),
                        style = Stroke(controlIconStrokeWidth),
                    )
                }
                WindowControl.Close -> {
                    drawLine(
                        color = controlIconColor,
                        start = Offset(3.dp.toPx(), 3.dp.toPx()),
                        end = Offset(11.dp.toPx(), 11.dp.toPx()),
                        strokeWidth = controlIconStrokeWidth,
                    )
                    drawLine(
                        color = controlIconColor,
                        start = Offset(11.dp.toPx(), 3.dp.toPx()),
                        end = Offset(3.dp.toPx(), 11.dp.toPx()),
                        strokeWidth = controlIconStrokeWidth,
                    )
                }
            }
        }
    }
}
