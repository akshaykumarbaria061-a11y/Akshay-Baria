package com.example.engine.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 4x4 Transformation Matrix in Column-Major order for 3D graphics rendering.
 */
class Mat4(val m: FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f }) {

    operator fun times(other: Mat4): Mat4 {
        val result = FloatArray(16)
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += this.m[k * 4 + row] * other.m[col * 4 + k]
                }
                result[col * 4 + row] = sum
            }
        }
        return Mat4(result)
    }

    fun transformPoint(v: Vec3): Vec3 {
        val x = v.x * m[0] + v.y * m[4] + v.z * m[8] + m[12]
        val y = v.x * m[1] + v.y * m[5] + v.z * m[9] + m[13]
        val z = v.x * m[2] + v.y * m[6] + v.z * m[10] + m[14]
        val w = v.x * m[3] + v.y * m[7] + v.z * m[11] + m[15]
        return if (w != 0f) Vec3(x / w, y / w, z / w) else Vec3(x, y, z)
    }

    fun transformDirection(v: Vec3): Vec3 {
        val x = v.x * m[0] + v.y * m[4] + v.z * m[8]
        val y = v.x * m[1] + v.y * m[5] + v.z * m[9]
        val z = v.x * m[2] + v.y * m[6] + v.z * m[10]
        return Vec3(x, y, z)
    }

    companion object {
        fun identity(): Mat4 = Mat4()

        fun translation(x: Float, y: Float, z: Float): Mat4 {
            val res = identity()
            res.m[12] = x
            res.m[13] = y
            res.m[14] = z
            return res
        }

        fun scale(sx: Float, sy: Float, sz: Float): Mat4 {
            val res = FloatArray(16)
            res[0] = sx
            res[5] = sy
            res[10] = sz
            res[15] = 1f
            return Mat4(res)
        }

        fun rotationY(angleRad: Float): Mat4 {
            val c = cos(angleRad)
            val s = sin(angleRad)
            val res = identity()
            res.m[0] = c
            res.m[2] = -s
            res.m[8] = s
            res.m[10] = c
            return res
        }

        fun rotationX(angleRad: Float): Mat4 {
            val c = cos(angleRad)
            val s = sin(angleRad)
            val res = identity()
            res.m[5] = c
            res.m[6] = s
            res.m[9] = -s
            res.m[10] = c
            return res
        }

        fun rotationZ(angleRad: Float): Mat4 {
            val c = cos(angleRad)
            val s = sin(angleRad)
            val res = identity()
            res.m[0] = c
            res.m[1] = s
            res.m[4] = -s
            res.m[5] = c
            return res
        }

        fun lookAt(eye: Vec3, target: Vec3, up: Vec3): Mat4 {
            val f = (target - eye).normalized()
            val s = f.cross(up).normalized()
            val u = s.cross(f)

            val res = FloatArray(16)
            res[0] = s.x
            res[4] = s.y
            res[8] = s.z
            res[12] = -s.dot(eye)

            res[1] = u.x
            res[5] = u.y
            res[9] = u.z
            res[13] = -u.dot(eye)

            res[2] = -f.x
            res[6] = -f.y
            res[10] = -f.z
            res[14] = f.dot(eye)

            res[3] = 0f
            res[7] = 0f
            res[11] = 0f
            res[15] = 1f

            return Mat4(res)
        }

        fun perspective(fovYRad: Float, aspect: Float, near: Float, far: Float): Mat4 {
            val tanHalfFov = tan(fovYRad / 2f)
            val res = FloatArray(16)
            res[0] = 1f / (aspect * tanHalfFov)
            res[5] = 1f / tanHalfFov
            res[10] = -(far + near) / (far - near)
            res[11] = -1f
            res[14] = -(2f * far * near) / (far - near)
            res[15] = 0f
            return Mat4(res)
        }
    }
}
