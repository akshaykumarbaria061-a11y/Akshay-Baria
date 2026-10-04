package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.CountdownBanner
import com.example.ui.components.DrivingControls
import com.example.ui.components.GameOverDialog
import com.example.ui.components.MinimapHUD
import com.example.ui.components.PauseDialog
import com.example.ui.components.RaceStatusOverlay
import com.example.ui.components.SpeedometerHUD
import com.example.ui.components.WeatherSelectionDialog
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel
) {
    BackHandler {
        viewModel.togglePause()
    }

    val speedKmh by viewModel.speedKmh.collectAsState()
    val rpm by viewModel.rpm.collectAsState()
    val currentGear by viewModel.currentGear.collectAsState()
    val nitroPercent by viewModel.nitroPercent.collectAsState()
    val healthPercent by viewModel.healthPercent.collectAsState()
    val isNitroActive by viewModel.isNitroActive.collectAsState()
    val currentZone by viewModel.currentZone.collectAsState()
    val raceState by viewModel.raceState.collectAsState()

    val isPaused by viewModel.isPaused.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val isWrecked by viewModel.isWrecked.collectAsState()
    val isWeatherDialogOpen by viewModel.isWeatherDialogOpen.collectAsState()
    val isAudioMuted by viewModel.isAudioMuted.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 3D OPEN WORLD REAL-TIME CANVAS
        Canvas(modifier = Modifier.fillMaxSize()) {
            viewModel.rasterizer.render(
                drawScope = this,
                camera = viewModel.camera,
                car = viewModel.carPhysics,
                worldMap = viewModel.worldMap,
                trafficSystem = viewModel.trafficSystem,
                weatherSystem = viewModel.weatherSystem
            )
        }

        // 2. MINIMAP & ZONE RADAR (Top Left)
        MinimapHUD(
            car = viewModel.carPhysics,
            worldMap = viewModel.worldMap,
            trafficSystem = viewModel.trafficSystem,
            currentZone = currentZone,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp)
        )

        // 3. RACE STATUS OVERLAY (Top Right)
        RaceStatusOverlay(
            raceState = raceState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 16.dp)
        )

        // 4. SPEEDOMETER & TACHOMETER (Bottom Center / Left)
        SpeedometerHUD(
            speedKmh = speedKmh,
            rpm = rpm,
            maxRpm = viewModel.carPhysics.maxRpm,
            currentGear = currentGear,
            nitroPercent = nitroPercent,
            healthPercent = healthPercent,
            isNitroActive = isNitroActive,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )

        // 5. DRIVING CONTROLS (Steering, Gas, Brake, Drift, Nitro, Cameras)
        DrivingControls(
            onSteerLeft = { pressed ->
                viewModel.inputSteer = if (pressed) -1f else (if (viewModel.inputSteer < 0) 0f else viewModel.inputSteer)
            },
            onSteerRight = { pressed ->
                viewModel.inputSteer = if (pressed) 1f else (if (viewModel.inputSteer > 0) 0f else viewModel.inputSteer)
            },
            onThrottle = { pressed ->
                viewModel.inputThrottle = if (pressed) 1f else 0f
            },
            onBrake = { pressed ->
                viewModel.inputBrake = if (pressed) 1f else 0f
            },
            onHandbrake = { pressed ->
                viewModel.inputHandbrake = pressed
            },
            onNitro = { pressed ->
                viewModel.inputNitro = pressed
            },
            onToggleCamera = {
                viewModel.toggleCamera()
            },
            onToggleHeadlights = {
                viewModel.toggleHeadlights()
            },
            onOpenWeatherDialog = {
                viewModel.isWeatherDialogOpen.value = true
            },
            onPause = {
                viewModel.togglePause()
            }
        )

        // 6. COUNTDOWN BANNER (3.. 2.. 1.. GO!)
        CountdownBanner(
            countdownValue = raceState.countdownValue,
            visible = raceState.isCountdown,
            modifier = Modifier.align(Alignment.Center)
        )

        // 7. WEATHER & TIME SELECTION DIALOG
        if (isWeatherDialogOpen) {
            WeatherSelectionDialog(
                currentWeather = viewModel.weatherSystem.weather,
                currentTime = viewModel.weatherSystem.timeOfDay,
                onSelectWeather = { viewModel.setWeather(it) },
                onSelectTime = { viewModel.setTimeOfDay(it) },
                onDismiss = { viewModel.isWeatherDialogOpen.value = false }
            )
        }

        // 8. PAUSE MENU
        if (isPaused && !isGameOver) {
            PauseDialog(
                isAudioMuted = isAudioMuted,
                onResume = { viewModel.isPaused.value = false },
                onRestart = { viewModel.startGame(raceState.mode) },
                onToggleAudio = { viewModel.toggleAudio() },
                onExitMenu = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
            )
        }

        // 9. GAME OVER / VICTORY DIALOG
        if (isGameOver) {
            GameOverDialog(
                raceState = raceState,
                isWrecked = isWrecked,
                onRestart = { viewModel.startGame(raceState.mode) },
                onOpenGarage = { viewModel.navigateTo(AppScreen.GARAGE) },
                onMainMenu = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
            )
        }
    }
}
