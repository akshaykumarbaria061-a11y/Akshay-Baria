package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrafficType
import com.example.engine.math.Vec3
import com.example.engine.physics.CarPhysics
import com.example.engine.world.RoadZone
import com.example.engine.world.TrafficSystem
import com.example.engine.world.WorldMap
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RacingDarkBg
import com.example.ui.theme.RacingRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MinimapHUD(
    car: CarPhysics,
    worldMap: WorldMap,
    trafficSystem: TrafficSystem,
    currentZone: RoadZone,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Road Zone Badge
        Box(
            modifier = Modifier
                .background(RacingDarkBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = currentZone.title.uppercase(),
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Radar Circle
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(RacingDarkBg.copy(alpha = 0.85f), CircleShape)
                .border(2.dp, Color(0xFF334155), CircleShape)
        ) {
            Canvas(modifier = Modifier.size(110.dp)) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val radarRangeMeters = 320f
                val scale = (size.width * 0.45f) / radarRangeMeters

                // Concentric radar grid rings
                drawCircle(Color(0xFF1E293B), radius = size.width * 0.45f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                drawCircle(Color(0xFF1E293B), radius = size.width * 0.25f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))

                // Render nearby road points
                val playerPos = car.position
                for (i in 0 until worldMap.roadPoints.size step 4) {
                    val pt = worldMap.roadPoints[i]
                    val dx = pt.position.x - playerPos.x
                    val dz = pt.position.z - playerPos.z

                    // Rotate relative to player heading
                    val rx = dx * cos(-car.headingRad) - dz * sin(-car.headingRad)
                    val rz = dx * sin(-car.headingRad) + dz * cos(-car.headingRad)

                    if (rx * rx + rz * rz < radarRangeMeters * radarRangeMeters) {
                        val sx = center.x + rx * scale
                        val sy = center.y - rz * scale
                        drawCircle(Color(0xFF64748B), radius = 2f, center = Offset(sx, sy))
                    }
                }

                // Render Checkpoints
                for (cp in worldMap.checkpoints) {
                    val dx = cp.x - playerPos.x
                    val dz = cp.z - playerPos.z
                    val rx = dx * cos(-car.headingRad) - dz * sin(-car.headingRad)
                    val rz = dx * sin(-car.headingRad) + dz * cos(-car.headingRad)
                    if (rx * rx + rz * rz < radarRangeMeters * radarRangeMeters) {
                        val sx = center.x + rx * scale
                        val sy = center.y - rz * scale
                        drawCircle(Color(0xFFFACC15), radius = 4f, center = Offset(sx, sy))
                    }
                }

                // Render Traffic Dots & Police
                for (v in trafficSystem.vehicles) {
                    val dx = v.position.x - playerPos.x
                    val dz = v.position.z - playerPos.z
                    val rx = dx * cos(-car.headingRad) - dz * sin(-car.headingRad)
                    val rz = dx * sin(-car.headingRad) + dz * cos(-car.headingRad)

                    if (rx * rx + rz * rz < radarRangeMeters * radarRangeMeters) {
                        val sx = center.x + rx * scale
                        val sy = center.y - rz * scale
                        val dotColor = if (v.type == TrafficType.POLICE) RacingRed else Color(0xFF38BDF8)
                        drawCircle(dotColor, radius = 3.5f, center = Offset(sx, sy))
                    }
                }

                // Player Arrow at center
                val arrowPath = Path().apply {
                    moveTo(center.x, center.y - 7f)
                    lineTo(center.x + 5f, center.y + 6f)
                    lineTo(center.x, center.y + 3f)
                    lineTo(center.x - 5f, center.y + 6f)
                    close()
                }
                drawPath(arrowPath, NeonCyan)
            }
        }
    }
}
