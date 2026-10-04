package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RacingDarkBg
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RacingDarkBg
                ) {
                    val gameViewModel: GameViewModel = viewModel()
                    MainAppNavigation(viewModel = gameViewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val customizations by viewModel.customizations.collectAsState()
    val records by viewModel.records.collectAsState()

    when (currentScreen) {
        AppScreen.MAIN_MENU -> {
            MainMenuScreen(
                playerCoins = profile.coins,
                currentCarId = profile.currentCarId,
                onStartGame = { mode ->
                    viewModel.startGame(mode)
                },
                onOpenGarage = {
                    viewModel.navigateTo(AppScreen.GARAGE)
                },
                onOpenLeaderboard = {
                    viewModel.navigateTo(AppScreen.LEADERBOARD)
                }
            )
        }
        AppScreen.RACING_GAME -> {
            GameScreen(viewModel = viewModel)
        }
        AppScreen.GARAGE -> {
            BackHandler {
                viewModel.navigateTo(AppScreen.MAIN_MENU)
            }
            GarageScreen(
                currentCarId = profile.currentCarId,
                playerCoins = profile.coins,
                customizations = customizations,
                onSelectCar = { carId ->
                    viewModel.selectCar(carId)
                },
                onPurchaseCar = { spec ->
                    viewModel.purchaseCar(spec)
                },
                onUpdateCustomization = { cust ->
                    viewModel.updateCustomization(cust)
                },
                onPurchaseUpgrade = { cust, upgradeType, cost ->
                    viewModel.purchaseUpgrade(cust, upgradeType, cost)
                },
                onBack = {
                    viewModel.navigateTo(AppScreen.MAIN_MENU)
                }
            )
        }
        AppScreen.LEADERBOARD -> {
            BackHandler {
                viewModel.navigateTo(AppScreen.MAIN_MENU)
            }
            LeaderboardScreen(
                profile = profile,
                records = records,
                onBack = {
                    viewModel.navigateTo(AppScreen.MAIN_MENU)
                }
            )
        }
    }
}
