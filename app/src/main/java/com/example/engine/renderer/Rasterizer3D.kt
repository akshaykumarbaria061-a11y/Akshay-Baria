package com.example.engine.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.model.CameraMode
import com.example.data.model.WeatherType
import com.example.engine.math.Mat4
import com.example.engine.math.Vec3
import com.example.engine.physics.CarPhysics
import com.example.engine.world.RoadPoint
import com.example.engine.world.RoadZone
import com.example.engine.world.TrafficSystem
import com.example.engine.world.WeatherSystem
import com.example.engine.world.WorldMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Rasterizer3D {

    // Reusable buffers to avoid allocations per frame
    private val projectedQuad = Array(4) { Offset.Zero }
    private val polygonPath = Path()

    fun render(
        drawScope: DrawScope,
        camera: Camera3D,
        car: CarPhysics,
        worldMap: WorldMap,
        trafficSystem: TrafficSystem,
        weatherSystem: WeatherSystem
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0f || height <= 0f) return

        val aspect = width / height
        val viewMatrix = camera.getViewMatrix()
        val projMatrix = camera.getProjectionMatrix(aspect)
        val viewProjMatrix = projMatrix * viewMatrix

        // 1. Render Sky & Atmosphere
        val (skyTop, skyBottom) = weatherSystem.getSkyColors()
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyBottom),
                startY = 0f,
                endY = height * 0.65f
            ),
            size = drawScope.size
        )

        // 2. Render Road Ribbon & Terrain Mesh
        val closestPt = worldMap.getClosestRoadPoint(car.position)
        val startRoadIdx = closestPt.roadIndex
        val visibleDistance = 55 // Segments ahead to draw

        // Collect road segments ordered far to near
        for (step in visibleDistance downTo 0) {
            val idx0 = (startRoadIdx + step) % worldMap.roadPoints.size
            val idx1 = (startRoadIdx + step + 1) % worldMap.roadPoints.size

            val p0 = worldMap.roadPoints[idx0]
            val p1 = worldMap.roadPoints[idx1]

            // Check if behind camera
            val camToPt = p0.position - camera.eye
            if (camToPt.dot(camera.target - camera.eye) < -1f) continue

            renderRoadSegment(drawScope, viewProjMatrix, width, height, p0, p1, weatherSystem, car)
        }

        // 3. Render 3D Props near the road
        renderProps(drawScope, viewProjMatrix, width, height, camera, worldMap, weatherSystem)

        // 4. Render Traffic Vehicles & AI Racers
        renderTraffic(drawScope, viewProjMatrix, width, height, camera, trafficSystem, weatherSystem)

        // 5. Render Player Car (if in Third Person mode or Showroom)
        if (camera.mode == CameraMode.THIRD_PERSON) {
            renderPlayerCar(drawScope, viewProjMatrix, width, height, car, weatherSystem)
        }

        // 6. Render Cockpit View (if in Cockpit Mode)
        if (camera.mode == CameraMode.FIRST_PERSON_COCKPIT) {
            renderCockpit(drawScope, width, height, car, weatherSystem)
        }

        // 7. Render Dynamic Weather Particles (Rain / Snow)
        renderWeatherParticles(drawScope, viewProjMatrix, width, height, weatherSystem, camera)
    }

    private fun renderRoadSegment(
        drawScope: DrawScope,
        vp: Mat4,
        w: Float,
        h: Float,
        p0: RoadPoint,
        p1: RoadPoint,
        weather: WeatherSystem,
        car: CarPhysics
    ) {
        val halfW0 = p0.width * 0.5f
        val halfW1 = p1.width * 0.5f

        // Outer shoulder left & right
        val l0 = p0.position - p0.right * (halfW0 + 2.5f)
        val r0 = p0.position + p0.right * (halfW0 + 2.5f)
        val l1 = p1.position - p1.right * (halfW1 + 2.5f)
        val r1 = p1.position + p1.right * (halfW1 + 2.5f)

        // Project road shoulder
        val sl0 = projectToScreen(vp, l0, w, h) ?: return
        val sr0 = projectToScreen(vp, r0, w, h) ?: return
        val sl1 = projectToScreen(vp, l1, w, h) ?: return
        val sr1 = projectToScreen(vp, r1, w, h) ?: return

        drawQuad(drawScope, sl0, sr0, sr1, sl1, p0.zone.shoulderColor)

        // Inner asphalt roadway
        val al0 = projectToScreen(vp, p0.position - p0.right * halfW0, w, h) ?: return
        val ar0 = projectToScreen(vp, p0.position + p0.right * halfW0, w, h) ?: return
        val al1 = projectToScreen(vp, p1.position - p1.right * halfW1, w, h) ?: return
        val ar1 = projectToScreen(vp, p1.position + p1.right * halfW1, w, h) ?: return

        // Asphalt with wet sheen during rain
        val asphaltColor = if (weather.weather == WeatherType.RAIN) Color(0xFF171A21) else p0.zone.surfaceColor
        drawQuad(drawScope, al0, ar0, ar1, al1, asphaltColor)

        // Center dashed dividing line
        if (p0.roadIndex % 2 == 0) {
            val cWidth0 = 0.25f
            val cWidth1 = 0.25f
            val cl0 = projectToScreen(vp, p0.position - p0.right * cWidth0 + Vec3(0f, 0.02f, 0f), w, h) ?: return
            val cr0 = projectToScreen(vp, p0.position + p0.right * cWidth0 + Vec3(0f, 0.02f, 0f), w, h) ?: return
            val cl1 = projectToScreen(vp, p1.position - p1.right * cWidth1 + Vec3(0f, 0.02f, 0f), w, h) ?: return
            val cr1 = projectToScreen(vp, p1.position + p1.right * cWidth1 + Vec3(0f, 0.02f, 0f), w, h) ?: return

            val dashColor = if (p0.zone == RoadZone.HIGHWAY || p0.zone == RoadZone.CITY) Color(0xFFFACC15) else Color(0xFFFFFFFF)
            drawQuad(drawScope, cl0, cr0, cr1, cl1, dashColor)
        }

        // Headlight glow on road
        if (weather.headlightsOn) {
            val distToCar = (p0.position - car.position).length()
            if (distToCar < 35f && (p0.position - car.position).dot(car.forward) > 0f) {
                val headlightAlpha = ((35f - distToCar) / 35f) * 0.28f
                drawQuad(drawScope, al0, ar0, ar1, al1, Color(0xFFFFFBEB).copy(alpha = headlightAlpha))
            }
        }
    }

    private fun renderProps(
        drawScope: DrawScope,
        vp: Mat4,
        w: Float,
        h: Float,
        camera: Camera3D,
        worldMap: WorldMap,
        weather: WeatherSystem
    ) {
        val ambient = weather.getAmbientLightFactor()

        // Filter props in front of camera within 180 meters
        val visibleProps = worldMap.props.filter { prop ->
            val toProp = prop.position - camera.eye
            val distSq = toProp.lengthSq()
            distSq < 180f * 180f && toProp.dot(camera.target - camera.eye) > 0f
        }.sortedByDescending { (it.position - camera.eye).lengthSq() }

        for (prop in visibleProps) {
            val sPos = projectToScreen(vp, prop.position, w, h) ?: continue
            val sTop = projectToScreen(vp, prop.position + Vec3(0f, prop.scale.y, 0f), w, h) ?: continue
            val propHeight = abs(sPos.y - sTop.y).coerceAtLeast(4f)
            val propWidth = (propHeight * (prop.scale.x / prop.scale.y.coerceAtLeast(1f))).coerceAtLeast(3f)

            val left = sPos.x - propWidth * 0.5f
            val top = sTop.y

            val shadedColor = Color(
                (prop.color.red * ambient).coerceIn(0f, 1f),
                (prop.color.green * ambient).coerceIn(0f, 1f),
                (prop.color.blue * ambient).coerceIn(0f, 1f),
                prop.color.alpha
            )

            when (prop.type) {
                com.example.engine.world.PropType.BUILDING_SKYSCRAPER,
                com.example.engine.world.PropType.BUILDING_SHOP,
                com.example.engine.world.PropType.HOUSE -> {
                    drawScope.drawRect(
                        color = shadedColor,
                        topLeft = Offset(left, top),
                        size = Size(propWidth, propHeight)
                    )
                    // Building roof cap
                    drawScope.drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(left - 2f, top),
                        size = Size(propWidth + 4f, propHeight * 0.08f)
                    )
                }
                com.example.engine.world.PropType.TREE_PINE -> {
                    // Draw pine tree trunk & triangular canopy
                    val trunkW = propWidth * 0.2f
                    drawScope.drawRect(
                        color = Color(0xFF451A03),
                        topLeft = Offset(sPos.x - trunkW * 0.5f, sPos.y - propHeight * 0.25f),
                        size = Size(trunkW, propHeight * 0.25f)
                    )
                    val treePath = Path().apply {
                        moveTo(sPos.x, top)
                        lineTo(sPos.x + propWidth * 0.55f, sPos.y - propHeight * 0.2f)
                        lineTo(sPos.x - propWidth * 0.55f, sPos.y - propHeight * 0.2f)
                        close()
                    }
                    drawScope.drawPath(treePath, shadedColor)
                }
                com.example.engine.world.PropType.TREE_PALM -> {
                    drawScope.drawCircle(
                        color = shadedColor,
                        radius = propWidth * 0.45f,
                        center = Offset(sPos.x, top + propWidth * 0.3f)
                    )
                }
                com.example.engine.world.PropType.BRIDGE_PYLON -> {
                    drawScope.drawRect(
                        color = prop.color,
                        topLeft = Offset(left, top),
                        size = Size(propWidth, propHeight)
                    )
                }
                com.example.engine.world.PropType.STREET_LIGHT -> {
                    drawScope.drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(sPos.x, sPos.y),
                        end = Offset(sPos.x, top),
                        strokeWidth = 3f
                    )
                    // Glowing lamp head
                    val glowColor = if (weather.headlightsOn) Color(0xFFFDE047) else Color(0xFF94A3B8)
                    drawScope.drawCircle(
                        color = glowColor,
                        radius = (propHeight * 0.12f).coerceIn(3f, 10f),
                        center = Offset(sPos.x, top)
                    )
                }
                else -> {
                    drawScope.drawRect(
                        color = shadedColor,
                        topLeft = Offset(left, top),
                        size = Size(propWidth, propHeight)
                    )
                }
            }
        }
    }

    private fun renderTraffic(
        drawScope: DrawScope,
        vp: Mat4,
        w: Float,
        h: Float,
        camera: Camera3D,
        trafficSystem: TrafficSystem,
        weather: WeatherSystem
    ) {
        val ambient = weather.getAmbientLightFactor()

        // Render AI Traffic vehicles
        for (v in trafficSystem.vehicles) {
            val dist = (v.position - camera.eye).length()
            if (dist > 150f) continue

            val mesh = Mesh3D.createTrafficMesh(v.type, v.colorHex, v.sirenActive, v.isBraking)
            val modelMat = Mat4.translation(v.position.x, v.position.y, v.position.z) *
                    Mat4.rotationY(v.headingRad)
            val transMesh = mesh.transform(modelMat)

            renderMeshFaces(drawScope, vp, w, h, transMesh, ambient)
        }

        // Render AI Racers
        for (racer in trafficSystem.aiRacers) {
            val dist = (racer.position - camera.eye).length()
            if (dist > 150f) continue

            val mesh = Mesh3D.createTrafficMesh(com.example.data.model.TrafficType.SEDAN, racer.colorHex, false, racer.isBraking)
            val modelMat = Mat4.translation(racer.position.x, racer.position.y, racer.position.z) *
                    Mat4.rotationY(racer.headingRad)
            val transMesh = mesh.transform(modelMat)

            renderMeshFaces(drawScope, vp, w, h, transMesh, ambient)
        }
    }

    private fun renderPlayerCar(
        drawScope: DrawScope,
        vp: Mat4,
        w: Float,
        h: Float,
        car: CarPhysics,
        weather: WeatherSystem
    ) {
        val ambient = weather.getAmbientLightFactor()
        val carMesh = Mesh3D.createCarMesh(
            spec = car.spec,
            customization = car.customization,
            isBraking = (car.currentGear == -1 || car.lateralSlip > 0.4f || car.isHandbraking),
            isNitroActive = car.isNitroActive,
            steerAngle = car.steeringAngleRad
        )

        // Apply car world transformation (translation + heading + suspension pitch/roll)
        val carModelMat = Mat4.translation(car.position.x, car.position.y, car.position.z) *
                Mat4.rotationY(car.headingRad) *
                Mat4.rotationX(car.suspensionPitch) *
                Mat4.rotationZ(car.suspensionRoll)

        val transCarMesh = carMesh.transform(carModelMat)
        renderMeshFaces(drawScope, vp, w, h, transCarMesh, ambient)
    }

    private fun renderMeshFaces(
        drawScope: DrawScope,
        vp: Mat4,
        w: Float,
        h: Float,
        mesh: Mesh3D,
        ambient: Float
    ) {
        for (face in mesh.faces) {
            val s0 = projectToScreen(vp, face.v0, w, h) ?: continue
            val s1 = projectToScreen(vp, face.v1, w, h) ?: continue
            val s2 = projectToScreen(vp, face.v2, w, h) ?: continue
            val s3 = face.v3?.let { projectToScreen(vp, it, w, h) }

            val faceColor = if (face.isEmissive) {
                face.color
            } else {
                // Directional diffuse lighting from sun
                val lightDot = (face.normal.dot(Vec3(0.3f, 0.85f, 0.45f).normalized())).coerceIn(0.2f, 1.0f)
                val litFactor = (lightDot * ambient).coerceIn(0.15f, 1.0f)
                Color(
                    (face.color.red * litFactor).coerceIn(0f, 1f),
                    (face.color.green * litFactor).coerceIn(0f, 1f),
                    (face.color.blue * litFactor).coerceIn(0f, 1f),
                    face.color.alpha
                )
            }

            if (s3 != null) {
                drawQuad(drawScope, s0, s1, s2, s3, faceColor)
            } else {
                drawTriangle(drawScope, s0, s1, s2, faceColor)
            }
        }
    }

    private fun renderCockpit(
        drawScope: DrawScope,
        w: Float,
        h: Float,
        car: CarPhysics,
        weather: WeatherSystem
    ) {
        // Cockpit Dashboard shroud at bottom of screen
        val dashTop = h * 0.68f

        // Dark matte leather dashboard base
        drawScope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(0f, dashTop),
            size = Size(w, h - dashTop)
        )
        // Dashboard trim highlight
        drawScope.drawLine(
            color = Color(0xFF00F0FF),
            start = Offset(0f, dashTop),
            end = Offset(w, dashTop),
            strokeWidth = 3f
        )

        // Central Animated Steering Wheel (centered left at driver position)
        val wheelCenter = Offset(w * 0.35f, h * 0.82f)
        val wheelRadius = h * 0.22f
        val steerAngleDeg = car.steeringAngleRad * 180f / PI.toFloat() * 2.8f

        // Outer rim
        drawScope.drawCircle(
            color = Color(0xFF1E293B),
            radius = wheelRadius,
            center = wheelCenter,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24f)
        )
        // Wheel center badge
        drawScope.drawCircle(
            color = Color(0xFFFF2A55),
            radius = wheelRadius * 0.28f,
            center = wheelCenter
        )

        // Rearview mirror at top center of windshield
        val mirrorW = w * 0.32f
        val mirrorH = h * 0.11f
        val mirrorX = (w - mirrorW) * 0.5f
        val mirrorY = h * 0.04f

        drawScope.drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(mirrorX - 4f, mirrorY - 4f),
            size = Size(mirrorW + 8f, mirrorH + 8f)
        )
        drawScope.drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(mirrorX, mirrorY),
            size = Size(mirrorW, mirrorH)
        )
    }

    private fun renderWeatherParticles(
        drawScope: DrawScope,
        vp: Mat4,
        w: Float,
        h: Float,
        weather: WeatherSystem,
        camera: Camera3D
    ) {
        if (weather.weather != WeatherType.RAIN && weather.weather != WeatherType.SNOW) return

        val isSnow = (weather.weather == WeatherType.SNOW)
        val color = if (isSnow) Color.White.copy(alpha = 0.85f) else Color(0xFFBAE6FD).copy(alpha = 0.65f)

        for (p in weather.particles) {
            val worldP = Vec3(p.x, p.y, p.z)
            val s0 = projectToScreen(vp, worldP, w, h) ?: continue
            val s1 = projectToScreen(vp, worldP + Vec3(0f, -p.length, 0f), w, h) ?: continue

            if (isSnow) {
                drawScope.drawCircle(color, radius = 2.5f, center = s0)
            } else {
                drawScope.drawLine(color, s0, s1, strokeWidth = 1.8f)
            }
        }
    }

    private fun projectToScreen(vp: Mat4, worldPos: Vec3, screenW: Float, screenH: Float): Offset? {
        val clip = vp.transformPoint(worldPos)
        // Guard against behind-near-plane projection
        if (clip.z < 0.05f || clip.z > 1.0f) return null

        val sx = (clip.x + 1f) * 0.5f * screenW
        val sy = (1f - clip.y) * 0.5f * screenH
        return Offset(sx, sy)
    }

    private fun drawQuad(drawScope: DrawScope, p0: Offset, p1: Offset, p2: Offset, p3: Offset, color: Color) {
        polygonPath.reset()
        polygonPath.moveTo(p0.x, p0.y)
        polygonPath.lineTo(p1.x, p1.y)
        polygonPath.lineTo(p2.x, p2.y)
        polygonPath.lineTo(p3.x, p3.y)
        polygonPath.close()
        drawScope.drawPath(polygonPath, color)
    }

    private fun drawTriangle(drawScope: DrawScope, p0: Offset, p1: Offset, p2: Offset, color: Color) {
        polygonPath.reset()
        polygonPath.moveTo(p0.x, p0.y)
        polygonPath.lineTo(p1.x, p1.y)
        polygonPath.lineTo(p2.x, p2.y)
        polygonPath.close()
        drawScope.drawPath(polygonPath, color)
    }
}
