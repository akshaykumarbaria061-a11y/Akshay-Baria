package com.example.engine.renderer

import androidx.compose.ui.graphics.Color
import com.example.data.model.CarCustomization
import com.example.data.model.CarSpecs
import com.example.data.model.RimStyle
import com.example.data.model.SpoilerStyle
import com.example.data.model.TrafficType
import com.example.engine.math.Mat4
import com.example.engine.math.Vec3

data class Face3D(
    val v0: Vec3,
    val v1: Vec3,
    val v2: Vec3,
    val v3: Vec3? = null, // null if triangle
    val color: Color,
    val isEmissive: Boolean = false,
    val normal: Vec3 = Vec3.UP
)

class Mesh3D(val faces: List<Face3D>) {

    fun transform(mat: Mat4): Mesh3D {
        val transformedFaces = faces.map { face ->
            val p0 = mat.transformPoint(face.v0)
            val p1 = mat.transformPoint(face.v1)
            val p2 = mat.transformPoint(face.v2)
            val p3 = face.v3?.let { mat.transformPoint(it) }
            val n = mat.transformDirection(face.normal).normalized()
            Face3D(p0, p1, p2, p3, face.color, face.isEmissive, n)
        }
        return Mesh3D(transformedFaces)
    }

    companion object {
        /**
         * Builds a detailed 3D Car model tailored to the car specs and customizations.
         */
        fun createCarMesh(
            spec: CarSpecs,
            customization: CarCustomization,
            isBraking: Boolean = false,
            isNitroActive: Boolean = false,
            steerAngle: Float = 0f
        ): Mesh3D {
            val faces = ArrayList<Face3D>()

            val bodyColor = runCatching { Color(android.graphics.Color.parseColor(customization.colorHex)) }
                .getOrDefault(Color(0xFFE11D48))
            val darkChassis = Color(0xFF111827)
            val glassColor = Color(0xCC1E293B)
            val headlightColor = Color(0xFFE0F7FA)
            val taillightColor = if (isBraking) Color(0xFFFF0033) else Color(0xFF99001A)
            val rimColor = runCatching { Color(android.graphics.Color.parseColor(customization.rimColorHex)) }
                .getOrDefault(Color(0xFFE2E8F0))

            val halfW = 0.95f
            val halfL = 2.1f
            val baseH = 0.25f
            val hoodH = 0.65f
            val roofH = 1.15f

            // 1. Lower chassis / underbody
            faces.add(
                Face3D(
                    Vec3(-halfW, baseH, -halfL),
                    Vec3(halfW, baseH, -halfL),
                    Vec3(halfW, baseH, halfL),
                    Vec3(-halfW, baseH, halfL),
                    darkChassis,
                    normal = -Vec3.UP
                )
            )

            // 2. Main lower body sides
            faces.add(Face3D(Vec3(-halfW, baseH, -halfL), Vec3(-halfW, hoodH, -halfL), Vec3(-halfW, hoodH, halfL), Vec3(-halfW, baseH, halfL), bodyColor, normal = -Vec3.RIGHT))
            faces.add(Face3D(Vec3(halfW, baseH, halfL), Vec3(halfW, hoodH, halfL), Vec3(halfW, hoodH, -halfL), Vec3(halfW, baseH, -halfL), bodyColor, normal = Vec3.RIGHT))

            // 3. Front Bumper & Grille
            faces.add(Face3D(Vec3(-halfW, baseH, halfL), Vec3(halfW, baseH, halfL), Vec3(halfW, hoodH, halfL), Vec3(-halfW, hoodH, halfL), bodyColor, normal = Vec3.FORWARD))
            faces.add(Face3D(Vec3(-halfW * 0.6f, baseH + 0.08f, halfL + 0.01f), Vec3(halfW * 0.6f, baseH + 0.08f, halfL + 0.01f), Vec3(halfW * 0.6f, hoodH * 0.7f, halfL + 0.01f), Vec3(-halfW * 0.6f, hoodH * 0.7f, halfL + 0.01f), Color(0xFF0F172A), normal = Vec3.FORWARD))

            // 4. Hood (sloping from cabin base to front bumper)
            faces.add(
                Face3D(
                    Vec3(-halfW * 0.92f, hoodH, halfL),
                    Vec3(halfW * 0.92f, hoodH, halfL),
                    Vec3(halfW * 0.85f, hoodH + 0.05f, 0.45f),
                    Vec3(-halfW * 0.85f, hoodH + 0.05f, 0.45f),
                    bodyColor,
                    normal = Vec3(0f, 0.85f, 0.2f).normalized()
                )
            )

            // 5. Cabin & Windshield
            // Front Windshield
            faces.add(
                Face3D(
                    Vec3(-halfW * 0.82f, hoodH + 0.05f, 0.45f),
                    Vec3(halfW * 0.82f, hoodH + 0.05f, 0.45f),
                    Vec3(halfW * 0.70f, roofH, -0.3f),
                    Vec3(-halfW * 0.70f, roofH, -0.3f),
                    glassColor,
                    normal = Vec3(0f, 0.65f, 0.75f).normalized()
                )
            )

            // Roof
            faces.add(
                Face3D(
                    Vec3(-halfW * 0.70f, roofH, -0.3f),
                    Vec3(halfW * 0.70f, roofH, -0.3f),
                    Vec3(halfW * 0.72f, roofH, -1.25f),
                    Vec3(-halfW * 0.72f, roofH, -1.25f),
                    bodyColor,
                    normal = Vec3.UP
                )
            )

            // Rear Window
            faces.add(
                Face3D(
                    Vec3(-halfW * 0.72f, roofH, -1.25f),
                    Vec3(halfW * 0.72f, roofH, -1.25f),
                    Vec3(halfW * 0.85f, hoodH + 0.02f, -1.75f),
                    Vec3(-halfW * 0.85f, hoodH + 0.02f, -1.75f),
                    glassColor,
                    normal = Vec3(0f, 0.6f, -0.8f).normalized()
                )
            )

            // Rear Trunk & Bumper
            faces.add(
                Face3D(
                    Vec3(-halfW, baseH, -halfL),
                    Vec3(-halfW, hoodH, -halfL),
                    Vec3(halfW, hoodH, -halfL),
                    Vec3(halfW, baseH, -halfL),
                    bodyColor,
                    normal = -Vec3.FORWARD
                )
            )

            // 6. Glowing Headlights (Front left & right)
            faces.add(
                Face3D(
                    Vec3(-halfW * 0.88f, hoodH - 0.15f, halfL + 0.02f),
                    Vec3(-halfW * 0.45f, hoodH - 0.15f, halfL + 0.02f),
                    Vec3(-halfW * 0.45f, hoodH - 0.02f, halfL + 0.02f),
                    Vec3(-halfW * 0.88f, hoodH - 0.02f, halfL + 0.02f),
                    headlightColor,
                    isEmissive = true,
                    normal = Vec3.FORWARD
                )
            )
            faces.add(
                Face3D(
                    Vec3(halfW * 0.45f, hoodH - 0.15f, halfL + 0.02f),
                    Vec3(halfW * 0.88f, hoodH - 0.15f, halfL + 0.02f),
                    Vec3(halfW * 0.88f, hoodH - 0.02f, halfL + 0.02f),
                    Vec3(halfW * 0.45f, hoodH - 0.02f, halfL + 0.02f),
                    headlightColor,
                    isEmissive = true,
                    normal = Vec3.FORWARD
                )
            )

            // 7. Glowing Taillights (Rear left & right)
            faces.add(
                Face3D(
                    Vec3(-halfW * 0.88f, hoodH - 0.18f, -halfL - 0.02f),
                    Vec3(-halfW * 0.88f, hoodH - 0.04f, -halfL - 0.02f),
                    Vec3(-halfW * 0.42f, hoodH - 0.04f, -halfL - 0.02f),
                    Vec3(-halfW * 0.42f, hoodH - 0.18f, -halfL - 0.02f),
                    taillightColor,
                    isEmissive = true,
                    normal = -Vec3.FORWARD
                )
            )
            faces.add(
                Face3D(
                    Vec3(halfW * 0.42f, hoodH - 0.18f, -halfL - 0.02f),
                    Vec3(halfW * 0.42f, hoodH - 0.04f, -halfL - 0.02f),
                    Vec3(halfW * 0.88f, hoodH - 0.04f, -halfL - 0.02f),
                    Vec3(halfW * 0.88f, hoodH - 0.18f, -halfL - 0.02f),
                    taillightColor,
                    isEmissive = true,
                    normal = -Vec3.FORWARD
                )
            )

            // 8. Custom Rear Spoiler
            if (customization.spoilerStyle != SpoilerStyle.NONE) {
                val wingH = if (customization.spoilerStyle == SpoilerStyle.GT_WING || customization.spoilerStyle == SpoilerStyle.HYPER) 0.35f else 0.12f
                val wingZ = -halfL * 0.96f
                val wingY = hoodH + wingH
                val wingW = halfW * 0.98f

                // Wing blade
                faces.add(
                    Face3D(
                        Vec3(-wingW, wingY, wingZ - 0.22f),
                        Vec3(wingW, wingY, wingZ - 0.22f),
                        Vec3(wingW, wingY + 0.04f, wingZ),
                        Vec3(-wingW, wingY + 0.04f, wingZ),
                        Color(0xFF0F172A),
                        normal = Vec3.UP
                    )
                )
                // Left & Right Mount Struts
                if (customization.spoilerStyle == SpoilerStyle.GT_WING || customization.spoilerStyle == SpoilerStyle.HYPER) {
                    faces.add(Face3D(Vec3(-wingW * 0.6f, hoodH, wingZ - 0.1f), Vec3(-wingW * 0.6f, wingY, wingZ - 0.1f), Vec3(-wingW * 0.6f, wingY, wingZ), Vec3(-wingW * 0.6f, hoodH, wingZ), Color(0xFF1E293B)))
                    faces.add(Face3D(Vec3(wingW * 0.6f, hoodH, wingZ - 0.1f), Vec3(wingW * 0.6f, wingY, wingZ - 0.1f), Vec3(wingW * 0.6f, wingY, wingZ), Vec3(wingW * 0.6f, hoodH, wingZ), Color(0xFF1E293B)))
                }
            }

            // 9. 4 Wheels & Rims
            val wheelPositions = listOf(
                Vec3(-halfW * 1.02f, 0.32f, 1.25f),  // Front Left
                Vec3(halfW * 1.02f, 0.32f, 1.25f),   // Front Right
                Vec3(-halfW * 1.02f, 0.32f, -1.35f), // Rear Left
                Vec3(halfW * 1.02f, 0.32f, -1.35f)   // Rear Right
            )

            for ((idx, wPos) in wheelPositions.withIndex()) {
                val isLeft = idx % 2 == 0
                val r = 0.32f
                val wWidth = 0.22f
                val rimOffset = if (isLeft) -wWidth else wWidth

                // Outer tire face
                faces.add(
                    Face3D(
                        wPos + Vec3(rimOffset, -r, -r),
                        wPos + Vec3(rimOffset, r, -r),
                        wPos + Vec3(rimOffset, r, r),
                        wPos + Vec3(rimOffset, -r, r),
                        Color(0xFF1C1917),
                        normal = if (isLeft) -Vec3.RIGHT else Vec3.RIGHT
                    )
                )

                // Rim center hub
                val hubR = r * 0.62f
                faces.add(
                    Face3D(
                        wPos + Vec3(rimOffset * 1.01f, -hubR, -hubR),
                        wPos + Vec3(rimOffset * 1.01f, hubR, -hubR),
                        wPos + Vec3(rimOffset * 1.01f, hubR, hubR),
                        wPos + Vec3(rimOffset * 1.01f, -hubR, hubR),
                        rimColor,
                        isEmissive = (customization.rimStyle == RimStyle.NEON),
                        normal = if (isLeft) -Vec3.RIGHT else Vec3.RIGHT
                    )
                )
            }

            // 10. Nitro Exhaust Flames (when active)
            if (isNitroActive) {
                val flameZ = -halfL - 0.45f
                val flameColorCore = Color(0xFF00E5FF)
                val flameColorEdge = Color(0xFFFF5722)

                faces.add(
                    Face3D(
                        Vec3(-0.45f, baseH + 0.05f, -halfL),
                        Vec3(-0.35f, baseH + 0.05f, -halfL),
                        Vec3(-0.40f, baseH + 0.08f, flameZ),
                        color = flameColorCore,
                        isEmissive = true,
                        normal = -Vec3.FORWARD
                    )
                )
                faces.add(
                    Face3D(
                        Vec3(0.35f, baseH + 0.05f, -halfL),
                        Vec3(0.45f, baseH + 0.05f, -halfL),
                        Vec3(0.40f, baseH + 0.08f, flameZ),
                        color = flameColorCore,
                        isEmissive = true,
                        normal = -Vec3.FORWARD
                    )
                )
            }

            return Mesh3D(faces)
        }

        /**
         * Builds 3D meshes for AI Traffic vehicles (Bus, Truck, Motorcycle, Police, Sedan).
         */
        fun createTrafficMesh(type: TrafficType, colorHex: String, isSirenActive: Boolean = false, isBraking: Boolean = false): Mesh3D {
            val faces = ArrayList<Face3D>()
            val baseColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color(0xFF3B82F6))
            val dark = Color(0xFF1E293B)
            val glass = Color(0xCC64748B)

            val hw = type.width * 0.5f
            val hl = type.length * 0.5f
            val hh = type.height

            // Main body box
            faces.add(Face3D(Vec3(-hw, 0.2f, -hl), Vec3(hw, 0.2f, -hl), Vec3(hw, hh * 0.7f, -hl), Vec3(-hw, hh * 0.7f, -hl), baseColor, normal = -Vec3.FORWARD))
            faces.add(Face3D(Vec3(-hw, 0.2f, hl), Vec3(-hw, hh * 0.7f, hl), Vec3(hw, hh * 0.7f, hl), Vec3(hw, 0.2f, hl), baseColor, normal = Vec3.FORWARD))
            faces.add(Face3D(Vec3(-hw, 0.2f, -hl), Vec3(-hw, hh * 0.7f, -hl), Vec3(-hw, hh * 0.7f, hl), Vec3(-hw, 0.2f, hl), baseColor, normal = -Vec3.RIGHT))
            faces.add(Face3D(Vec3(hw, 0.2f, hl), Vec3(hw, hh * 0.7f, hl), Vec3(hw, hh * 0.7f, -hl), Vec3(hw, 0.2f, -hl), baseColor, normal = Vec3.RIGHT))
            faces.add(Face3D(Vec3(-hw, hh * 0.7f, -hl), Vec3(hw, hh * 0.7f, -hl), Vec3(hw, hh * 0.7f, hl), Vec3(-hw, hh * 0.7f, hl), baseColor, normal = Vec3.UP))

            // Headlights & Taillights
            faces.add(Face3D(Vec3(-hw * 0.8f, 0.4f, hl + 0.02f), Vec3(-hw * 0.3f, 0.4f, hl + 0.02f), Vec3(-hw * 0.3f, 0.6f, hl + 0.02f), Vec3(-hw * 0.8f, 0.6f, hl + 0.02f), Color(0xFFFFF9C4), isEmissive = true))
            faces.add(Face3D(Vec3(hw * 0.3f, 0.4f, hl + 0.02f), Vec3(hw * 0.8f, 0.4f, hl + 0.02f), Vec3(hw * 0.8f, 0.6f, hl + 0.02f), Vec3(hw * 0.3f, 0.6f, hl + 0.02f), Color(0xFFFFF9C4), isEmissive = true))

            val tailColor = if (isBraking) Color(0xFFFF1744) else Color(0xFFB71C1C)
            faces.add(Face3D(Vec3(-hw * 0.8f, 0.4f, -hl - 0.02f), Vec3(-hw * 0.8f, 0.6f, -hl - 0.02f), Vec3(-hw * 0.3f, 0.6f, -hl - 0.02f), Vec3(-hw * 0.3f, 0.4f, -hl - 0.02f), tailColor, isEmissive = true))
            faces.add(Face3D(Vec3(hw * 0.3f, 0.4f, -hl - 0.02f), Vec3(hw * 0.3f, 0.6f, -hl - 0.02f), Vec3(hw * 0.8f, 0.6f, -hl - 0.02f), Vec3(hw * 0.8f, 0.4f, -hl - 0.02f), tailColor, isEmissive = true))

            // Police Strobe Bar
            if (type == TrafficType.POLICE) {
                val strobeY = hh * 0.72f
                val strobeColorL = if (isSirenActive) Color(0xFFEF4444) else Color(0xFF991B1B)
                val strobeColorR = if (isSirenActive) Color(0xFF3B82F6) else Color(0xFF1E3A8A)

                faces.add(Face3D(Vec3(-hw * 0.5f, strobeY, 0f), Vec3(-0.05f, strobeY, 0f), Vec3(-0.05f, strobeY + 0.18f, 0f), Vec3(-hw * 0.5f, strobeY + 0.18f, 0f), strobeColorL, isEmissive = true))
                faces.add(Face3D(Vec3(0.05f, strobeY, 0f), Vec3(hw * 0.5f, strobeY, 0f), Vec3(hw * 0.5f, strobeY + 0.18f, 0f), Vec3(0.05f, strobeY + 0.18f, 0f), strobeColorR, isEmissive = true))
            }

            return Mesh3D(faces)
        }

