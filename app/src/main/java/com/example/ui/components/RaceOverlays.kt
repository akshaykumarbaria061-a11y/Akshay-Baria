package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GameMode
import com.example.data.model.RaceState
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherType
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroOrange
import com.example.ui.theme.RacingCard
import com.example.ui.theme.RacingDarkBg
import com.example.ui.theme.RacingRed

@Composable
fun RaceStatusOverlay(
    raceState: RaceState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(RacingDarkBg.copy(alpha = 0.82f), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (raceState.mode == GameMode.CIRCUIT_RACE) {
            // Position & Lap
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position badge
                Text(
                    text = "${raceState.playerPosition} / ${raceState.totalRacers}",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "LAP ${raceState.currentLap} / ${raceState.totalLaps}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (raceState.mode == GameMode.POLICE_CHASE) {
            // Police Heat meter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalPolice,
                    contentDescription = "Police",
                    tint = RacingRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "HEAT LVL ${raceState.heatLevel}",
                    color = RacingRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Timer
        val totalSecs = (raceState.raceTimeMs / 1000)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        val millis = (raceState.raceTimeMs % 1000) / 10
        val timeString = String.format("%02d:%02d.%02d", mins, secs, millis)

        Text(
            text = "TIME: $timeString",
            color = Color(0xFFE2E8F0),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Near-Miss Combo Counter
        if (raceState.nearMissCount > 0) {
            Text(
                text = "NEAR MISSES: ${raceState.nearMissCount}",
                color = NitroOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CountdownBanner(
    countdownValue: Int,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        val label = if (countdownValue > 0) countdownValue.toString() else "GO!"
        val color = if (countdownValue > 0) NitroOrange else NeonCyan

        Box(
            modifier = Modifier
                .background(RacingDarkBg.copy(alpha = 0.88f), RoundedCornerShape(24.dp))
                .border(2.dp, color, RoundedCornerShape(24.dp))
                .padding(horizontal = 40.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = color,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun WeatherSelectionDialog(
    currentWeather: WeatherType,
    currentTime: TimeOfDay,
    onSelectWeather: (WeatherType) -> Unit,
    onSelectTime: (TimeOfDay) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("weather_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = RacingCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ENVIRONMENT SETTINGS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "WEATHER CONDITION",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WeatherType.values().forEach { w ->
                        val selected = w == currentWeather
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) NeonCyan else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectWeather(w) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = w.name.take(4),
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "TIME OF DAY",
                    color = NitroOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeOfDay.values().forEach { t ->
                        val selected = t == currentTime
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) NitroOrange else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectTime(t) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t.name.take(5),
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("close_weather_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("APPLY", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PauseDialog(
    isAudioMuted: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleAudio: () -> Unit,
    onExitMenu: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("pause_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = RacingCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME PAUSED",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().testTag("resume_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME DRIVE", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().testTag("restart_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onToggleAudio,
                    modifier = Modifier.fillMaxWidth().testTag("toggle_audio_btn")
                ) {
                    Icon(
                        if (isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = NitroOrange
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isAudioMuted) "UNMUTE SOUND" else "MUTE SOUND",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onExitMenu,
                    modifier = Modifier.fillMaxWidth().testTag("exit_menu_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RacingRed)
                ) {
                    Text("EXIT TO MAIN MENU", color = RacingRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
