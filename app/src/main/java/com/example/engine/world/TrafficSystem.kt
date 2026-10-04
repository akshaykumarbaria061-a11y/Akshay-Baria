package com.example.engine.world

import com.example.data.model.AiRacer
import com.example.data.model.GameMode
import com.example.data.model.TrafficLightState
import com.example.data.model.TrafficType
import com.example.data.model.TrafficVehicle
import com.example.engine.math.Vec3
import com.example.engine.physics.CarPhysics
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class TrafficSystem(private val worldMap: WorldMap) {

    val vehicles: ArrayList<TrafficVehicle> = ArrayList()
    val aiRacers: ArrayList<AiRacer> = ArrayList()

    var nearMissCount: Int = 0
    var lastNearMissTime: Long = 0L

    fun initTraffic(mode: GameMode) {
        vehicles.clear()
        aiRacers.clear()
        nearMissCount = 0

        val trafficColors = listOf("#E11D48", "#2563EB", "#D97706", "#059669", "#7C3AED", "#475569", "#F8FAFC", "#0F172A")
        val numVehicles = if (mode == GameMode.TRAFFIC_DRIVE) 26 else 16

        // Distribute traffic vehicles around the open world road circuit
        for (i in 0 until numVehicles) {
            val roadIdx = (i * (worldMap.roadPoints.size / numVehicles) + 20) % worldMap.roadPoints.size
            val pt = worldMap.getRoadPoint(roadIdx)
            val laneOffset = if (i % 2 == 0) 3.5f else -3.5f

            val type = when {
                mode == GameMode.POLICE_CHASE && i % 3 == 0 -> TrafficType.POLICE
                i % 8 == 0 -> TrafficType.BUS
                i % 7 == 0 -> TrafficType.TRUCK
                i % 6 == 0 -> TrafficType.MOTORCYCLE
                i % 5 == 0 -> TrafficType.SUV
                else -> TrafficType.SEDAN
            }

            val targetSpeed = when (type) {
                TrafficType.BUS, TrafficType.TRUCK -> 65f
                TrafficType.MOTORCYCLE -> 110f
                TrafficType.POLICE -> 130f
                else -> 85f
            }

            val pos = pt.position + pt.right * laneOffset + Vec3(0f, 0.4f, 0f)
            val heading = atan2(pt.forward.x, pt.forward.z)

            vehicles.add(
                TrafficVehicle(
                    id = i,
                    type = type,
                    position = pos,
                    velocity = pt.forward * (targetSpeed / 3.6f),
                    headingRad = heading,
                    speedKmh = targetSpeed,
                    targetSpeedKmh = targetSpeed,
                    colorHex = if (type == TrafficType.POLICE) "#1E293B" else trafficColors[i % trafficColors.size],
                    sirenActive = type == TrafficType.POLICE,
                    laneIndex = if (i % 2 == 0) 1 else 0
                )
            )
        }

        // Initialize AI Racers for Circuit Race mode
        if (mode == GameMode.CIRCUIT_RACE) {
            val startPt = worldMap.getRoadPoint(0)
            val racerNames = listOf("Apex Venom", "Blaze GT", "Shadow Mach")
            val racerCars = listOf("veloce_v12", "thunder_muscle", "apex_gt")
            val racerColors = listOf("#FACC15", "#DC2626", "#8B5CF6")

            for (r in 0..2) {
                val gridOffset = (r + 1) * -12f
                val laneOffset = if (r % 2 == 0) 3.0f else -3.0f
                val rPt = worldMap.getRoadPoint(0)
                aiRacers.add(
                    AiRacer(
                        id = r,
                        name = racerNames[r],
                        carSpecId = racerCars[r],
                        colorHex = racerColors[r],
                        position = rPt.position + rPt.forward * gridOffset + rPt.right * laneOffset + Vec3(0f, 0.4f, 0f),
                        headingRad = atan2(rPt.forward.x, rPt.forward.z),
                        speedKmh = 0f,
                        currentCheckpoint = 0,
                        lap = 1
                    )
                )
            }
        }
    }

    fun update(
        dt: Float,
        playerPhysics: CarPhysics,
        mode: GameMode,
        onNearMiss: (Int) -> Unit,
        onCollision: (TrafficVehicle) -> Unit
    ) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // 1. Update AI Traffic Vehicles
        for (v in vehicles) {
            val closestRoad = worldMap.getClosestRoadPoint(v.position)
            val roadForward = closestRoad.forward
            val roadRight = closestRoad.right

            // Check upcoming traffic signal within 35 meters
            var shouldStopAtLight = false
            for (signal in worldMap.trafficSignals) {
                val distToLight = (signal.position - v.position).length()
                if (distToLight < 32f && (signal.state == TrafficLightState.RED || signal.state == TrafficLightState.YELLOW)) {
                    val dotDir = roadForward.dot((signal.position - v.position).normalized())
                    if (dotDir > 0.4f) {
                        shouldStopAtLight = true
                        break
                    }
                }
            }

            // In Police Chase mode, police vehicle tracks player
            if (v.type == TrafficType.POLICE && mode == GameMode.POLICE_CHASE) {
                v.sirenActive = true
                val dirToPlayer = (playerPhysics.position - v.position)
                val distToPlayer = dirToPlayer.length()

                if (distToPlayer < 120f) {
                    // Chase player aggressively
                    val targetHeading = atan2(dirToPlayer.x, dirToPlayer.z)
                    v.headingRad += (targetHeading - v.headingRad) * (4f * clampedDt)
                    v.targetSpeedKmh = 145f
                } else {
                    val roadHeading = atan2(roadForward.x, roadForward.z)
                    v.headingRad += (roadHeading - v.headingRad) * (3f * clampedDt)
                }
            } else {
                // Normal lane following
                val roadHeading = atan2(roadForward.x, roadForward.z)
                v.headingRad += (roadHeading - v.headingRad) * (4f * clampedDt)
            }

            // Speed adjustment
            if (shouldStopAtLight) {
                v.speedKmh = (v.speedKmh - 60f * clampedDt).coerceAtLeast(0f)
                v.isBraking = true
            } else {
                v.speedKmh = v.speedKmh + (v.targetSpeedKmh - v.speedKmh) * (2f * clampedDt)
                v.isBraking = false
            }

            val forwardVec = Vec3(sin(v.headingRad), 0f, cos(v.headingRad)).normalized()
            v.velocity = forwardVec * (v.speedKmh / 3.6f)
            v.position = v.position + v.velocity * clampedDt

            // Maintain elevation on road
            val targetY = closestRoad.position.y + 0.35f
            v.position = Vec3(v.position.x, targetY, v.position.z)

            // Collision check with player
            val distToPlayer = (v.position - playerPhysics.position).length()
            val collisionDist = (v.type.length * 0.45f + 2.0f)

            if (distToPlayer < collisionDist && !v.isDestroyed) {
                // Impact!
                val impactDir = (playerPhysics.position - v.position).normalized()
                val relativeSpeed = (playerPhysics.speedKmh - v.speedKmh).coerceAtLeast(20f)
                val damage = (relativeSpeed * 0.35f).coerceIn(8f, 35f)

                playerPhysics.applyCollisionImpulse(impactDir * (relativeSpeed * 0.12f), damage)
                v.speedKmh *= 0.4f
                onCollision(v)
            } else if (distToPlayer < 4.2f && playerPhysics.speedKmh > 65f) {
                // Near miss check!
                val now = System.currentTimeMillis()
                if (now - lastNearMissTime > 1200L) {
                    lastNearMissTime = now
                    nearMissCount++
                    onNearMiss(nearMissCount)
                }
            }
        }

        // 2. Update AI Racers in Circuit Race
        if (mode == GameMode.CIRCUIT_RACE) {
            for (racer in aiRacers) {
                val nextCpIdx = (racer.currentCheckpoint + 1) % worldMap.checkpoints.size
                val targetCp = worldMap.checkpoints[nextCpIdx]

                val dirToCp = (targetCp - racer.position)
                val distToCp = dirToCp.length()

                if (distToCp < 18f) {
                    racer.currentCheckpoint = nextCpIdx
                    if (nextCpIdx == 0) {
                        racer.lap++
                    }
                }

                val targetHeading = atan2(dirToCp.x, dirToCp.z)
                racer.headingRad += (targetHeading - racer.headingRad) * (5f * clampedDt)

                // Racer speed variance
                val maxSpeed = 190f + racer.id * 15f
                racer.speedKmh = (racer.speedKmh + 28f * clampedDt).coerceAtMost(maxSpeed)

                val forwardVec = Vec3(sin(racer.headingRad), 0f, cos(racer.headingRad)).normalized()
                racer.position = racer.position + forwardVec * (racer.speedKmh / 3.6f) * clampedDt

                val closestRoad = worldMap.getClosestRoadPoint(racer.position)
                racer.position = Vec3(racer.position.x, closestRoad.position.y + 0.35f, racer.position.z)
                racer.progressDistance += (racer.speedKmh / 3.6f) * clampedDt
            }
        }
    }
}
