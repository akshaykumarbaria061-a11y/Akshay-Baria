package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RaceState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroGold
import com.example.ui.theme.RacingCard
import com.example.ui.theme.RacingRed

@Composable
fun GameOverDialog(
    raceState: RaceState,
    isWrecked: Boolean,
    onRestart: () -> Unit,
    onOpenGarage: () -> Unit,
    onMainMenu: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("game_over_card"),
            colors = CardDefaults.cardColors(containerColor = RacingCard),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                if (isWrecked) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Wrecked",
                        tint = RacingRed,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "VEHICLE TOTALED",
                        color = RacingRed,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = NitroGold,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val title = if (raceState.playerPosition == 1) "VICTORY! 1ST PLACE" else "RACE FINISHED"
                    Text(
                        text = title,
                        color = NitroGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val totalSecs = (raceState.raceTimeMs / 1000)
                        val mins = totalSecs / 60
                        val secs = totalSecs % 60
                        val millis = (raceState.raceTimeMs % 1000) / 10

                        StatRow("Total Time", String.format("%02d:%02d.%02d", mins, secs, millis))
                        if (raceState.playerPosition > 0) {
                            StatRow("Final Position", "#${raceState.playerPosition} of ${raceState.totalRacers}")
                        }
                        if (raceState.nearMissCount > 0) {
                            StatRow("Near Misses", "${raceState.nearMissCount}")
                        }

                        // Reward coins
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reward", color = Color.White, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = NitroGold, modifier = Modifier.size(16.dp))
                                Text("+${raceState.earnedReward} CR", color = NitroGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().testTag("play_again_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PLAY AGAIN", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenGarage,
                    modifier = Modifier.fillMaxWidth().testTag("garage_from_game_over_btn")
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TUNE & UPGRADE IN GARAGE", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier.fillMaxWidth().testTag("menu_from_game_over_btn")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MAIN MENU", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
