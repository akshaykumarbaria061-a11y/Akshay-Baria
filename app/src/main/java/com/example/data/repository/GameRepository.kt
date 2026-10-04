package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.CarCustomizationEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.RaceRecordEntity
import com.example.data.model.CarCustomization
import com.example.data.model.CarPaintFinish
import com.example.data.model.CarSpecs
import com.example.data.model.RimStyle
import com.example.data.model.SpoilerStyle
import com.example.data.model.BumperKit
import com.example.data.model.ExhaustTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepository(private val database: AppDatabase) {

    private val profileDao = database.playerProfileDao()
    private val customizationDao = database.carCustomizationDao()
    private val recordDao = database.raceRecordDao()

    val profileFlow: Flow<PlayerProfileEntity> = profileDao.getProfileFlow().map {
        it ?: PlayerProfileEntity()
    }

    val customizationsFlow: Flow<Map<String, CarCustomization>> = customizationDao.getAllCustomizationsFlow().map { list ->
        list.associate { entity ->
            entity.carId to CarCustomization(
                carId = entity.carId,
                colorHex = entity.colorHex,
                finish = runCatching { CarPaintFinish.valueOf(entity.finishName) }.getOrDefault(CarPaintFinish.GLOSS),
                rimStyle = runCatching { RimStyle.valueOf(entity.rimStyleName) }.getOrDefault(RimStyle.SPORT),
                rimColorHex = entity.rimColorHex,
                spoilerStyle = runCatching { SpoilerStyle.valueOf(entity.spoilerStyleName) }.getOrDefault(SpoilerStyle.LIP),
                bumperKit = runCatching { BumperKit.valueOf(entity.bumperKitName) }.getOrDefault(BumperKit.STOCK),
                exhaustTip = runCatching { ExhaustTip.valueOf(entity.exhaustTipName) }.getOrDefault(ExhaustTip.STOCK),
                engineStage = entity.engineStage,
                brakeStage = entity.brakeStage,
                suspensionStage = entity.suspensionStage,
                nitroStage = entity.nitroStage
            )
        }
    }

    val recordsFlow: Flow<List<RaceRecordEntity>> = recordDao.getAllRecordsFlow()

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        val existingProfile = profileDao.getProfile()
        if (existingProfile == null) {
            profileDao.insertOrUpdateProfile(
                PlayerProfileEntity(
                    id = 1,
                    coins = 12500, // Starter bonus cash
                    currentCarId = "apex_gt",
                    totalRaces = 0,
                    totalWins = 0,
                    totalDistanceKm = 0f
                )
            )
        }

        // Initialize customizations for each car if not present
        val defaults = CarSpecs.ALL_CARS.map { car ->
            CarCustomizationEntity(
                carId = car.id,
                colorHex = car.defaultColorHex,
                finishName = "GLOSS",
                rimStyleName = "SPORT",
                rimColorHex = "#CCCCCC",
                spoilerStyleName = if (car.id == "apex_gt" || car.id == "veloce_v12") "GT_WING" else "LIP",
                bumperKitName = "STOCK",
                exhaustTipName = if (car.id == "veloce_v12") "QUAD" else "STOCK",
                engineStage = 1,
                brakeStage = 1,
                suspensionStage = 1,
                nitroStage = 1,
                isUnlocked = car.unlockedByDefault
            )
        }
        customizationDao.insertAll(defaults)
    }

    suspend fun saveCustomization(customization: CarCustomization, isUnlocked: Boolean = true) = withContext(Dispatchers.IO) {
        customizationDao.insertOrUpdate(
            CarCustomizationEntity(
                carId = customization.carId,
                colorHex = customization.colorHex,
                finishName = customization.finish.name,
                rimStyleName = customization.rimStyle.name,
                rimColorHex = customization.rimColorHex,
                spoilerStyleName = customization.spoilerStyle.name,
                bumperKitName = customization.bumperKit.name,
                exhaustTipName = customization.exhaustTip.name,
                engineStage = customization.engineStage,
                brakeStage = customization.brakeStage,
                suspensionStage = customization.suspensionStage,
                nitroStage = customization.nitroStage,
                isUnlocked = isUnlocked
            )
        )
    }

    suspend fun updateCurrentCar(carId: String) = withContext(Dispatchers.IO) {
        profileDao.updateCurrentCar(carId)
    }

    suspend fun addCoins(amount: Int) = withContext(Dispatchers.IO) {
        val profile = profileDao.getProfile() ?: PlayerProfileEntity()
        val updated = (profile.coins + amount).coerceAtLeast(0)
        profileDao.updateCoins(updated)
    }

    suspend fun spendCoins(amount: Int): Boolean = withContext(Dispatchers.IO) {
        val profile = profileDao.getProfile() ?: PlayerProfileEntity()
        if (profile.coins >= amount) {
            profileDao.updateCoins(profile.coins - amount)
            true
        } else {
            false
        }
    }

    suspend fun recordRaceFinished(
        modeName: String,
        trackName: String,
        timeMs: Long,
        isWin: Boolean,
        distanceKm: Float,
        rewardCoins: Int
    ) = withContext(Dispatchers.IO) {
        val profile = profileDao.getProfile() ?: PlayerProfileEntity()
        profileDao.insertOrUpdateProfile(
            profile.copy(
                coins = profile.coins + rewardCoins,
                totalRaces = profile.totalRaces + 1,
                totalWins = profile.totalWins + (if (isWin) 1 else 0),
                totalDistanceKm = profile.totalDistanceKm + distanceKm
            )
        )
        recordDao.insertRecord(
            RaceRecordEntity(
                modeName = modeName,
                trackName = trackName,
                bestTimeMs = timeMs,
                stars = if (isWin) 3 else 2
            )
        )
    }
}
