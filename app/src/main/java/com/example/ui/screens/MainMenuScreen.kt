package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CarSpecs
import com.example.data.model.GameMode
import com.example.ui.theme.MetalGray
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroGold
import com.example.ui.theme.NitroOrange
import com.example.ui.theme.RacingCard
import com.example.ui.theme.RacingDarkBg
import com.example.ui.theme.RacingRed

@Composable
fun MainMenuScreen(
    playerCoins: Int,
    currentCarId: String,
    onStartGame: (GameMode) -> Unit,
    onOpenGarage: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val currentCar = CarSpecs.find(currentCarId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RacingDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "APEX DRIVE 3D",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "OPEN WORLD SIMULATOR",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            // Credits pill
            Box(
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(20.dp))
                    .border(1.dp, NitroGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = NitroGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "$playerCoins CR",
                        color = NitroGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // HERO CAR STATUS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RacingCard),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B3854))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE VEHICLE",
                        color = MetalGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentCar.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${currentCar.category} • Top Speed ${currentCar.baseTopSpeedKmh.toInt()} KM/H",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onOpenGarage,
                    modifier = Modifier.testTag("menu_garage_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GARAGE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // GAME MODES HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SELECT GAME MODE",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            // Records button
            Text(
                text = "LEADERBOARD",
                color = NitroOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = onOpenLeaderboard)
                    .testTag("menu_leaderboard_btn")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // GAME MODES LIST
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(GameMode.values()) { mode ->
                GameModeCard(
                    mode = mode,
                    onSelect = { onStartGame(mode) }
                )
            }
        }
    }
}

@Composable
fun GameModeCard(
    mode: GameMode,
    onSelect: () -> Unit
) {
    val (icon, accentColor, gradient) = when (mode) {
        GameMode.FREE_DRIVE -> Triple(
            Icons.Default.DirectionsCar,
            NeonCyan,
            listOf(Color(0xFF0F2B3E), Color(0xFF131926))
        )
        GameMode.TRAFFIC_DRIVE -> Triple(
            Icons.Default.Traffic,
            NitroOrange,
            listOf(Color(0xFF33200D), Color(0xFF131926))
        )
        GameMode.CIRCUIT_RACE -> Triple(
            Icons.Default.EmojiEvents,
            NitroGold,
            listOf(Color(0xFF332B0D), Color(0xFF131926))
        )
        GameMode.HILL_CLIMB -> Triple(
            Icons.Default.Landscape,
            Color(0xFF10B981),
            listOf(Color(0xFF0E3022), Color(0xFF131926))
        )
        GameMode.TIME_TRIAL -> Triple(
            Icons.Default.Timer,
            Color(0xFF8B5CF6),
            listOf(Color(0xFF23173F), Color(0xFF131926))
        )
        GameMode.POLICE_CHASE -> Triple(
            Icons.Default.LocalPolice,
            RacingRed,
            listOf(Color(0xFF3B0D19), Color(0xFF131926))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("mode_${mode.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RacingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.background(Brush.horizontalGradient(gradient))) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, accentColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mode.displayName.uppercase(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mode.description,
                        color = MetalGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
