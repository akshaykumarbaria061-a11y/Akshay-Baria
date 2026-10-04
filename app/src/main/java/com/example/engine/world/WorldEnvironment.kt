package com.example.engine.world

import androidx.compose.ui.graphics.Color
import com.example.data.model.TrafficLightState
import com.example.engine.math.Vec3

enum class RoadZone(
    val title: String,
    val surfaceColor: Color,
    val shoulderColor: Color,
    val lanes: Int,
    val speedLimitKmh: Float
) {
    CITY("Downtown Metropolis", Color(0xFF232730), Color(0xFF4A4E5A), 4, 80f),
    HIGHWAY("Apex Interstate Highway", Color(0xFF1E2129), Color(0xFF374151), 4, 180f),
    MOUNTAIN_HAIRPIN("Dragon Ridge Pass", Color(0xFF2C2F38), Color(0xFF5A4D41), 2, 90f),
    VILLAGE("Alpine Village", Color(0xFF383632), Color(0xFF2E5E35), 2, 60f),
    FOREST("Blackwood Forest", Color(0xFF252924), Color(0xFF1F4324), 2, 100f),
    DESERT("Mojave Dunes", Color(0xFF3E3932), Color(0xFFD4A373), 3, 140f),
    SUSPENSION_BRIDGE("Golden Bay Bridge", Color(0xFF2A2D34), Color(0xFF4B5563), 4, 130f),
    MOUNTAIN_TUNNEL("Titan Bore Tunnel", Color(0xFF1B1D22), Color(0xFF111827), 2, 110f),
    COASTAL("Pacific Coastline", Color(0xFF282B33), Color(0xFFE5D0A1), 2, 120f),
    SNOW_SUMMIT("Frost Peak Pass", Color(0xFF434A56), Color(0xFFE2E8F0), 2, 80f)
}

enum class PropType {
    BUILDING_SKYSCRAPER,
    BUILDING_SHOP,
    HOUSE,
    TREE_PINE,
    TREE_PALM,
    ROCK_LARGE,
    CACTUS,
    STREET_LIGHT,
    TRAFFIC_SIGNAL,
    BRIDGE_PYLON,
    TUNNEL_ARCH,
    GUARD_RAIL,
    BILLBOARD,
    GAS_STATION,
    PEDESTRIAN_CROSSING
}

data class WorldProp(
    val type: PropType,
    val position: Vec3,
    val rotationY: Float,
    val scale: Vec3 = Vec3(1f, 1f, 1f),
    val color: Color = Color.Gray,
    val textLabel: String = ""
)

data class TrafficSignalNode(
    val position: Vec3,
    val roadIndex: Int,
    var state: TrafficLightState = TrafficLightState.GREEN,
    var timerSeconds: Float = 0f
)

data class RoadPoint(
    val position: Vec3,
    val forward: Vec3,
    val normal: Vec3,
    val right: Vec3,
    val width: Float,
    val zone: RoadZone,
    val roadIndex: Int,
    val distanceAlongRoad: Float,
    val hasTrafficLight: Boolean = false
)
