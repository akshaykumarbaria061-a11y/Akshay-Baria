package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BumperKit
import com.example.data.model.CarCustomization
import com.example.data.model.CarPaintFinish
import com.example.data.model.CarSpecs
import com.example.data.model.ExhaustTip
import com.example.data.model.RimStyle
import com.example.data.model.SpoilerStyle
import com.example.engine.math.Mat4
import com.example.engine.math.Vec3
import com.example.engine.renderer.Camera3D
import com.example.engine.renderer.Mesh3D
import com.example.ui.theme.MetalGray
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroGold
import com.example.ui.theme.NitroOrange
import com.example.ui.theme.RacingCard
import com.example.ui.theme.RacingDarkBg
import com.example.ui.theme.RacingRed
import com.example.ui.theme.SpeedGreen

@Composable
fun GarageScreen(
    currentCarId: String,
    playerCoins: Int,
    customizations: Map<String, CarCustomization>,
    onSelectCar: (String) -> Unit,
    onPurchaseCar: (CarSpecs) -> Unit,
    onUpdateCustomization: (CarCustomization) -> Unit,
    onPurchaseUpgrade: (CarCustomization, String, Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedCarId by remember { mutableStateOf(currentCarId) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var turntableAngle by remember { mutableFloatStateOf(0.4f) }

    val currentSpec = CarSpecs.find(selectedCarId)
    val currentCust = customizations[selectedCarId] ?: CarCustomization(selectedCarId, currentSpec.defaultColorHex)

    val isCurrentCarUnlocked = (selectedCarId == "apex_gt" || customizations[selectedCarId] != null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RacingDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("garage_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "APEX GARAGE & TUNING",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentSpec.name.uppercase(),
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Coins chip
            Box(
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .border(1.dp, NitroGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = NitroGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "$playerCoins CR",
                        color = NitroGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 3D SHOWROOM TURNTABLE CANVAS (Touch drag to spin car 360°)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF090D16)),
                        radius = 450f
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        turntableAngle += dragAmount.x * 0.012f
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w * 0.5f, h * 0.78f)

                // Showroom Circular Turntable floor pad
                drawOval(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(center.x - w * 0.42f, center.y - 25f),
                    size = androidx.compose.ui.geometry.Size(w * 0.84f, 50f)
                )
                drawOval(
                    color = NeonCyan.copy(alpha = 0.35f),
                    topLeft = Offset(center.x - w * 0.40f, center.y - 23f),
                    size = androidx.compose.ui.geometry.Size(w * 0.80f, 46f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
                )

                // Render 3D Car model rotated on turntable
                val camera = Camera3D().apply {
                    orbitAngle = turntableAngle
                    orbitPitch = 0.32f
                    orbitDistance = 5.2f
                    updateShowroomOrbit(Vec3(0f, 0f, 0f))
                }

                val viewProj = camera.getProjectionMatrix(w / h) * camera.getViewMatrix()
                val carMesh = Mesh3D.createCarMesh(currentSpec, currentCust, isBraking = false, isNitroActive = false)
                val modelMat = Mat4.rotationY(turntableAngle)
                val transMesh = carMesh.transform(modelMat)

                val polyPath = androidx.compose.ui.graphics.Path()

                for (face in transMesh.faces) {
                    val c0 = viewProj.transformPoint(face.v0)
                    val c1 = viewProj.transformPoint(face.v1)
                    val c2 = viewProj.transformPoint(face.v2)
                    val c3 = face.v3?.let { viewProj.transformPoint(it) }

                    if (c0.z < 0.05f || c1.z < 0.05f || c2.z < 0.05f) continue

                    val s0 = Offset((c0.x + 1f) * 0.5f * w, (1f - c0.y) * 0.5f * h)
                    val s1 = Offset((c1.x + 1f) * 0.5f * w, (1f - c1.y) * 0.5f * h)
                    val s2 = Offset((c2.x + 1f) * 0.5f * w, (1f - c2.y) * 0.5f * h)

                    val lightFactor = (face.normal.dot(Vec3(0.4f, 0.9f, 0.4f).normalized())).coerceIn(0.3f, 1.0f)
                    val faceColor = if (face.isEmissive) face.color else Color(
                        (face.color.red * lightFactor).coerceIn(0f, 1f),
                        (face.color.green * lightFactor).coerceIn(0f, 1f),
                        (face.color.blue * lightFactor).coerceIn(0f, 1f),
                        face.color.alpha
                    )

                    polyPath.reset()
                    polyPath.moveTo(s0.x, s0.y)
                    polyPath.lineTo(s1.x, s1.y)
                    polyPath.lineTo(s2.x, s2.y)
                    if (c3 != null) {
                        val s3 = Offset((c3.x + 1f) * 0.5f * w, (1f - c3.y) * 0.5f * h)
                        polyPath.lineTo(s3.x, s3.y)
                    }
                    polyPath.close()
                    drawPath(polyPath, faceColor)
                }
            }

            // Drag indicator text
            Text(
                text = "< DRAG TO ROTATE 360° >",
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
            )
        }

        // CAR SELECTOR ROW
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CarSpecs.ALL_CARS) { spec ->
                val isSelected = spec.id == selectedCarId
                val isUnlocked = (spec.id == "apex_gt" || spec.price == 0 || customizations[spec.id] != null)

                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0xFF131926),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) NeonCyan else Color(0xFF2B3854),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedCarId = spec.id }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isUnlocked) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = RacingRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = spec.name,
                            color = if (isSelected) NeonCyan else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Action: Select or Purchase current car
        if (selectedCarId != currentCarId) {
            val isUnlocked = (currentSpec.price == 0 || customizations[selectedCarId] != null)
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                if (isUnlocked) {
                    Button(
                        onClick = { onSelectCar(selectedCarId) },
                        modifier = Modifier.fillMaxWidth().testTag("select_car_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SELECT AS ACTIVE CAR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onPurchaseCar(currentSpec) },
                        enabled = playerCoins >= currentSpec.price,
                        modifier = Modifier.fillMaxWidth().testTag("unlock_car_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NitroGold)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("UNLOCK FOR ${currentSpec.price} CR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CUSTOMIZATION & PERFORMANCE TABS
        val tabs = listOf("PERFORMANCE", "PAINT", "WHEELS", "AERO WINGS", "EXHAUST")
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF0F172A),
            contentColor = NeonCyan,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Bold
                        )
                    }
                )
            }
        }

        // TAB CONTENTS
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // PERFORMANCE UPGRADES
                    PerformanceUpgradeItem(
                        name = "Engine Tuning",
                        stage = currentCust.engineStage,
                        maxStage = 5,
                        cost = currentCust.engineStage * 3500,
                        statDesc = "+${(currentCust.engineStage - 1) * 12} KM/H Top Speed",
                        onUpgrade = { onPurchaseUpgrade(currentCust, "engine", currentCust.engineStage * 3500) },
                        canAfford = playerCoins >= currentCust.engineStage * 3500
                    )
                    PerformanceUpgradeItem(
                        name = "Turbocharger / Supercharger",
                        stage = currentCust.brakeStage,
                        maxStage = 5,
                        cost = currentCust.brakeStage * 3000,
                        statDesc = "+${(currentCust.brakeStage - 1) * 6}% Acceleration Boost",
                        onUpgrade = { onPurchaseUpgrade(currentCust, "brake", currentCust.brakeStage * 3000) },
                        canAfford = playerCoins >= currentCust.brakeStage * 3000
                    )
                    PerformanceUpgradeItem(
                        name = "Racing Suspension & Drift Grip",
                        stage = currentCust.suspensionStage,
                        maxStage = 5,
                        cost = currentCust.suspensionStage * 2800,
                        statDesc = "+${(currentCust.suspensionStage - 1) * 6}% Cornering Stability",
                        onUpgrade = { onPurchaseUpgrade(currentCust, "suspension", currentCust.suspensionStage * 2800) },
                        canAfford = playerCoins >= currentCust.suspensionStage * 2800
                    )
                    PerformanceUpgradeItem(
                        name = "Twin Bottle Nitro System",
                        stage = currentCust.nitroStage,
                        maxStage = 5,
                        cost = currentCust.nitroStage * 3200,
                        statDesc = "+${(currentCust.nitroStage - 1) * 20} Nitro PSI Capacity",
                        onUpgrade = { onPurchaseUpgrade(currentCust, "nitro", currentCust.nitroStage * 3200) },
                        canAfford = playerCoins >= currentCust.nitroStage * 3200
                    )
                }
                1 -> {
                    // PAINT COLORS
                    Text("Select Body Paint Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val colors = listOf(
                        "#FF1E40", "#FFD700", "#00F0FF", "#8B5CF6", "#10B981", "#F97316",
                        "#0F172A", "#FFFFFF", "#EC4899", "#3B82F6", "#475569", "#84CC16"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colors.take(6).forEach { hex ->
                            ColorCircle(hex, isSelected = currentCust.colorHex == hex) {
                                onUpdateCustomization(currentCust.copy(colorHex = hex))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colors.drop(6).forEach { hex ->
                            ColorCircle(hex, isSelected = currentCust.colorHex == hex) {
                                onUpdateCustomization(currentCust.copy(colorHex = hex))
                            }
                        }
                    }
                }
                2 -> {
                    // RIMS & WHEELS
                    Text("Rim Style Selection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    RimStyle.values().forEach { rim ->
                        val isSelected = currentCust.rimStyle == rim
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateCustomization(currentCust.copy(rimStyle = rim)) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else RacingCard
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rim.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // SPOILER WINGS
                    Text("Aerodynamic Rear Wing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    SpoilerStyle.values().forEach { spoiler ->
                        val isSelected = currentCust.spoilerStyle == spoiler
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateCustomization(currentCust.copy(spoilerStyle = spoiler)) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else RacingCard
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(spoiler.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // EXHAUST
                    Text("Exhaust System Style", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    ExhaustTip.values().forEach { exhaust ->
                        val isSelected = currentCust.exhaustTip == exhaust
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateCustomization(currentCust.copy(exhaustTip = exhaust)) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else RacingCard
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(exhaust.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White)
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(color, CircleShape)
            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) NeonCyan else Color.Gray, CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
fun PerformanceUpgradeItem(
    name: String,
    stage: Int,
    maxStage: Int,
    cost: Int,
    statDesc: String,
    onUpgrade: () -> Unit,
    canAfford: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RacingCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B3854))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("STAGE $stage / $maxStage", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(statDesc, color = MetalGray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(10.dp))

            if (stage < maxStage) {
                Button(
                    onClick = onUpgrade,
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NitroGold)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Text("UPGRADE FOR $cost CR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MAX STAGE REACHED", color = SpeedGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
