package com.example.engine.world

import androidx.compose.ui.graphics.Color
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherType
import com.example.engine.math.Vec3
import kotlin.random.Random

data class WeatherParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var speed: Float,
    var length: Float = 1.0f
)

class WeatherSystem {

    var weather: WeatherType = WeatherType.SUNNY
    var timeOfDay: TimeOfDay = TimeOfDay.DAY

    var headlightsOn: Boolean = false

    val particles: ArrayList<WeatherParticle> = ArrayList()

    init {
        // Initialize 120 weather particles for Rain/Snow
        for (i in 0 until 120) {
            particles.add(
                WeatherParticle(
                    x = (Random.nextFloat() - 0.5f) * 60f,
                    y = Random.nextFloat() * 25f + 1f,
                    z = (Random.nextFloat() - 0.5f) * 60f,
                    speed = Random.nextFloat() * 18f + 12f,
                    length = Random.nextFloat() * 1.5f + 0.8f
                )
            )
        }
    }

    fun update(dt: Float, playerPos: Vec3) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Headlights automatically turn on during Night, Fog or Rain
        headlightsOn = (timeOfDay == TimeOfDay.NIGHT || timeOfDay == TimeOfDay.DAWN ||
                weather == WeatherType.RAIN || weather == WeatherType.FOG)

        // Update weather particle positions relative to player
        if (weather == WeatherType.RAIN || weather == WeatherType.SNOW) {
            val isSnow = weather == WeatherType.SNOW
            val fallSpeed = if (isSnow) 6f else 28f

            for (p in particles) {
                p.y -= fallSpeed * clampedDt
                if (isSnow) {
                    p.x += Random.nextFloat() * 0.4f - 0.2f
                }
                if (p.y < 0.2f) {
                    p.y = 22f + Random.nextFloat() * 8f
                    p.x = playerPos.x + (Random.nextFloat() - 0.5f) * 55f
                    p.z = playerPos.z + (Random.nextFloat() - 0.5f) * 55f
                }
            }
        }
    }

    fun getSkyColors(): Pair<Color, Color> {
        return when (timeOfDay) {
            TimeOfDay.DAWN -> Pair(Color(0xFF312E81), Color(0xFFFB923C)) // Deep purple to amber
            TimeOfDay.DAY -> when (weather) {
                WeatherType.CLOUDY, WeatherType.RAIN -> Pair(Color(0xFF475569), Color(0xFF94A3B8))
                WeatherType.FOG -> Pair(Color(0xFF94A3B8), Color(0xFFCBD5E1))
                WeatherType.SNOW -> Pair(Color(0xFF64748B), Color(0xFFE2E8F0))
                WeatherType.SUNNY -> Pair(Color(0xFF0284C7), Color(0xFF7DD3FC)) // Vibrant azure sky
            }
            TimeOfDay.SUNSET -> Pair(Color(0xFF581C87), Color(0xFFEA580C)) // Purple to crimson sunset
            TimeOfDay.NIGHT -> Pair(Color(0xFF030712), Color(0xFF111827)) // Deep space obsidian night
        }
    }

    fun getAmbientLightFactor(): Float {
        val timeBase = when (timeOfDay) {
            TimeOfDay.DAY -> 1.0f
            TimeOfDay.SUNSET -> 0.75f
            TimeOfDay.DAWN -> 0.65f
            TimeOfDay.NIGHT -> 0.25f
        }
        val weatherFactor = when (weather) {
            WeatherType.SUNNY -> 1.0f
            WeatherType.CLOUDY -> 0.85f
            WeatherType.RAIN -> 0.70f
            WeatherType.SNOW -> 0.80f
            WeatherType.FOG -> 0.60f
        }
        return timeBase * weatherFactor
    }

    fun getSunLightDirection(): Vec3 {
        return when (timeOfDay) {
            TimeOfDay.DAY -> Vec3(0.4f, -0.85f, 0.35f).normalized()
            TimeOfDay.SUNSET -> Vec3(0.85f, -0.3f, 0.45f).normalized()
            TimeOfDay.DAWN -> Vec3(-0.85f, -0.3f, 0.45f).normalized()
            TimeOfDay.NIGHT -> Vec3(0.2f, -0.9f, 0.4f).normalized() // Moonlight
        }
    }
}
