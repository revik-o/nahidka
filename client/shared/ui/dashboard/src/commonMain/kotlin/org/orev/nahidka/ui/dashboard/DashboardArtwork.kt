package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun Progress(value: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(7.dp).background(Edge, CircleShape)) {
        Box(Modifier.fillMaxWidth(value).fillMaxHeight().background(color, CircleShape))
    }
}

@Composable
internal fun Ring(progress: Float, label: String, modifier: Modifier, heart: Boolean = false) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = if (heart) 3.dp.toPx() else 9.dp.toPx()
            val inset = stroke / 2
            if (heart) drawCircle(Brush.radialGradient(listOf(Pink.copy(alpha = .32f), Color.Transparent)))
            drawArc(Edge, -90f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
            drawArc(Brush.sweepGradient(listOf(Purple, Pink, Purple)), -90f, progress * 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(label, color = if (heart) Pink else Color.White, fontSize = if (heart) 48.sp else 21.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun Sparkline() {
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val p = Path()
        p.moveTo(0f, size.height)
        p.cubicTo(size.width * .12f, 0f, size.width * .13f, size.height, size.width * .25f, size.height * .5f)
        p.cubicTo(size.width * .4f, -size.height * .2f, size.width * .4f, size.height, size.width * .55f, size.height * .4f)
        p.cubicTo(size.width * .7f, -size.height * .5f, size.width * .75f, size.height, size.width, 0f)
        drawPath(p, Pink, style = Stroke(2.dp.toPx()))
    }
}

@Composable
internal fun CameraArt(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF35333D), Offset(w * .05f, h * .28f), Size(w * .9f, h * .57f), androidx.compose.ui.geometry.CornerRadius(8f))
        drawRect(Color(0xFF45414E), Offset(w * .28f, h * .18f), Size(w * .38f, h * .18f))
        drawCircle(Color(0xFF080A0D), w * .29f, Offset(w * .53f, h * .57f))
        drawCircle(Color(0xFF535163), w * .23f, Offset(w * .53f, h * .57f), style = Stroke(3f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF205554), Color(0xFF0B1017))), w * .18f, Offset(w * .53f, h * .57f))
        drawCircle(Color(0xFF779FBA), w * .045f, Offset(w * .48f, h * .51f))
    }
}

@Composable
internal fun NightLandscape(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.radialGradient(listOf(Color(0xFF85265F).copy(alpha = .6f), Color(0xFF241340).copy(alpha = .4f), Color.Transparent), Offset(w * .7f, h * .75f), w * .65f))
        repeat(40) {
            i -> drawCircle(Color(0xFFDFBFFF).copy(alpha = .2f + (i % 4) * .12f), if (i % 5 == 0) 1.4f else .7f, Offset(w * ((i * 37 % 101) / 101f), h * ((i * 19 % 83) / 100f)))
        }
        drawCircle(Color(0xFFEEA5EE), h * .085f, Offset(w * .76f, h * .36f))
        drawCircle(Color(0xFF382041), h * .08f, Offset(w * .78f, h * .33f))
        repeat(3) {
            layer -> val p = Path()
            p.moveTo(0f, h)
            repeat(12) {
                i -> p.lineTo(w * i / 11f, h * (.63f + layer * .09f + ((i * 13 + layer * 7) % 17) / 100f))
            }
            p.lineTo(w, h)
            p.close()
            drawPath(p, listOf(Color(0xFF30203D), Color(0xFF19162C), Ink)[layer])
        }
        repeat(20) {
            i -> val x = w * i / 19
            val y = h * (.84f + (i % 3) * .03f)
            val p = Path()
            p.moveTo(x, y - h * .12f)
            p.lineTo(x - h * .04f, y)
            p.lineTo(x + h * .04f, y)
            p.close()
            drawPath(p, Ink)
        }
    }
}
