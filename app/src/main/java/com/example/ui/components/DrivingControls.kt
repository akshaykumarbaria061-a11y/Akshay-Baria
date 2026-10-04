package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroOrange
import com.example.ui.theme.RacingDarkBg
import com.example.ui.theme.RacingRed

@Composable
fun DrivingControls(
    onSteerLeft: (Boolean) -> Unit,
    onSteerRight: (Boolean) -> Unit,
    onThrottle: (Boolean) -> Unit,
    onBrake: (Boolean) -> Unit,
    onHandbrake: (Boolean) -> Unit,
    onNitro: (Boolean) -> Unit,
    onToggleCamera: () -> Unit,
    onToggleHeadlights: () -> Unit,
    onOpenWeatherDialog: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        // TOP BAR CONTROLS
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(RacingDarkBg.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera toggle
            IconButton(
                onClick = onToggleCamera,
                modifier = Modifier.size(42.dp).testTag("cam_toggle_btn")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera Mode", tint = NeonCyan)
            }

            // Headlights toggle
            IconButton(
                onClick = onToggleHeadlights,
                modifier = Modifier.size(42.dp).testTag("lights_toggle_btn")
            ) {
                Icon(Icons.Default.Highlight, contentDescription = "Headlights", tint = Color(0xFFFDE047))
            }

            // Weather & Time toggle
            IconButton(
                onClick = onOpenWeatherDialog,
                modifier = Modifier.size(42.dp).testTag("weather_dialog_btn")
            ) {
                Icon(Icons.Default.WbSunny, contentDescription = "Weather & Time", tint = NitroOrange)
            }

            // Pause
            IconButton(
                onClick = onPause,
                modifier = Modifier.size(42.dp).testTag("pause_btn")
            ) {
                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
            }
        }

        // BOTTOM LEFT: STEERING CONTROLS
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Arrow Button
            ControlTouchButton(
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Steer Left",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                },
                size = 72.dp,
                onStateChanged = onSteerLeft,
                tag = "steer_left_btn"
            )

            // Right Arrow Button
            ControlTouchButton(
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Steer Right",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                },
                size = 72.dp,
                onStateChanged = onSteerRight,
                tag = "steer_right_btn"
            )
        }

        // BOTTOM RIGHT: ACCELERATOR, BRAKE, HANDBRAKE, NITRO
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Handbrake Drift button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ControlTouchButton(
                    icon = {
                        Text("DRIFT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    },
                    size = 56.dp,
                    bgBrush = Brush.linearGradient(listOf(RacingRed, Color(0xFF99001A))),
                    onStateChanged = onHandbrake,
                    tag = "handbrake_btn"
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Nitro Boost Button
                ControlTouchButton(
                    icon = {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = "Nitro",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    size = 62.dp,
                    bgBrush = Brush.linearGradient(listOf(NitroOrange, Color(0xFFFF3D00))),
                    onStateChanged = onNitro,
                    tag = "nitro_btn"
                )
            }

            // Brake / Reverse Pedal
            ControlTouchPedal(
                label = "BRAKE",
                width = 62.dp,
                height = 100.dp,
                color = Color(0xFFE11D48),
                onStateChanged = onBrake,
                tag = "brake_pedal"
            )

            // Gas / Accelerator Pedal
            ControlTouchPedal(
                label = "GAS",
                width = 66.dp,
                height = 120.dp,
                color = Color(0xFF10B981),
                onStateChanged = onThrottle,
                tag = "gas_pedal"
            )
        }
    }
}

@Composable
fun ControlTouchButton(
    icon: @Composable () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    onStateChanged: (Boolean) -> Unit,
    bgBrush: Brush = Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
    tag: String
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .testTag(tag)
            .background(
                if (isPressed) Color(0xFF475569) else Color.Transparent,
                CircleShape
            )
            .background(bgBrush, CircleShape)
            .border(2.dp, if (isPressed) NeonCyan else Color(0xFF475569), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStateChanged(true)
                        tryAwaitRelease()
                        isPressed = false
                        onStateChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun ControlTouchPedal(
    label: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    onStateChanged: (Boolean) -> Unit,
    tag: String
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .testTag(tag)
            .background(
                if (isPressed) color else color.copy(alpha = 0.55f),
                RoundedCornerShape(12.dp)
            )
            .border(
                2.dp,
                if (isPressed) Color.White else color,
                RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStateChanged(true)
                        tryAwaitRelease()
                        isPressed = false
                        onStateChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
    }
}
