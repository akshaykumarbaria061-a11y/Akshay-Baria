package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CarCustomization
import com.example.data.model.CarSpecs
import com.example.data.model.GameMode
import com.example.engine.physics.CarPhysics
import com.example.engine.world.RoadZone
import com.example.engine.world.WorldMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Apex Drive", appName)
    }

    @Test
    fun testWorldMapZonesAndCheckpoints() {
        val worldMap = WorldMap()
        assertTrue("World map should contain road points", worldMap.roadPoints.size > 1000)
        assertTrue("World map should contain checkpoints", worldMap.checkpoints.size >= 8)

        // Verify key road zones exist
        val zones = worldMap.roadPoints.map { it.zone }.toSet()
        assertTrue(zones.contains(RoadZone.CITY))
        assertTrue(zones.contains(RoadZone.HIGHWAY))
        assertTrue(zones.contains(RoadZone.MOUNTAIN_HAIRPIN))
        assertTrue(zones.contains(RoadZone.SNOW_SUMMIT))
        assertTrue(zones.contains(RoadZone.MOUNTAIN_TUNNEL))
        assertTrue(zones.contains(RoadZone.VILLAGE))
        assertTrue(zones.contains(RoadZone.FOREST))
        assertTrue(zones.contains(RoadZone.DESERT))
        assertTrue(zones.contains(RoadZone.SUSPENSION_BRIDGE))
        assertTrue(zones.contains(RoadZone.COASTAL))
    }

    @Test
    fun testCarPhysicsAccelerationAndSteering() {
        val spec = CarSpecs.ALL_CARS[0]
        val cust = CarCustomization(spec.id, spec.defaultColorHex)
        val physics = CarPhysics(spec, cust)
        val worldMap = WorldMap()

        assertEquals(0f, physics.speedKmh, 0.01f)

        // Accelerate for 10 frames
        for (i in 0 until 10) {
            physics.step(
                dt = 0.033f,
                throttleInput = 1.0f,
                brakeInput = 0.0f,
                steerInput = 0.5f,
                handbrakeInput = false,
                nitroInput = false,
                worldMap = worldMap
            )
        }

        assertTrue("Car should accelerate with throttle", physics.speedKmh > 0f)
        assertTrue("Steering angle should be positive when steering right", physics.steeringAngleRad > 0f)
    }

    @Test
    fun testCarCustomizationUpgrades() {
        val spec = CarSpecs.ALL_CARS[0]
        val cust = CarCustomization(
            carId = spec.id,
            colorHex = "#FF1E40",
            engineStage = 3,
            nitroStage = 2
        )

        val boostedSpeed = cust.calculateEffectiveTopSpeed(spec.baseTopSpeedKmh)
        assertTrue("Boosted top speed should be greater than base", boostedSpeed > spec.baseTopSpeedKmh)
    }
}
