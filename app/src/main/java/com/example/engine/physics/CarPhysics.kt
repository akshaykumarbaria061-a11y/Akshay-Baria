package com.example.engine.physics

import com.example.data.model.CarCustomization
import com.example.data.model.CarSpecs
import com.example.engine.math.Vec3
import com.example.engine.world.WorldMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Realistic vehicle physics simulation covering engine power, gear ratios,
 * lateral slip/drift dynamics, suspension pitch/roll, aerodynamic downforce,
 * nitro boost, and collision bounding.
 */
class CarPhysics(
    val spec: CarSpecs,
    var customization: CarCustomization
) {
    // Spatial state
    var position: Vec3 = Vec3(0f, 0.5f, 0f)
    var forward: Vec3 = Vec3(0f, 0f, 1f)
    var right: Vec3 = Vec3(1f, 0f, 0f)
    var up: Vec3 = Vec3(0f, 1f, 0f)

    var velocity: Vec3 = Vec3.ZERO
    var speedMs: Float = 0f // meters per second
    val speedKmh: Float get() = speedMs * 3.6f

    var headingRad: Float = 0f
    var steeringAngleRad: Float = 0f

    // Suspension body orientation (visual pitch & roll)
    var suspensionPitch: Float = 0f // Nose dive/squat
    var suspensionRoll: Float = 0f  // Lateral body lean
    var wheelSpinAngle: Float = 0f

    // Powertrain & Gearbox
    var currentGear: Int = 1 // -1 = Reverse, 0 = Neutral, 1..6 = Forward
    var rpm: Float = 1000f
    val maxRpm: Float = 8500f
    val idleRpm: Float = 1000f

    // Gear ratios for 6-speed sport transmission
    private val gearRatios = floatArrayOf(3.8f, 2.3f, 1.6f, 1.2f, 0.95f, 0.78f)
    private val finalDriveRatio = 3.4f
    private val reverseGearRatio = 3.5f

    // Nitro / Boost state
    var nitroFuel: Float = 100f
    val maxNitroFuel: Float get() = spec.baseNitroCapacity + (customization.nitroStage - 1) * 20f
    var isNitroActive: Boolean = false

    // Drift & Slip state
    var lateralSlip: Float = 0f // 0 = full grip, 1 = heavy drift slide
    var isHandbraking: Boolean = false

    // Vehicle Health & Damage (0 to 100)
    var health: Float = 100f
    var isDestroyed: Boolean = false

    // Effective specs calculated from upgrades
    private val topSpeedKmh: Float get() = customization.calculateEffectiveTopSpeed(spec.baseTopSpeedKmh)
    private val accelPower: Float get() = customization.calculateEffectiveAccel(spec.baseAcceleration)
    private val handlingGrip: Float get() = customization.calculateEffectiveHandling(spec.baseHandling)
    private val brakePower: Float get() = customization.calculateEffectiveBraking(spec.baseBraking)

    fun resetAt(pos: Vec3, heading: Float) {
        position = pos
        headingRad = heading
        velocity = Vec3.ZERO
        speedMs = 0f
        steeringAngleRad = 0f
        suspensionPitch = 0f
        suspensionRoll = 0f
        currentGear = 1
        rpm = idleRpm
        nitroFuel = maxNitroFuel
        health = 100f
        isDestroyed = false
        updateVectors()
    }

    private fun updateVectors() {
        forward = Vec3(sin(headingRad), 0f, cos(headingRad)).normalized()
        right = Vec3(cos(headingRad), 0f, -sin(headingRad)).normalized()
        up = Vec3.UP
    }

    fun step(
        dt: Float,
        throttleInput: Float,   // 0.0 to 1.0
        brakeInput: Float,      // 0.0 to 1.0
        steerInput: Float,      // -1.0 (left) to 1.0 (right)
        handbrakeInput: Boolean,
        nitroInput: Boolean,
        worldMap: WorldMap
    ) {
        if (isDestroyed) {
            velocity = velocity * 0.94f
            speedMs = velocity.length()
            position = position + velocity * dt
            return
        }

        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // 1. Steering simulation (speed-sensitive steering lock)
        val speedFactor = (1f - (speedKmh / 350f).coerceIn(0f, 0.75f))
        val targetSteer = steerInput * (0.60f * speedFactor)
        steeringAngleRad = steeringAngleRad + (targetSteer - steeringAngleRad) * (12f * clampedDt)

        // 2. Nitro Boost logic
        isNitroActive = nitroInput && nitroFuel > 2f && throttleInput > 0.2f
        if (isNitroActive) {
            nitroFuel = (nitroFuel - 22f * clampedDt).coerceAtLeast(0f)
        } else {
            // Passive recharge
            nitroFuel = (nitroFuel + 3.5f * clampedDt).coerceAtMost(maxNitroFuel)
        }

        // 3. Acceleration & Powertrain calculation
        val nitroMultiplier = if (isNitroActive) 1.65f else 1.0f
        var driveForce = 0f

        val effectiveTopSpeedMs = (topSpeedKmh * (if (isNitroActive) 1.15f else 1.0f)) / 3.6f

        if (throttleInput > 0f) {
            if (currentGear == -1) {
                // Reverse gear
                driveForce = -throttleInput * 3500f * accelPower
            } else {
                // Forward gear
                val speedRatio = (speedMs / effectiveTopSpeedMs).coerceIn(0f, 1f)
                val torqueCurve = 1.0f - (speedRatio * speedRatio * 0.75f)
                driveForce = throttleInput * (6500f * accelPower * nitroMultiplier) * torqueCurve
            }
        }

        // 4. Braking and Reverse detection
        var brakingForce = 0f
        if (brakeInput > 0f) {
            if (speedMs > 1.0f) {
                // Normal brake
                brakingForce = brakeInput * 12000f * brakePower
            } else if (throttleInput == 0f) {
                // Shift to reverse when stopped and holding brake
                currentGear = -1
                driveForce = -brakeInput * 3000f * accelPower
            }
        } else if (currentGear == -1 && throttleInput > 0f) {
            currentGear = 1
        }

        // 5. Handbrake / E-brake
        isHandbraking = handbrakeInput
        if (isHandbraking) {
            brakingForce += 8000f
            // Drifting rewards slight nitro recharge
            if (speedKmh > 30f) {
                nitroFuel = (nitroFuel + 8f * clampedDt).coerceAtMost(maxNitroFuel)
            }
        }

        // 6. Longitudinal Physics (Drive force vs Friction vs Aerodynamic Drag)
        val carMass = 1350f
        val airDragCoeff = 0.38f
        val rollingResistance = 180f

        val dragForce = 0.5f * airDragCoeff * 2.1f * (speedMs * speedMs)
        val netLongitudinalForce = driveForce - (brakingForce + rollingResistance + dragForce) * (if (speedMs >= 0f) 1f else -1f)

        val longitudinalAccel = netLongitudinalForce / carMass
        speedMs = (speedMs + longitudinalAccel * clampedDt).coerceIn(-18f, effectiveTopSpeedMs)

        // 7. Lateral Dynamics, Drift & Slip Angle
        val steerResponsiveness = handlingGrip * (if (isHandbraking) 1.55f else 1.0f)
        val angularVelocity = (speedMs / 2.7f) * sin(steeringAngleRad) * steerResponsiveness
        headingRad += angularVelocity * clampedDt

        updateVectors()

        // Calculate lateral slip
        val lateralG = (angularVelocity * speedMs) / 9.81f
        lateralSlip = if (isHandbraking) {
            (abs(lateralG) * 0.6f + 0.45f).coerceIn(0.4f, 1.0f)
        } else {
            (abs(lateralG) - 0.7f).coerceIn(0f, 1.0f)
        }

        // Update velocity vector
        val forwardVel = forward * speedMs
        val driftSidewaysVel = right * (if (steeringAngleRad > 0) -1f else 1f) * (speedMs * lateralSlip * 0.35f)
        velocity = forwardVel + driftSidewaysVel

        // Move position
        position = position + velocity * clampedDt

        // 8. Ground Conformation (match road height & banking)
        val closestRoad = worldMap.getClosestRoadPoint(position)
        val targetY = closestRoad.position.y + 0.35f
        position = Vec3(position.x, position.y + (targetY - position.y) * (14f * clampedDt), position.z)

        // 9. Automatic Gear Shifting & RPM calculation
        if (currentGear > 0) {
            val ratio = gearRatios[currentGear - 1] * finalDriveRatio
            val wheelRpm = (speedMs / (0.34f * 2f * PI.toFloat())) * 60f
            rpm = (wheelRpm * ratio * 0.4f + idleRpm + throttleInput * 1500f).coerceIn(idleRpm, maxRpm)

            // Shift up
            if (rpm > 7200f && currentGear < 6) {
                currentGear++
                rpm *= 0.68f // Rev drop on upshift
            }
            // Shift down
            if (rpm < 2600f && currentGear > 1) {
                currentGear--
                rpm *= 1.35f
            }
        } else {
            rpm = (idleRpm + abs(speedMs) * 180f + (if (brakeInput > 0) brakeInput * 2000f else 0f)).coerceIn(idleRpm, maxRpm)
        }

        // 10. Suspension Visual Pitch & Roll
        val targetPitch = (longitudinalAccel / 18f).coerceIn(-0.15f, 0.15f) // Brake dive & squat
        val targetRoll = (-angularVelocity * 0.08f).coerceIn(-0.12f, 0.12f) // Cornering body roll
        suspensionPitch += (targetPitch - suspensionPitch) * (10f * clampedDt)
        suspensionRoll += (targetRoll - suspensionRoll) * (10f * clampedDt)

        // Wheel rotation
        wheelSpinAngle += (speedMs / 0.34f) * clampedDt

        // 11. Road boundary check & barrier collision
        val distToCenterline = (position - closestRoad.position).length()
        val roadHalfWidth = closestRoad.width * 0.5f + 1.2f
        if (distToCenterline > roadHalfWidth) {
            // Hit shoulder/guardrail
            speedMs *= 0.88f
            // Push back slightly towards road
            val correctionDir = (closestRoad.position - position).normalized()
            position = position + correctionDir * 0.2f
            takeDamage(0.6f * clampedDt * (speedKmh / 50f))
        }
    }

    fun takeDamage(amount: Float) {
        health = (health - amount).coerceAtLeast(0f)
        if (health <= 0f) {
            isDestroyed = true
        }
    }

    fun repair(amount: Float = 100f) {
        health = (health + amount).coerceAtMost(100f)
        isDestroyed = false
    }

    fun applyCollisionImpulse(impulse: Vec3, damageAmount: Float) {
        velocity = velocity + impulse
        speedMs = velocity.length()
        takeDamage(damageAmount)
    }
}
