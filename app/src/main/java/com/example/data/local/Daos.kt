package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PlayerProfileEntity)

    @Query("UPDATE player_profile SET coins = :newCoins WHERE id = 1")
    suspend fun updateCoins(newCoins: Int)

    @Query("UPDATE player_profile SET currentCarId = :carId WHERE id = 1")
    suspend fun updateCurrentCar(carId: String)
}

@Dao
interface CarCustomizationDao {
    @Query("SELECT * FROM car_customization")
    fun getAllCustomizationsFlow(): Flow<List<CarCustomizationEntity>>

    @Query("SELECT * FROM car_customization WHERE carId = :carId LIMIT 1")
    suspend fun getCustomization(carId: String): CarCustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(customization: CarCustomizationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(customizations: List<CarCustomizationEntity>)
}

@Dao
interface RaceRecordDao {
    @Query("SELECT * FROM race_records ORDER BY bestTimeMs ASC")
    fun getAllRecordsFlow(): Flow<List<RaceRecordEntity>>

    @Query("SELECT * FROM race_records WHERE modeName = :mode ORDER BY bestTimeMs ASC LIMIT 5")
    fun getTopRecordsForMode(mode: String): Flow<List<RaceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RaceRecordEntity)
}
