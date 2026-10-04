package com.example.engine.math

import kotlin.math.sqrt

data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSq(): Float = x * x + y * y + z * z

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 0.00001f) Vec3(x / len, y / len, z / len) else Vec3(0f, 1f, 0f)
    }

    fun distanceTo(other: Vec3): Float = (this - other).length()

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val FORWARD = Vec3(0f, 0f, 1f)
        val RIGHT = Vec3(1f, 0f, 0f)

        fun lerp(a: Vec3, b: Vec3, t: Float): Vec3 {
            val clampedT = t.coerceIn(0f, 1f)
            return Vec3(
                a.x + (b.x - a.x) * clampedT,
                a.y + (b.y - a.y) * clampedT,
                a.z + (b.z - a.z) * clampedT
            )
        }
    }
}
