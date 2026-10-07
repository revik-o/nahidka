package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val PARTICLE_COUNT = 96
private const val PARTICLE_RANDOM_SEED = 2026
private const val PARTICLE_ANIMATION_DURATION_MILLISECONDS = 18_000
private const val MAXIMUM_PARTICLE_TRAVEL_CYCLES = 4
private const val FULL_TURN = 2 * PI.toFloat()
private const val GLOW_ALPHA = 0.18f
private val MINIMUM_PARTICLE_RADIUS = 1.5.dp
private val PARTICLE_RADIUS_SPREAD = 2.5.dp

private val BATTERY_PARTICLES = Random(PARTICLE_RANDOM_SEED).let { random ->
    List(PARTICLE_COUNT) { particleIndex ->
        BatteryParticle(
            direction = random.nextFloat() * FULL_TURN,
            travelOffset = random.nextFloat(),
            travelCycles = random.nextInt(1, MAXIMUM_PARTICLE_TRAVEL_CYCLES + 1),
            radius = MINIMUM_PARTICLE_RADIUS + PARTICLE_RADIUS_SPREAD * random.nextFloat(),
            chargeThreshold = particleIndex.toFloat() / PARTICLE_COUNT,
        )
    }
}

@Composable
internal fun BatteryParticles(
    chargeFraction: Float,
    chargeColor: Color,
    batterySize: DpSize,
    modifier: Modifier = Modifier,
) {
    val animationProgress by rememberInfiniteTransition()
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(PARTICLE_ANIMATION_DURATION_MILLISECONDS, easing = LinearEasing)),
        )

    Canvas(modifier) {
        val emissionRadius = Offset(batterySize.width.toPx() / 2, batterySize.height.toPx() / 2)
        val travelDistance = center - emissionRadius

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(chargeColor.copy(alpha = GLOW_ALPHA * chargeFraction), Color.Transparent),
                center = center,
                radius = size.maxDimension / 2,
            ),
            radius = size.maxDimension / 2,
        )
        BATTERY_PARTICLES
            .filter { batteryParticle -> batteryParticle.chargeThreshold < chargeFraction }
            .forEach { batteryParticle ->
                val travelProgress = batteryParticle.travelProgress(animationProgress)

                drawCircle(
                    color = chargeColor,
                    radius = batteryParticle.radius.toPx(),
                    center = center + Offset(
                        x = cos(batteryParticle.direction) * (emissionRadius.x + travelDistance.x * travelProgress),
                        y = sin(batteryParticle.direction) * (emissionRadius.y + travelDistance.y * travelProgress),
                    ),
                    alpha = 1f - travelProgress,
                )
            }
    }
}
