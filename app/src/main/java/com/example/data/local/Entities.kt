package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 5000,
    val currentCarId: String = "apex_gt",
    val totalRaces: Int = 0,
    val totalWins: Int = 0,
    val totalDistanceKm: Float = 0f,
    val nearMissCount: Int = 0
)

@Entity(tableName = "car_customization")
data class CarCustomizationEntity(
    @PrimaryKey val carId: String,
    val colorHex: String,
    val finishName: String = "GLOSS",
    val rimStyleName: String = "SPORT",
    val rimColorHex: String = "#CCCCCC",
    val spoilerStyleName: String = "LIP",
    val bumperKitName: String = "STOCK",
    val exhaustTipName: String = "STOCK",
    val engineStage: Int = 1,
    val brakeStage: Int = 1,
    val suspensionStage: Int = 1,
    val nitroStage: Int = 1,
    val isUnlocked: Boolean = false
)

@Entity(tableName = "race_records")
data class RaceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val modeName: String,
    val trackName: String,
    val bestTimeMs: Long,
    val stars: Int = 3,
    val dateTimestamp: Long = System.currentTimeMillis()
)
