package com.example.data.model

import com.example.engine.math.Vec3

enum class GameMode(val displayName: String, val description: String) {
    FREE_DRIVE("Free Drive", "Cruise freely across all 10 open-world zones with no limits."),
    TRAFFIC_DRIVE("Traffic Rush", "Navigate dense highway & city traffic. Score near-misses and avoid crashes!"),
    CIRCUIT_RACE("Circuit Race", "Compete against aggressive AI racers over 3 intense laps with cash rewards."),
    HILL_CLIMB("Hill Climb", "Tackle treacherous mountain hairpins, steep cliffs, and elevation jumps."),
    TIME_TRIAL("Time Attack", "Race against the clock and beat target split times for gold medals."),
    POLICE_CHASE("Police Pursuit", "High-heat chase! Outrun aggressive police interceptors and roadblocks.")
}

enum class WeatherType(val displayName: String) {
    SUNNY("Sunny Clear"),
    CLOUDY("Overcast"),
    RAIN("Heavy Rain"),
    FOG("Dense Fog"),
    SNOW("Blizzard")
}

enum class TimeOfDay(val displayName: String) {
    DAWN("Dawn / Sunrise"),
    DAY("Midday Sun"),
    SUNSET("Sunset / Golden"),
    NIGHT("Midnight Neon")
}

enum class CameraMode(val displayName: String) {
    THIRD_PERSON("Chase Cam"),
    FIRST_PERSON_COCKPIT("Cockpit Cam"),
    HOOD_BUMPER("Hood Cam")
}

enum class CarPaintFinish {
    GLOSS,
    MATTE,
    METALLIC,
    NEON
}

enum class RimStyle(val displayName: String) {
    STOCK("Factory Alloy"),
    SPORT("Dual 5-Spoke"),
    FORGED("Forged Deep Dish"),
    NEON("Aero Blade Neon")
}

enum class SpoilerStyle(val displayName: String) {
    NONE("No Wing (Clean)"),
    LIP("Ducktail Lip"),
    GT_WING("GT Carbon Wing"),
    HYPER("Hyper Velocity Spoiler")
}

enum class BumperKit(val displayName: String) {
    STOCK("Factory Spec"),
    AERO("Aero Splitter Kit"),
    TRACK("Widebody Track Package")
}

enum class ExhaustTip(val displayName: String) {
    STOCK("Dual Sport Oval"),
    QUAD("Titanium Quad Pipes"),
    CANNON("Big Bore Flame Cannon")
}

data class CarSpecs(
    val id: String,
    val name: String,
    val brand: String,
    val category: String,
    val defaultColorHex: String,
    val baseTopSpeedKmh: Float,
    val baseAcceleration: Float, // 0.0 to 1.0
    val baseHandling: Float,     // 0.0 to 1.0
    val baseBraking: Float,      // 0.0 to 1.0
    val baseNitroCapacity: Float,
    val price: Int,
    val unlockedByDefault: Boolean,
    val engineSoundPitch: Float = 1.0f
) {
    companion object {
        val ALL_CARS = listOf(
            CarSpecs(
                id = "apex_gt",
                name = "Apex GT-R",
                brand = "Apex Motors",
                category = "Sports Coupe",
                defaultColorHex = "#FF1E40",
                baseTopSpeedKmh = 265f,
                baseAcceleration = 0.70f,
                baseHandling = 0.78f,
                baseBraking = 0.75f,
                baseNitroCapacity = 100f,
                price = 0,
                unlockedByDefault = true,
                engineSoundPitch = 1.0f
            ),
            CarSpecs(
                id = "veloce_v12",
                name = "Veloce V12",
                brand = "Scuderia Veloce",
                category = "Hypercar",
                defaultColorHex = "#FFD700",
                baseTopSpeedKmh = 345f,
                baseAcceleration = 0.92f,
                baseHandling = 0.88f,
                baseBraking = 0.90f,
                baseNitroCapacity = 130f,
                price = 45000,
                unlockedByDefault = false,
                engineSoundPitch = 1.25f
            ),
            CarSpecs(
                id = "phantom_4x4",
                name = "Phantom Ridge",
                brand = "Apex Heavy",
                category = "Off-Road SUV",
                defaultColorHex = "#2E7D32",
                baseTopSpeedKmh = 220f,
                baseAcceleration = 0.65f,
                baseHandling = 0.68f,
                baseBraking = 0.85f,
                baseNitroCapacity = 90f,
                price = 18000,
                unlockedByDefault = false,
                engineSoundPitch = 0.85f
            ),
            CarSpecs(
                id = "thunder_muscle",
                name = "Thunder RT",
                brand = "Detroit Steel",
                category = "Muscle Beast",
                defaultColorHex = "#1565C0",
                baseTopSpeedKmh = 280f,
                baseAcceleration = 0.82f,
                baseHandling = 0.65f,
                baseBraking = 0.70f,
                baseNitroCapacity = 110f,
                price = 28000,
                unlockedByDefault = false,
                engineSoundPitch = 0.92f
            ),
            CarSpecs(
                id = "hyperion_proto",
                name = "Hyperion Zero",
                brand = "Apex Racing Lab",
                category = "Track Prototype",
                defaultColorHex = "#00E5FF",
                baseTopSpeedKmh = 390f,
                baseAcceleration = 0.98f,
                baseHandling = 0.95f,
                baseBraking = 0.98f,
                baseNitroCapacity = 150f,
                price = 95000,
                unlockedByDefault = false,
                engineSoundPitch = 1.35f
            )
        )

        fun find(id: String): CarSpecs = ALL_CARS.firstOrNull { it.id == id } ?: ALL_CARS[0]
    }
}

