package com.example.engine.world

import androidx.compose.ui.graphics.Color
import com.example.data.model.TrafficLightState
import com.example.engine.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class WorldMap {

    val roadPoints: ArrayList<RoadPoint> = ArrayList()
    val props: ArrayList<WorldProp> = ArrayList()
    val trafficSignals: ArrayList<TrafficSignalNode> = ArrayList()
    val checkpoints: ArrayList<Vec3> = ArrayList()

    var totalCircuitLength: Float = 0f

    init {
        buildWorld()
    }

    private fun buildWorld() {
        roadPoints.clear()
        props.clear()
        trafficSignals.clear()
        checkpoints.clear()

        // We construct a massive continuous open-world loop of 1080 points (approx 16 km total road)
        val numSegments = 1080
        val segmentDist = 16f // meters per segment

        var currPos = Vec3(0f, 0f, 0f)
        var currAngle = 0f // heading in radians
        var currElevation = 0f
        var accumDist = 0f

        for (i in 0 until numSegments) {
            val progress = i.toFloat() / numSegments

            // Determine which of the 10 zones we are currently in
            val zone = when (i) {
                in 0..120 -> RoadZone.CITY
                in 121..240 -> RoadZone.HIGHWAY
                in 241..360 -> RoadZone.MOUNTAIN_HAIRPIN
                in 361..460 -> RoadZone.SNOW_SUMMIT
                in 461..540 -> RoadZone.MOUNTAIN_TUNNEL
                in 541..640 -> RoadZone.VILLAGE
                in 641..740 -> RoadZone.FOREST
                in 741..860 -> RoadZone.DESERT
                in 861..960 -> RoadZone.SUSPENSION_BRIDGE
                else -> RoadZone.COASTAL
            }

            // Determine curvature & elevation based on zone
            var turnRate = 0f
            var elevationRate = 0f
            var roadWidth = 14f

            when (zone) {
                RoadZone.CITY -> {
                    roadWidth = 18f
                    // 90-degree city grid turns with straightaways
                    turnRate = when (i % 60) {
                        in 45..54 -> (PI.toFloat() / 2f) / 10f
                        else -> 0f
                    }
                    elevationRate = 0f
                }
                RoadZone.HIGHWAY -> {
                    roadWidth = 22f
                    // Fast sweeping wide highway bends
                    turnRate = sin(i * 0.05f) * 0.015f
                    elevationRate = 0.02f
                }
                RoadZone.MOUNTAIN_HAIRPIN -> {
                    roadWidth = 11f
                    // Severe switchbacks and steep climb
                    turnRate = sin(i * 0.12f) * 0.07f
                    elevationRate = 0.65f
                }
                RoadZone.SNOW_SUMMIT -> {
                    roadWidth = 12f
                    // Mountain top plateau with undulating corners
                    turnRate = cos(i * 0.08f) * 0.035f
                    elevationRate = sin(i * 0.06f) * 0.2f
                }
                RoadZone.MOUNTAIN_TUNNEL -> {
                    roadWidth = 13f
                    // Gentle descent cutting straight through the mountain
                    turnRate = 0.005f
                    elevationRate = -0.55f
                }
                RoadZone.VILLAGE -> {
                    roadWidth = 12f
                    // Quaint rolling curves through village
                    turnRate = sin(i * 0.07f) * 0.03f
                    elevationRate = -0.1f
                }
                RoadZone.FOREST -> {
                    roadWidth = 13f
                    // Serpentine forest path
                    turnRate = cos(i * 0.09f) * 0.045f
                    elevationRate = 0.05f
                }
                RoadZone.DESERT -> {
                    roadWidth = 16f
                    // Long open desert straights and wide canyon turns
                    turnRate = sin(i * 0.04f) * 0.02f
                    elevationRate = -0.05f
                }
                RoadZone.SUSPENSION_BRIDGE -> {
                    roadWidth = 20f
                    // Perfectly straight majestic ocean crossing at steady height
                    turnRate = 0.002f
                    elevationRate = 0f
                }
                RoadZone.COASTAL -> {
                    roadWidth = 15f
                    // Sweeping coastal loop returning back to city
                    turnRate = 0.032f + sin(i * 0.06f) * 0.015f
                    elevationRate = -0.08f
                }
            }

            currAngle += turnRate
            currElevation += elevationRate

            // Road direction vectors
            val forward = Vec3(sin(currAngle), 0f, cos(currAngle)).normalized()
            val right = Vec3(cos(currAngle), 0f, -sin(currAngle)).normalized()
            val normal = Vec3.UP

            val roadPt = RoadPoint(
                position = Vec3(currPos.x, currElevation.coerceAtLeast(0f), currPos.z),
                forward = forward,
                normal = normal,
                right = right,
                width = roadWidth,
                zone = zone,
                roadIndex = i,
                distanceAlongRoad = accumDist,
                hasTrafficLight = (zone == RoadZone.CITY && i % 40 == 0) || (zone == RoadZone.VILLAGE && i == 580)
            )

            roadPoints.add(roadPt)

            // Add Traffic Light if designated
            if (roadPt.hasTrafficLight) {
                trafficSignals.add(
                    TrafficSignalNode(
                        position = roadPt.position + right * (roadWidth * 0.55f) + Vec3(0f, 4.5f, 0f),
                        roadIndex = i,
                        state = if (trafficSignals.size % 2 == 0) TrafficLightState.GREEN else TrafficLightState.RED,
                        timerSeconds = (i % 15).toFloat()
                    )
                )
            }

            // Place Checkpoints every 90 segments for Circuit & Time Trial
            if (i % 90 == 0) {
                checkpoints.add(roadPt.position + Vec3(0f, 1.5f, 0f))
            }

            // Generate detailed 3D environment props along both road shoulders
            spawnPropsForSegment(roadPt, i, zone)

            currPos += forward * segmentDist
            accumDist += segmentDist
        }

        totalCircuitLength = accumDist
    }

    private fun spawnPropsForSegment(pt: RoadPoint, index: Int, zone: RoadZone) {
        val halfW = pt.width * 0.5f

        // Street Lights & Guardrails
        if (index % 6 == 0) {
            if (zone == RoadZone.CITY || zone == RoadZone.HIGHWAY || zone == RoadZone.SUSPENSION_BRIDGE) {
                props.add(
                    WorldProp(
                        type = PropType.STREET_LIGHT,
                        position = pt.position + pt.right * (halfW + 1.8f),
                        rotationY = 0f,
                        color = Color(0xFFFFD54F)
                    )
                )
            }
            if (zone == RoadZone.MOUNTAIN_HAIRPIN || zone == RoadZone.SNOW_SUMMIT || zone == RoadZone.COASTAL) {
                props.add(
                    WorldProp(
                        type = PropType.GUARD_RAIL,
                        position = pt.position - pt.right * (halfW + 0.8f),
                        rotationY = 0f,
                        scale = Vec3(pt.width, 0.8f, 15f),
                        color = Color(0xFFD1D5DB)
                    )
                )
            }
        }

        // Zone-specific 3D architectural & environmental elements
        when (zone) {
            RoadZone.CITY -> {
                if (index % 4 == 0) {
                    val isLeft = (index % 8 == 0)
                    val offsetRight = if (isLeft) -(halfW + 14f) else (halfW + 14f)
                    val buildingType = if (index % 12 == 0) PropType.BUILDING_SHOP else PropType.BUILDING_SKYSCRAPER
                    val bHeight = 25f + (index % 7) * 9f
                    val bColor = when (index % 5) {
                        0 -> Color(0xFF1E293B)
                        1 -> Color(0xFF0F172A)
                        2 -> Color(0xFF334155)
                        3 -> Color(0xFF1E3A8A)
                        else -> Color(0xFF0D9488)
                    }
                    props.add(
                        WorldProp(
                            type = buildingType,
                            position = pt.position + pt.right * offsetRight,
                            rotationY = 0f,
                            scale = Vec3(18f, bHeight, 18f),
                            color = bColor,
                            textLabel = if (buildingType == PropType.BUILDING_SHOP) "APEX MOTORS" else "SKYLINE TOWER"
                        )
                    )
                }
            }
            RoadZone.HIGHWAY -> {
                if (index % 25 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.BILLBOARD,
                            position = pt.position + pt.right * (halfW + 6f) + Vec3(0f, 6f, 0f),
                            rotationY = 0f,
                            scale = Vec3(10f, 5f, 1f),
                            color = Color(0xFF0284C7),
                            textLabel = "SPEEDWAY ARENA ->"
                        )
                    )
                }
                if (index % 50 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.GAS_STATION,
                            position = pt.position - pt.right * (halfW + 16f),
                            rotationY = 0f,
                            scale = Vec3(22f, 7f, 16f),
                            color = Color(0xFFEF4444),
                            textLabel = "OCTANE 99 NITRO FUEL"
                        )
                    )
                }
            }
            RoadZone.MOUNTAIN_HAIRPIN -> {
                if (index % 3 == 0) {
                    val offsetSide = if (index % 2 == 0) (halfW + 5f) else -(halfW + 6f)
                    props.add(
                        WorldProp(
                            type = PropType.ROCK_LARGE,
                            position = pt.position + pt.right * offsetSide,
                            rotationY = (index * 45).toFloat(),
                            scale = Vec3(7f + (index % 4) * 3f, 12f + (index % 3) * 6f, 7f),
                            color = Color(0xFF4B463E)
                        )
                    )
                }
            }
            RoadZone.SNOW_SUMMIT -> {
                if (index % 5 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.TREE_PINE,
                            position = pt.position + pt.right * (halfW + 4f + (index % 5)),
                            rotationY = 0f,
                            scale = Vec3(3.5f, 9f, 3.5f),
                            color = Color(0xFFE2E8F0) // Snow covered
                        )
                    )
                }
            }
            RoadZone.MOUNTAIN_TUNNEL -> {
                // Repeating tunnel portal arch rings every 4 segments
                if (index % 3 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.TUNNEL_ARCH,
                            position = pt.position + Vec3(0f, 3f, 0f),
                            rotationY = 0f,
                            scale = Vec3(pt.width + 2f, 6.5f, 4f),
                            color = Color(0xFF1E2124)
                        )
                    )
                }
            }
            RoadZone.VILLAGE -> {
                if (index % 8 == 0) {
                    val isRight = index % 16 == 0
                    val offset = if (isRight) (halfW + 10f) else -(halfW + 10f)
                    props.add(
                        WorldProp(
                            type = PropType.HOUSE,
                            position = pt.position + pt.right * offset,
                            rotationY = 0f,
                            scale = Vec3(9f, 6f, 9f),
                            color = if (isRight) Color(0xFFB45309) else Color(0xFF991B1B)
                        )
                    )
                }
            }
            RoadZone.FOREST -> {
                if (index % 3 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.TREE_PINE,
                            position = pt.position + pt.right * (halfW + 3f + (index % 4) * 2f),
                            rotationY = 0f,
                            scale = Vec3(3.5f, 10f, 3.5f),
                            color = Color(0xFF166534)
                        )
                    )
                    props.add(
                        WorldProp(
                            type = PropType.TREE_PINE,
                            position = pt.position - pt.right * (halfW + 3f + (index % 5) * 2f),
                            rotationY = 0f,
                            scale = Vec3(4f, 11f, 4f),
                            color = Color(0xFF14532D)
                        )
                    )
                }
            }
            RoadZone.DESERT -> {
                if (index % 7 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.CACTUS,
                            position = pt.position + pt.right * (halfW + 6f + (index % 6)),
                            rotationY = 0f,
                            scale = Vec3(1.5f, 5.5f, 1.5f),
                            color = Color(0xFF4D7C0F)
                        )
                    )
                }
                if (index % 18 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.ROCK_LARGE,
                            position = pt.position - pt.right * (halfW + 14f),
                            rotationY = 0f,
                            scale = Vec3(14f, 18f, 14f),
                            color = Color(0xFF9A3412) // Red sandstone mesa
                        )
                    )
                }
            }
            RoadZone.SUSPENSION_BRIDGE -> {
                // Towering suspension bridge pylons every 24 segments
                if (index % 24 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.BRIDGE_PYLON,
                            position = pt.position + pt.right * (halfW + 1f),
                            rotationY = 0f,
                            scale = Vec3(3f, 48f, 3f),
                            color = Color(0xFFDC2626) // International Bridge Red
                        )
                    )
                    props.add(
                        WorldProp(
                            type = PropType.BRIDGE_PYLON,
                            position = pt.position - pt.right * (halfW + 1f),
                            rotationY = 0f,
                            scale = Vec3(3f, 48f, 3f),
                            color = Color(0xFFDC2626)
                        )
                    )
                }
            }
            RoadZone.COASTAL -> {
                if (index % 6 == 0) {
                    props.add(
                        WorldProp(
                            type = PropType.TREE_PALM,
                            position = pt.position + pt.right * (halfW + 5f),
                            rotationY = 0f,
                            scale = Vec3(2.5f, 7f, 2.5f),
                            color = Color(0xFF15803D)
                        )
                    )
                }
            }
        }
    }

    /**
     * Finds the closest road point to any given 3D position in the world.
     */
    fun getClosestRoadPoint(pos: Vec3): RoadPoint {
        var closest = roadPoints[0]
        var minDistSq = Float.MAX_VALUE
        for (pt in roadPoints) {
            val distSq = (pos.x - pt.position.x) * (pos.x - pt.position.x) +
                    (pos.z - pt.position.z) * (pos.z - pt.position.z)
            if (distSq < minDistSq) {
                minDistSq = distSq
                closest = pt
            }
        }
        return closest
    }

    fun getRoadPoint(index: Int): RoadPoint {
        val safeIndex = (index % roadPoints.size + roadPoints.size) % roadPoints.size
        return roadPoints[safeIndex]
    }

    fun updateTrafficSignals(dtSeconds: Float) {
        for (signal in trafficSignals) {
            signal.timerSeconds += dtSeconds
            // 8 sec Green, 2.5 sec Yellow, 7 sec Red
            val cycleTime = signal.timerSeconds % 17.5f
            signal.state = when {
                cycleTime < 8.0f -> TrafficLightState.GREEN
                cycleTime < 10.5f -> TrafficLightState.YELLOW
                else -> TrafficLightState.RED
            }
        }
    }
}
