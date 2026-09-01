package com.dawnanddusk

import com.dawnanddusk.core.GeoUtils
import com.dawnanddusk.core.SecurityUtils
import com.dawnanddusk.data.local.catalog.StaticCreatureCatalog
import com.dawnanddusk.domain.engine.CaptureEngine
import com.dawnanddusk.domain.engine.SpawnConfig
import com.dawnanddusk.domain.engine.SpawnEngine
import com.dawnanddusk.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class GameUnitTests {

    @Test
    fun testGeoDistanceAndBoundingBox() {
        val lat = 35.6895
        val lon = 139.6917
        val radius = 300.0 // 300 meters

        val bounds = GeoUtils.buildBoundingBox(lat, lon, radius)
        assertTrue(bounds.maxLat > bounds.minLat)
        assertTrue(bounds.maxLon > bounds.minLon)

        // Point at center should have distance 0
        val distCenter = GeoUtils.calculateDistanceMeters(lat, lon, lat, lon)
        assertEquals(0.0, distCenter, 0.001)

        // Point within 100m should be within radius
        val dLat = GeoUtils.metersToLatitudeDegrees(100.0)
        val pointWithin = lat + dLat
        val distWithin = GeoUtils.calculateDistanceMeters(lat, lon, pointWithin, lon)
        assertEquals(100.0, distWithin, 1.0)
        assertTrue(GeoUtils.isWithinRadius(lat, lon, pointWithin, lon, 300.0))

        // Point at 500m should NOT be within 300m radius
        val pointOutside = lat + GeoUtils.metersToLatitudeDegrees(500.0)
        assertFalse(GeoUtils.isWithinRadius(lat, lon, pointOutside, lon, 300.0))
    }

    @Test
    fun testSecurityPasswordHashingAndValidation() {
        val password = "SuperSecretPassword123"
        val salt = SecurityUtils.generateSalt()
        assertEquals(16, salt.length)

        val hash = SecurityUtils.hashPassword(password, salt)
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())

        // Same password and salt should verify
        assertTrue(SecurityUtils.verifyPassword(password, salt, hash))

        // Wrong password should fail
        assertFalse(SecurityUtils.verifyPassword("WrongPassword", salt, hash))

        // Username validation
        assertTrue(SecurityUtils.isValidUsername("ash_ketchum"))
        assertTrue(SecurityUtils.isValidUsername("Red123"))
        assertFalse(SecurityUtils.isValidUsername("a")) // Too short
        assertFalse(SecurityUtils.isValidUsername("invalid user!")) // Special chars
    }

    @Test
    fun testStaticCatalog151Creatures() {
        val creatures = StaticCreatureCatalog.allCreatures
        assertEquals(151, creatures.size)

        // Verify first and last
        assertEquals("Bulbasaur", creatures.first().name)
        assertEquals(1, creatures.first().id)
        assertEquals("Mew", creatures.last().name)
        assertEquals(151, creatures.last().id)

        // Verify types and stats are populated
        creatures.forEach { c ->
            assertTrue(c.id in 1..151)
            assertTrue(c.name.isNotBlank())
            assertTrue(c.primaryType.isNotBlank())
            assertTrue(c.rarityCode in listOf("C", "I", "R", "L"))
            assertTrue(c.hp > 0)
            assertTrue(c.baseCaptureRate in 0.0..1.0)
            assertTrue(c.fleeRate in 0.0..1.0)
        }
    }

    @Test
    fun testSpawnEngineGeneratesValidSpawns() {
        val engine = SpawnEngine(SpawnConfig(spawnRadiusMeters = 300.0, minSpawns = 5, maxSpawns = 8))
        val allCreatures = StaticCreatureCatalog.allCreatures.map { StaticCreatureCatalog.toDomain(it) }

        val playerLat = 40.7128
        val playerLon = -74.0060

        val spawns = engine.generateSpawns(playerLat, playerLon, allCreatures)
        assertTrue(spawns.size in 5..8)

        spawns.forEach { spawn ->
            assertNotNull(spawn.creature)
            val dist = GeoUtils.calculateDistanceMeters(playerLat, playerLon, spawn.latitude, spawn.longitude)
            assertTrue("Spawn distance $dist exceeds 300m radius", dist <= 300.0 + 1.0)
            assertFalse(spawn.isExpired())
        }
    }

    @Test
    fun testCaptureEngineOutcomes() {
        val captureEngine = CaptureEngine()
        val allCreatures = StaticCreatureCatalog.allCreatures.map { StaticCreatureCatalog.toDomain(it) }
        val bulbasaur = allCreatures.first { it.name == "Bulbasaur" }

        // Test Miss
        val missedThrow = ThrowResult(
            velocityX = 0.1f,
            velocityY = 0.1f,
            durationMs = 200L,
            hitAccuracy = 0.05f, // Missed target
            timingBonus = 0.5f
        )
        val missResolution = captureEngine.resolveThrow(bulbasaur, missedThrow)
        assertEquals(CaptureOutcome.MISSED, missResolution.outcome)

        // Test Perfect Throw
        val perfectThrow = ThrowResult(
            velocityX = 0.8f,
            velocityY = 0.8f,
            durationMs = 150L,
            hitAccuracy = 1.0f, // Bullseye
            timingBonus = 1.0f
        )
        val res = captureEngine.resolveThrow(bulbasaur, perfectThrow)
        assertTrue(res.outcome in listOf(CaptureOutcome.CAPTURED, CaptureOutcome.BROKE_FREE, CaptureOutcome.ESCAPED))
    }
}