data class CarCustomization(
    val carId: String,
    val colorHex: String,
    val finish: CarPaintFinish = CarPaintFinish.GLOSS,
    val rimStyle: RimStyle = RimStyle.SPORT,
    val rimColorHex: String = "#CCCCCC",
    val spoilerStyle: SpoilerStyle = SpoilerStyle.LIP,
    val bumperKit: BumperKit = BumperKit.STOCK,
    val exhaustTip: ExhaustTip = ExhaustTip.STOCK,
    val engineStage: Int = 1,
    val brakeStage: Int = 1,
    val suspensionStage: Int = 1,
    val nitroStage: Int = 1
) {
    fun calculateEffectiveTopSpeed(baseSpeed: Float): Float {
        return baseSpeed + (engineStage - 1) * 12f + (nitroStage - 1) * 4f
    }

    fun calculateEffectiveAccel(baseAccel: Float): Float {
        return (baseAccel + (engineStage - 1) * 0.05f + (suspensionStage - 1) * 0.02f).coerceAtMost(1.0f)
    }

    fun calculateEffectiveHandling(baseHandling: Float): Float {
        return (baseHandling + (suspensionStage - 1) * 0.06f).coerceAtMost(1.0f)
    }

    fun calculateEffectiveBraking(baseBraking: Float): Float {
        return (baseBraking + (brakeStage - 1) * 0.06f).coerceAtMost(1.0f)
    }
}

enum class TrafficType(val displayName: String, val length: Float, val width: Float, val height: Float) {
    SEDAN("Sedan", 4.2f, 1.9f, 1.4f),
    SUV("SUV", 4.8f, 2.1f, 1.8f),
    BUS("City Transit Bus", 9.5f, 2.6f, 3.2f),
    TRUCK("Cargo Hauler", 8.0f, 2.5f, 2.9f),
    MOTORCYCLE("Superbike", 2.2f, 0.9f, 1.3f),
    POLICE("Police Cruiser", 4.6f, 2.0f, 1.5f)
}

enum class TrafficLightState {
    GREEN,
    YELLOW,
    RED
}

data class TrafficVehicle(
    val id: Int,
    val type: TrafficType,
    var position: Vec3,
    var velocity: Vec3,
    var headingRad: Float,
    var speedKmh: Float,
    var targetSpeedKmh: Float,
    val colorHex: String,
    var isBraking: Boolean = false,
    var sirenActive: Boolean = false,
    var isDestroyed: Boolean = false,
    var laneIndex: Int = 0
)

data class AiRacer(
    val id: Int,
    val name: String,
    val carSpecId: String,
    val colorHex: String,
    var position: Vec3,
    var headingRad: Float,
    var speedKmh: Float,
    var currentCheckpoint: Int = 0,
    var lap: Int = 1,
    var progressDistance: Float = 0f,
    var isBraking: Boolean = false
)

data class RaceState(
    val mode: GameMode,
    val totalLaps: Int = 3,
    var currentLap: Int = 1,
    var currentCheckpointIndex: Int = 0,
    var totalCheckpoints: Int = 12,
    var raceTimeMs: Long = 0L,
    var lapTimeMs: Long = 0L,
    var bestLapTimeMs: Long = 0L,
    var playerPosition: Int = 1,
    var totalRacers: Int = 4,
    var isFinished: Boolean = false,
    var isCountdown: Boolean = true,
    var countdownValue: Int = 3,
    var earnedReward: Int = 0,
    var nearMissCount: Int = 0,
    var comboScore: Int = 0,
    var heatLevel: Int = 1 // for Police Chase (1 to 5 stars)
)
