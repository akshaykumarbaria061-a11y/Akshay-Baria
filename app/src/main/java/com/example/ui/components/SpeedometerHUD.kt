package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroOrange
import com.example.ui.theme.RacingDarkBg
import com.example.ui.theme.RacingRed
import com.example.ui.theme.SpeedGreen
import kotlin.math.roundToInt

@Composable
fun SpeedometerHUD(
    speedKmh: Float,
    rpm: Float,
    maxRpm: Float,
    currentGear: Int,
    nitroPercent: Float,
    healthPercent: Float,
    isNitroActive: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedRpm by animateFloatAsState(targetValue = rpm, label = "rpm")
    val animatedSpeed by animateFloatAsState(targetValue = speedKmh, label = "speed")

    val gearText = when (currentGear) {
        -1 -> "R"
        0 -> "N"
        else -> currentGear.toString()
    }

    Box(
        modifier = modifier
            .size(190.dp)
            .background(RacingDarkBg.copy(alpha = 0.82f), CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Tachometer Arc & Speedometer dial
        Canvas(modifier = Modifier.size(174.dp)) {
            val strokeW = 12f
            val startAngle = 140f
            val sweepTotal = 260f

            // Track background arc
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Dynamic RPM Arc (Cyan to Orange to Redline)
            val rpmRatio = (animatedRpm / maxRpm).coerceIn(0f, 1f)
            val activeSweep = sweepTotal * rpmRatio

            val arcColors = if (rpmRatio > 0.85f) {
                listOf(NeonCyan, NitroOrange, RacingRed)
            } else {
                listOf(NeonCyan, Color(0xFF00E5FF))
            }

            drawArc(
                brush = Brush.sweepGradient(arcColors),
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // Center Gauges
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Big Speed Display
            Text(
                text = "${animatedSpeed.roundToInt()}",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "KM/H",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Gear Indicator Badge
            Box(
                modifier = Modifier
                    .background(
                        if (currentGear == -1) RacingRed else Color(0xFF1E293B),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GEAR $gearText",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Nitro & Health bars row
            Row(
                modifier = Modifier.fillMaxWidth(0.72f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nitro bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Nitro",
                        tint = if (isNitroActive) NitroOrange else NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(6.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(nitroPercent.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(
                                    if (isNitroActive) NitroOrange else NeonCyan,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }

                // Health bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Health",
                        tint = if (healthPercent < 0.35f) RacingRed else SpeedGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(6.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(healthPercent.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(
                                    if (healthPercent < 0.35f) RacingRed else SpeedGreen,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
