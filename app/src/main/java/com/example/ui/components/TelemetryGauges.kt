package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ArcMetricGauge(
    progress: Float,
    valueText: String,
    labelText: String,
    accentColor: Color,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    gaugeSize: Dp = 128.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "arc_gauge_progress"
    )

    Box(
        modifier = modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.11f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.7f),
                        accentColor,
                        accentColor.copy(alpha = 0.9f)
                    )
                ),
                startAngle = 140f,
                sweepAngle = 260f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FpsCadenceSparkline(
    samples: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val fillBrush = Brush.verticalGradient(
        listOf(lineColor.copy(alpha = 0.32f), lineColor.copy(alpha = 0.02f))
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        if (samples.size < 2) {
            drawLine(
                color = lineColor.copy(alpha = 0.5f),
                start = Offset(0f, size.height * 0.5f),
                end = Offset(size.width, size.height * 0.5f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val maxVal = (samples.maxOrNull() ?: 120f).coerceAtLeast(60f)
        val minVal = 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val stepX = size.width / (samples.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()

        samples.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = ((value - minVal) / range).coerceIn(0.05f, 0.95f)
            val y = size.height - (normalizedY * size.height)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(path = fillPath, brush = fillBrush)
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
