package com.example.engine.renderer

import com.example.data.model.CameraMode
import com.example.engine.math.Mat4
import com.example.engine.math.Vec3
import com.example.engine.physics.CarPhysics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Camera3D {

    var mode: CameraMode = CameraMode.THIRD_PERSON

    var eye: Vec3 = Vec3(0f, 4f, -7f)
    var target: Vec3 = Vec3.ZERO
    var up: Vec3 = Vec3.UP

    var fovRad: Float = (62f * PI.toFloat() / 180f)
    var targetFovDeg: Float = 62f

    // Turntable orbit state for Garage Showroom
    var orbitAngle: Float = 0.5f
    var orbitPitch: Float = 0.25f
    var orbitDistance: Float = 5.2f

    fun updateGameCamera(dt: Float, car: CarPhysics) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Dynamic FOV widening with speed and nitro
        val speedRatio = (car.speedKmh / 350f).coerceIn(0f, 1f)
        val nitroBoost = if (car.isNitroActive) 14f else 0f
        targetFovDeg = 62f + speedRatio * 16f + nitroBoost

        val currentFovDeg = fovRad * 180f / PI.toFloat()
        val smoothedFovDeg = currentFovDeg + (targetFovDeg - currentFovDeg) * (8f * clampedDt)
        fovRad = smoothedFovDeg * PI.toFloat() / 180f

        when (mode) {
            CameraMode.THIRD_PERSON -> {
                // Chase camera positioned behind car
                val behindDist = 5.8f + speedRatio * 1.5f
                val height = 2.4f - car.suspensionPitch * 2.0f
                val desiredEye = car.position - car.forward * behindDist + Vec3(0f, height, 0f)

                // Spring lag smoothing
                eye = Vec3.lerp(eye, desiredEye, 12f * clampedDt)
                val lookAheadDist = 8f + speedRatio * 12f
                val desiredTarget = car.position + car.forward * lookAheadDist + Vec3(0f, 0.8f, 0f)
                target = Vec3.lerp(target, desiredTarget, 16f * clampedDt)
                up = Vec3.UP
            }
            CameraMode.FIRST_PERSON_COCKPIT -> {
                // Cockpit interior eye position behind steering wheel & windshield
                val cockpitOffset = car.forward * 0.1f + car.up * 0.95f - car.right * 0.32f
                eye = car.position + cockpitOffset
                val lookAheadDist = 25f
                target = car.position + car.forward * lookAheadDist + Vec3(0f, 0.6f, 0f)
                up = car.up
            }
            CameraMode.HOOD_BUMPER -> {
                // Front bumper low angle
                val bumperOffset = car.forward * 1.8f + car.up * 0.45f
                eye = car.position + bumperOffset
                target = car.position + car.forward * 30f + Vec3(0f, 0.3f, 0f)
                up = car.up
            }
        }
    }

    fun updateShowroomOrbit(carPos: Vec3) {
        val yOffset = 0.6f
        val radPitch = orbitPitch.coerceIn(0.05f, 1.2f)
        val camY = carPos.y + yOffset + sin(radPitch) * orbitDistance
        val groundDist = cos(radPitch) * orbitDistance
        val camX = carPos.x + sin(orbitAngle) * groundDist
        val camZ = carPos.z + cos(orbitAngle) * groundDist

        eye = Vec3(camX, camY, camZ)
        target = carPos + Vec3(0f, yOffset, 0f)
        up = Vec3.UP
        fovRad = 48f * PI.toFloat() / 180f // Cinematic portrait FOV
    }

    fun getViewMatrix(): Mat4 {
        return Mat4.lookAt(eye, target, up)
    }

    fun getProjectionMatrix(aspectRatio: Float): Mat4 {
        return Mat4.perspective(fovRad, aspectRatio, 0.3f, 600f)
    }
}
