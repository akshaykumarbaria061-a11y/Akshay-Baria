package com.example.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.RaceRecordEntity
import com.example.data.model.CameraMode
import com.example.data.model.CarCustomization
import com.example.data.model.CarSpecs
import com.example.data.model.GameMode
import com.example.data.model.RaceState
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherType
import com.example.data.repository.GameRepository
import com.example.engine.audio.GameAudioEngine
import com.example.engine.physics.CarPhysics
import com.example.engine.renderer.Camera3D
import com.example.engine.renderer.Rasterizer3D
import com.example.engine.world.RoadZone
import com.example.engine.world.TrafficSystem
import com.example.engine.world.WeatherSystem
import com.example.engine.world.WorldMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    RACING_GAME,
    GARAGE,
    LEADERBOARD
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(AppDatabase.getInstance(application))

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Database state
    val profile: StateFlow<PlayerProfileEntity> = repository.profileFlow
        .let { flow ->
            val state = MutableStateFlow(PlayerProfileEntity())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    val customizations: StateFlow<Map<String, CarCustomization>> = repository.customizationsFlow
        .let { flow ->
            val state = MutableStateFlow<Map<String, CarCustomization>>(emptyMap())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    val records: StateFlow<List<RaceRecordEntity>> = repository.recordsFlow
        .let { flow ->
            val state = MutableStateFlow<List<RaceRecordEntity>>(emptyList())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    // Engine subsystems
    val worldMap = WorldMap()
    val trafficSystem = TrafficSystem(worldMap)
    val weatherSystem = WeatherSystem()
    val camera = Camera3D()
    val rasterizer = Rasterizer3D()
    val audioEngine = GameAudioEngine()

    // Physics & Car
    var carPhysics: CarPhysics = CarPhysics(
        CarSpecs.ALL_CARS[0],
        CarCustomization(CarSpecs.ALL_CARS[0].id, CarSpecs.ALL_CARS[0].defaultColorHex)
    )

    // Dynamic UI observables
    val speedKmh = MutableStateFlow(0f)
    val rpm = MutableStateFlow(1000f)
    val currentGear = MutableStateFlow(1)
    val nitroPercent = MutableStateFlow(1f)
    val healthPercent = MutableStateFlow(1f)
    val isNitroActive = MutableStateFlow(false)
    val currentZone = MutableStateFlow(RoadZone.CITY)
    val raceState = MutableStateFlow(RaceState(GameMode.FREE_DRIVE))

    val isPaused = MutableStateFlow(false)
    val isGameOver = MutableStateFlow(false)
    val isWrecked = MutableStateFlow(false)
    val isWeatherDialogOpen = MutableStateFlow(false)
    val isAudioMuted = MutableStateFlow(false)

    // Touch inputs
    var inputThrottle = 0f
    var inputBrake = 0f
    var inputSteer = 0f
    var inputHandbrake = false
    var inputNitro = false

    private var gameLoopJob: Job? = null
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            audioEngine.start()
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen != AppScreen.RACING_GAME) {
            stopGameLoop()
        }
        _currentScreen.value = screen
    }

    fun startGame(mode: GameMode) {
        val carSpec = CarSpecs.find(profile.value.currentCarId)
        val carCust = customizations.value[carSpec.id] ?: CarCustomization(carSpec.id, carSpec.defaultColorHex)

        carPhysics = CarPhysics(carSpec, carCust)
        audioEngine.baseEnginePitch = carSpec.engineSoundPitch

        // Start position depends on mode
        val startRoadIdx = when (mode) {
            GameMode.HILL_CLIMB -> 241 // Starts right at Dragon Ridge mountain pass!
            GameMode.FREE_DRIVE -> 0
            GameMode.TRAFFIC_DRIVE -> 120 // Highway traffic rush
            GameMode.POLICE_CHASE -> 0
            else -> 0
        }

        val startPt = worldMap.getRoadPoint(startRoadIdx)
        val heading = kotlin.math.atan2(startPt.forward.x, startPt.forward.z)
        carPhysics.resetAt(startPt.position + kotlin.math.sin(heading).let { com.example.engine.math.Vec3(0f, 0.4f, 0f) }, heading)

        trafficSystem.initTraffic(mode)

        // Initialize Race State
        raceState.value = RaceState(
            mode = mode,
            totalLaps = if (mode == GameMode.CIRCUIT_RACE) 3 else 1,
            totalCheckpoints = worldMap.checkpoints.size,
            isCountdown = (mode == GameMode.CIRCUIT_RACE || mode == GameMode.TIME_TRIAL),
            countdownValue = 3
        )

        isPaused.value = false
        isGameOver.value = false
        isWrecked.value = false

        audioEngine.isEngineRunning = true
        _currentScreen.value = AppScreen.RACING_GAME

        // Start Countdown if race mode
        if (raceState.value.isCountdown) {
            startCountdown()
        }

        startGameLoop()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (count in 3 downTo 1) {
                raceState.value = raceState.value.copy(countdownValue = count)
                delay(900)
            }
            raceState.value = raceState.value.copy(countdownValue = 0)
            delay(500)
            raceState.value = raceState.value.copy(isCountdown = false)
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = SystemClock.elapsedRealtime()

            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val dt = ((now - lastTime) / 1000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                if (!isPaused.value && !isGameOver.value) {
                    stepGame(dt)
                }

                delay(16) // Approx 60 FPS
            }
        }
    }

    private fun stepGame(dt: Float) {
        val isFrozen = raceState.value.isCountdown

        val th = if (isFrozen) 0f else inputThrottle
        val br = if (isFrozen) 0f else inputBrake
        val st = if (isFrozen) 0f else inputSteer
        val hb = if (isFrozen) false else inputHandbrake
        val nt = if (isFrozen) false else inputNitro

        // 1. Update Physics
        carPhysics.step(dt, th, br, st, hb, nt, worldMap)

        // 2. Update Audio Engine
        audioEngine.rpmNorm = (carPhysics.rpm - carPhysics.idleRpm) / (carPhysics.maxRpm - carPhysics.idleRpm)
        audioEngine.throttle = th
        audioEngine.isNitroActive = carPhysics.isNitroActive
        audioEngine.tireSlip = carPhysics.lateralSlip
        audioEngine.isSirenActive = (raceState.value.mode == GameMode.POLICE_CHASE)

        // 3. Update Weather & Traffic Signals
        weatherSystem.update(dt, carPhysics.position)
        worldMap.updateTrafficSignals(dt)

        // 4. Update Traffic & AI Racers
        trafficSystem.update(
            dt = dt,
            playerPhysics = carPhysics,
            mode = raceState.value.mode,
            onNearMiss = { count ->
                raceState.value = raceState.value.copy(
                    nearMissCount = count,
                    earnedReward = raceState.value.earnedReward + 150
                )
            },
            onCollision = {
                audioEngine.triggerCrash = true
            }
        )

        // 5. Update Camera
        camera.updateGameCamera(dt, carPhysics)

        // 6. Checkpoint & Lap Progress
        val closestRoad = worldMap.getClosestRoadPoint(carPhysics.position)
        currentZone.value = closestRoad.zone

        val rState = raceState.value
        if (!rState.isCountdown && !rState.isFinished) {
            rState.raceTimeMs += (dt * 1000).toLong()
            rState.lapTimeMs += (dt * 1000).toLong()

            // Check distance to next checkpoint
            val nextCpIdx = (rState.currentCheckpointIndex + 1) % worldMap.checkpoints.size
            val cpPos = worldMap.checkpoints[nextCpIdx]
            val distToCp = (cpPos - carPhysics.position).length()

            if (distToCp < 22f) {
                rState.currentCheckpointIndex = nextCpIdx
                if (nextCpIdx == 0) {
                    // Completed a lap!
                    if (rState.bestLapTimeMs == 0L || rState.lapTimeMs < rState.bestLapTimeMs) {
                        rState.bestLapTimeMs = rState.lapTimeMs
                    }
                    rState.lapTimeMs = 0L

                    if (rState.currentLap < rState.totalLaps) {
                        rState.currentLap++
                    } else {
                        // Race completed!
                        finishRace(won = (rState.playerPosition == 1))
                    }
                }
            }

            // Update Race Position relative to AI Racers in Circuit Race
            if (rState.mode == GameMode.CIRCUIT_RACE) {
                var pos = 1
                for (racer in trafficSystem.aiRacers) {
                    if (racer.lap > rState.currentLap || (racer.lap == rState.currentLap && racer.currentCheckpoint > rState.currentCheckpointIndex)) {
                        pos++
                    }
                }
                rState.playerPosition = pos
            }

            // Police Chase Heat Progression
            if (rState.mode == GameMode.POLICE_CHASE) {
                rState.heatLevel = (1 + (rState.raceTimeMs / 25000)).toInt().coerceIn(1, 5)
                // Evaded successfully after 90 seconds!
                if (rState.raceTimeMs > 90000L) {
                    finishRace(won = true)
                }
            }
        }

        // 7. Check Car Destruction
        if (carPhysics.isDestroyed && !isGameOver.value) {
            isGameOver.value = true
            isWrecked.value = true
            audioEngine.triggerCrash = true
            stopGameLoop()
        }

        // Publish observables to UI
        speedKmh.value = carPhysics.speedKmh
        rpm.value = carPhysics.rpm
        currentGear.value = carPhysics.currentGear
        nitroPercent.value = carPhysics.nitroFuel / carPhysics.maxNitroFuel
        healthPercent.value = carPhysics.health / 100f
        isNitroActive.value = carPhysics.isNitroActive
    }

    private fun finishRace(won: Boolean) {
        val rState = raceState.value
        rState.isFinished = true
        isGameOver.value = true

        val reward = when (rState.mode) {
            GameMode.CIRCUIT_RACE -> if (won) 8500 else 3200
            GameMode.POLICE_CHASE -> 6000
            GameMode.HILL_CLIMB -> 4500
            GameMode.TIME_TRIAL -> 4000
            else -> 2000
        } + rState.nearMissCount * 150

        rState.earnedReward = reward

        viewModelScope.launch {
            repository.recordRaceFinished(
                modeName = rState.mode.name,
                trackName = currentZone.value.title,
                timeMs = rState.raceTimeMs,
                isWin = won,
                distanceKm = (carPhysics.speedKmh / 3600f) * (rState.raceTimeMs / 1000f),
                rewardCoins = reward
            )
        }
        stopGameLoop()
    }

    fun toggleCamera() {
        camera.mode = when (camera.mode) {
            CameraMode.THIRD_PERSON -> CameraMode.FIRST_PERSON_COCKPIT
            CameraMode.FIRST_PERSON_COCKPIT -> CameraMode.HOOD_BUMPER
            CameraMode.HOOD_BUMPER -> CameraMode.THIRD_PERSON
        }
    }

    fun toggleHeadlights() {
        weatherSystem.headlightsOn = !weatherSystem.headlightsOn
    }

    fun setWeather(w: WeatherType) {
        weatherSystem.weather = w
    }

    fun setTimeOfDay(t: TimeOfDay) {
        weatherSystem.timeOfDay = t
    }

    fun togglePause() {
        isPaused.value = !isPaused.value
    }

    fun toggleAudio() {
        isAudioMuted.value = !isAudioMuted.value
        audioEngine.isMuted = isAudioMuted.value
    }

    fun selectCar(carId: String) {
        viewModelScope.launch {
            repository.updateCurrentCar(carId)
        }
    }

    fun purchaseCar(spec: CarSpecs) {
        viewModelScope.launch {
            if (repository.spendCoins(spec.price)) {
                val newCust = CarCustomization(spec.id, spec.defaultColorHex)
                repository.saveCustomization(newCust, isUnlocked = true)
                repository.updateCurrentCar(spec.id)
            }
        }
    }

    fun updateCustomization(cust: CarCustomization) {
        viewModelScope.launch {
            repository.saveCustomization(cust)
        }
    }

    fun purchaseUpgrade(cust: CarCustomization, upgradeType: String, cost: Int) {
        viewModelScope.launch {
            if (repository.spendCoins(cost)) {
                val updated = when (upgradeType) {
                    "engine" -> cust.copy(engineStage = cust.engineStage + 1)
                    "brake" -> cust.copy(brakeStage = cust.brakeStage + 1)
                    "suspension" -> cust.copy(suspensionStage = cust.suspensionStage + 1)
                    "nitro" -> cust.copy(nitroStage = cust.nitroStage + 1)
                    else -> cust
                }
                repository.saveCustomization(updated)
            }
        }
    }

    private fun stopGameLoop() {
        audioEngine.isEngineRunning = false
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        audioEngine.stop()
    }
}
