package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

enum class ScanPhase {
    DETECTING,      // Blue/cyan scanning
    LIVENESS_CHECK, // Amber asking for smile/blink
    MATCHING,       // Purple/indigo processing
    SUCCESS,        // Emerald green verified
    FAILED          // Red retry
}

@Composable
fun FaceScannerOverlay(
    scanPhase: ScanPhase,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_transition")
    
    // Laser animation
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Pulse animation for brackets
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracket_pulse"
    )

    val activeColor = when (scanPhase) {
        ScanPhase.DETECTING -> Color(0xFF38BDF8)
        ScanPhase.LIVENESS_CHECK -> Color(0xFFF59E0B)
        ScanPhase.MATCHING -> Color(0xFF818CF8)
        ScanPhase.SUCCESS -> Color(0xFF22C55E)
        ScanPhase.FAILED -> Color(0xFFEF4444)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Allows transparent blend mode
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Dimensions for face oval cutout
            val ovalWidth = canvasWidth * 0.72f
            val ovalHeight = canvasHeight * 0.48f
            val ovalLeft = (canvasWidth - ovalWidth) / 2f
            val ovalTop = (canvasHeight - ovalHeight) / 2.3f

            val faceRect = Rect(ovalLeft, ovalTop, ovalLeft + ovalWidth, ovalTop + ovalHeight)

            // Dark semi-transparent scrim around face
            drawRect(
                color = Color(0x990A0F1D),
                size = size
            )

            // Cut out the face window
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = faceRect,
                        cornerRadius = CornerRadius(ovalWidth * 0.45f, ovalHeight * 0.48f)
                    )
                )
            }
            drawPath(
                path = path,
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )

            // Draw glowing border around face cutout
            drawPath(
                path = path,
                color = activeColor.copy(alpha = 0.8f),
                style = Stroke(width = 4f)
            )

            // Draw corner guides
            val cornerLen = 36f * pulseScale
            val strokeW = 6f
            val cap = StrokeCap.Round

            // Top-left corner
            drawLine(activeColor, Offset(ovalLeft - 10, ovalTop + cornerLen), Offset(ovalLeft - 10, ovalTop - 10), strokeW, cap)
            drawLine(activeColor, Offset(ovalLeft - 10, ovalTop - 10), Offset(ovalLeft + cornerLen, ovalTop - 10), strokeW, cap)

            // Top-right corner
            drawLine(activeColor, Offset(ovalLeft + ovalWidth + 10, ovalTop + cornerLen), Offset(ovalLeft + ovalWidth + 10, ovalTop - 10), strokeW, cap)
            drawLine(activeColor, Offset(ovalLeft + ovalWidth + 10, ovalTop - 10), Offset(ovalLeft + ovalWidth - cornerLen, ovalTop - 10), strokeW, cap)

            // Bottom-left corner
            drawLine(activeColor, Offset(ovalLeft - 10, ovalTop + ovalHeight - cornerLen), Offset(ovalLeft - 10, ovalTop + ovalHeight + 10), strokeW, cap)
            drawLine(activeColor, Offset(ovalLeft - 10, ovalTop + ovalHeight + 10), Offset(ovalLeft + cornerLen, ovalTop + ovalHeight + 10), strokeW, cap)

            // Bottom-right corner
            drawLine(activeColor, Offset(ovalLeft + ovalWidth + 10, ovalTop + ovalHeight - cornerLen), Offset(ovalLeft + ovalWidth + 10, ovalTop + ovalHeight + 10), strokeW, cap)
            drawLine(activeColor, Offset(ovalLeft + ovalWidth + 10, ovalTop + ovalHeight + 10), Offset(ovalLeft + ovalWidth - cornerLen, ovalTop + ovalHeight + 10), strokeW, cap)

            // Draw scanning laser line if detecting or matching
            if (scanPhase == ScanPhase.DETECTING || scanPhase == ScanPhase.MATCHING || scanPhase == ScanPhase.LIVENESS_CHECK) {
                val laserY = ovalTop + (ovalHeight * laserYRatio)
                val laserWidth = ovalWidth * 0.9f
                val laserLeft = (canvasWidth - laserWidth) / 2f

                // Laser glow gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            activeColor.copy(alpha = 0.35f),
                            activeColor.copy(alpha = 0.75f),
                            activeColor.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = laserY - 14f,
                        endY = laserY + 14f
                    ),
                    topLeft = Offset(laserLeft, laserY - 14f),
                    size = Size(laserWidth, 28f)
                )

                // Laser core line
                drawLine(
                    color = activeColor,
                    start = Offset(laserLeft, laserY),
                    end = Offset(laserLeft + laserWidth, laserY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            // Facial feature simulated landmarks dots
            if (scanPhase != ScanPhase.SUCCESS) {
                val centerX = canvasWidth / 2f
                val centerY = ovalTop + (ovalHeight * 0.45f)

                // Left Eye
                drawCircle(activeColor.copy(alpha = 0.6f), radius = 5f, center = Offset(centerX - 42f, centerY - 25f))
                // Right Eye
                drawCircle(activeColor.copy(alpha = 0.6f), radius = 5f, center = Offset(centerX + 42f, centerY - 25f))
                // Nose Tip
                drawCircle(activeColor.copy(alpha = 0.7f), radius = 4f, center = Offset(centerX, centerY + 15f))
                // Mouth arc indicator
                drawArc(
                    color = activeColor.copy(alpha = 0.6f),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - 30f, centerY + 35f),
                    size = Size(60f, 30f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