        /**
         * Builds 3D Skyscraper with illuminated windows.
         */
        fun createBuildingMesh(scale: Vec3, color: Color): Mesh3D {
            val faces = ArrayList<Face3D>()
            val hw = scale.x * 0.5f
            val h = scale.y
            val hl = scale.z * 0.5f

            // Front face
            faces.add(Face3D(Vec3(-hw, 0f, hl), Vec3(hw, 0f, hl), Vec3(hw, h, hl), Vec3(-hw, h, hl), color, normal = Vec3.FORWARD))
            // Side faces
            faces.add(Face3D(Vec3(-hw, 0f, -hl), Vec3(-hw, h, -hl), Vec3(-hw, h, hl), Vec3(-hw, 0f, hl), color, normal = -Vec3.RIGHT))
            faces.add(Face3D(Vec3(hw, 0f, hl), Vec3(hw, h, hl), Vec3(hw, h, -hl), Vec3(hw, 0f, -hl), color, normal = Vec3.RIGHT))
            // Roof
            faces.add(Face3D(Vec3(-hw, h, -hl), Vec3(hw, h, -hl), Vec3(hw, h, hl), Vec3(-hw, h, hl), Color(0xFF0F172A), normal = Vec3.UP))

            // Glowing neon window strips
            val windowColor = Color(0xFFFEF08A)
            faces.add(Face3D(Vec3(-hw * 0.8f, h * 0.4f, hl + 0.05f), Vec3(hw * 0.8f, h * 0.4f, hl + 0.05f), Vec3(hw * 0.8f, h * 0.45f, hl + 0.05f), Vec3(-hw * 0.8f, h * 0.45f, hl + 0.05f), windowColor, isEmissive = true))
            faces.add(Face3D(Vec3(-hw * 0.8f, h * 0.7f, hl + 0.05f), Vec3(hw * 0.8f, h * 0.7f, hl + 0.05f), Vec3(hw * 0.8f, h * 0.75f, hl + 0.05f), Vec3(-hw * 0.8f, h * 0.75f, hl + 0.05f), windowColor, isEmissive = true))

            return Mesh3D(faces)
        }
    }
}
