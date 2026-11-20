package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.data.BobAccessory
import com.example.data.BobAppearance
import com.example.data.BobEyeStyle
import com.example.data.BobShellStyle

@Composable
fun BobAvatar(
    appearance: BobAppearance,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "bob_idle")
    val bobFloat by transition.animateFloat(
        initialValue = -0.025f,
        targetValue = 0.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 850 else 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob_float"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isActive) 0.75f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 700 else 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob_pulse"
    )

    val accent = Color(appearance.accentColor.colorHex)
    val shellHighlight = accent.copy(alpha = 0.92f)
    val shellDark = Color(0xFF1A2233)
    val visorBg = Color(0xFF0B1220)

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Bob assistant avatar in ${appearance.shellStyle.label} style"
        }
    ) {
        val w = size.width
        val h = size.height
        val floatOffsetY = h * bobFloat
        val cx = w / 2f
        val cy = h / 2f + floatOffsetY

        // Ambient aura glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = pulseAlpha), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.52f
            ),
            radius = w * 0.52f,
            center = Offset(cx, cy)
        )

        // Accessory behind/top: Antenna Beacon or Cooling Halo
        when (appearance.accessory) {
            BobAccessory.ANTENNA_BEACON -> {
                drawLine(
                    color = accent,
                    start = Offset(cx, cy - h * 0.28f),
                    end = Offset(cx, cy - h * 0.43f),
                    strokeWidth = w * 0.045f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = w * 0.065f,
                    center = Offset(cx, cy - h * 0.44f)
                )
            }
            BobAccessory.COOLING_HALO -> {
                drawArc(
                    color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                    startAngle = 195f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.34f, cy - h * 0.44f),
                    size = Size(w * 0.68f, h * 0.24f),
                    style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
                )
            }
            else -> Unit
        }

        // Bob Shell Body
        val bodyW = w * 0.64f
        val bodyH = h * 0.56f
        val bodyLeft = cx - bodyW / 2f
        val bodyTop = cy - bodyH / 2f + h * 0.04f

        when (appearance.shellStyle) {
            BobShellStyle.CORE_BOT -> {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(shellHighlight, shellDark)),
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                    style = Stroke(width = w * 0.025f)
                )
            }
            BobShellStyle.TACTICAL_MECH -> {
                val mechPath = Path().apply {
                    moveTo(cx - bodyW * 0.38f, bodyTop)
                    lineTo(cx + bodyW * 0.38f, bodyTop)
                    lineTo(cx + bodyW * 0.52f, bodyTop + bodyH * 0.28f)
                    lineTo(cx + bodyW * 0.45f, bodyTop + bodyH)
                    lineTo(cx - bodyW * 0.45f, bodyTop + bodyH)
                    lineTo(cx - bodyW * 0.52f, bodyTop + bodyH * 0.28f)
                    close()
                }
                drawPath(
                    path = mechPath,
                    brush = Brush.verticalGradient(listOf(shellHighlight, shellDark))
                )
                drawPath(
                    path = mechPath,
                    color = Color.White.copy(alpha = 0.26f),
                    style = Stroke(width = w * 0.028f)
                )
            }
            BobShellStyle.SLEEK_VISOR -> {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(shellHighlight, Color(0xFF121B2B))),
                    topLeft = Offset(bodyLeft - w * 0.03f, bodyTop + h * 0.03f),
                    size = Size(bodyW + w * 0.06f, bodyH * 0.90f),
                    cornerRadius = CornerRadius(w * 0.28f, w * 0.28f)
                )
            }
            BobShellStyle.FRIENDLY_ORB -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(shellHighlight, shellDark),
                        center = Offset(cx - w * 0.08f, cy - h * 0.05f)
                    ),
                    radius = bodyW * 0.53f,
                    center = Offset(cx, cy + h * 0.03f)
                )
            }
        }

        // Faceplate Visor
        val visorW = bodyW * 0.74f
        val visorH = bodyH * 0.44f
        val visorLeft = cx - visorW / 2f
        val visorTop = bodyTop + bodyH * 0.22f
        drawRoundRect(
            color = visorBg,
            topLeft = Offset(visorLeft, visorTop),
            size = Size(visorW, visorH),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
        )

        // Eyes inside visor
        val eyeY = visorTop + visorH * 0.5f
        val leftEyeX = cx - visorW * 0.23f
        val rightEyeX = cx + visorW * 0.23f
        val eyeGlowColor = Color(0xFF67E8F9)

        when (appearance.eyeStyle) {
            BobEyeStyle.FRIENDLY_GLOW -> {
                drawCircle(color = eyeGlowColor, radius = w * 0.052f, center = Offset(leftEyeX, eyeY))
                drawCircle(color = eyeGlowColor, radius = w * 0.052f, center = Offset(rightEyeX, eyeY))
                drawCircle(color = Color.White, radius = w * 0.018f, center = Offset(leftEyeX - w * 0.014f, eyeY - h * 0.014f))
                drawCircle(color = Color.White, radius = w * 0.018f, center = Offset(rightEyeX - w * 0.014f, eyeY - h * 0.014f))
            }
            BobEyeStyle.CYBER_VISOR -> {
                drawLine(
                    color = eyeGlowColor,
                    start = Offset(visorLeft + visorW * 0.14f, eyeY),
                    end = Offset(visorLeft + visorW * 0.86f, eyeY),
                    strokeWidth = w * 0.055f,
                    cap = StrokeCap.Round
                )
            }
            BobEyeStyle.PIXEL_WINK -> {
                drawCircle(color = eyeGlowColor, radius = w * 0.052f, center = Offset(leftEyeX, eyeY))
                drawArc(
                    color = eyeGlowColor,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(rightEyeX - w * 0.055f, eyeY - h * 0.03f),
                    size = Size(w * 0.11f, h * 0.07f),
                    style = Stroke(width = w * 0.036f, cap = StrokeCap.Round)
                )
            }
            BobEyeStyle.CHILL_OPTICS -> {
                drawRoundRect(
                    color = eyeGlowColor,
                    topLeft = Offset(leftEyeX - w * 0.055f, eyeY - h * 0.025f),
                    size = Size(w * 0.11f, h * 0.05f),
                    cornerRadius = CornerRadius(w * 0.025f, w * 0.025f)
                )
                drawRoundRect(
                    color = eyeGlowColor,
                    topLeft = Offset(rightEyeX - w * 0.055f, eyeY - h * 0.025f),
                    size = Size(w * 0.11f, h * 0.05f),
                    cornerRadius = CornerRadius(w * 0.025f, w * 0.025f)
                )
            }
        }

        // Small smile / pulse bar under visor
        drawLine(
            color = accent.copy(alpha = 0.85f),
            start = Offset(cx - visorW * 0.18f, visorTop + visorH + h * 0.065f),
            end = Offset(cx + visorW * 0.18f, visorTop + visorH + h * 0.065f),
            strokeWidth = w * 0.032f,
            cap = StrokeCap.Round
        )

        // Side / Overlay Accessories
        when (appearance.accessory) {
            BobAccessory.GAMING_HEADSET -> {
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(bodyLeft - w * 0.04f, bodyTop - h * 0.08f),
                    size = Size(bodyW + w * 0.08f, bodyH * 0.72f),
                    style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
                )
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(bodyLeft - w * 0.08f, bodyTop + bodyH * 0.22f),
                    size = Size(w * 0.08f, h * 0.18f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(bodyLeft + bodyW, bodyTop + bodyH * 0.22f),
                    size = Size(w * 0.08f, h * 0.18f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
            }
            BobAccessory.TECH_GLASSES -> {
                val glassSizeW = visorW * 0.36f
                val glassSizeH = visorH * 0.62f
                val glassTop = eyeY - glassSizeH / 2f
                drawRoundRect(
                    color = Color(0xFFFBBF24),
                    topLeft = Offset(leftEyeX - glassSizeW / 2f, glassTop),
                    size = Size(glassSizeW, glassSizeH),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = Stroke(width = w * 0.025f)
                )
                drawRoundRect(
                    color = Color(0xFFFBBF24),
                    topLeft = Offset(rightEyeX - glassSizeW / 2f, glassTop),
                    size = Size(glassSizeW, glassSizeH),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = Stroke(width = w * 0.025f)
                )
                drawLine(
                    color = Color(0xFFFBBF24),
                    start = Offset(leftEyeX + glassSizeW / 2f, eyeY),
                    end = Offset(rightEyeX - glassSizeW / 2f, eyeY),
                    strokeWidth = w * 0.025f
                )
            }
            else -> Unit
        }
    }
}
