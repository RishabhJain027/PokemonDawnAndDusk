package com.dawnanddusk.domain.engine

import com.dawnanddusk.core.GeoBounds
import com.dawnanddusk.core.GeoPoint
import com.dawnanddusk.core.GeoUtils
import com.dawnanddusk.domain.model.*
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class SpawnConfig(
    val spawnRadiusMeters: Double = 300.0,
    val interactionRadiusMeters: Double = 60.0,
    val minSpawns: Int = 5,
    val maxSpawns: Int = 10,
    val minDistanceBetweenSpawnsMeters: Double = 25.0,
    val spawnTtlMillis: Long = 5 * 60 * 1000L // 5 minutes
)

class SpawnEngine(private val config: SpawnConfig = SpawnConfig()) {

    /**
     * Generates a batch of wild Pokémon spawns around the player's current GPS location.
     */
    fun generateSpawns(
        playerLatitude: Double,
        playerLongitude: Double,
        allCreatures: List<Creature>,
        existingActiveSpawns: List<Spawn> = emptyList()
    ): List<Spawn> {
        if (allCreatures.isEmpty()) return emptyList()

        val bounds = GeoUtils.buildBoundingBox(
            playerLatitude,
            playerLongitude,
            config.spawnRadiusMeters
        )

        val targetCount = Random.nextInt(config.minSpawns, config.maxSpawns + 1)
        val resultSpawns = mutableListOf<Spawn>()
        val existingPositions = existingActiveSpawns.map { GeoPoint(it.latitude, it.longitude) }.toMutableList()

        val commonPool = allCreatures.filter { it.rarity == Rarity.COMMON }
        val uncommonPool = allCreatures.filter { it.rarity == Rarity.UNCOMMON }
        val rarePool = allCreatures.filter { it.rarity == Rarity.RARE }
        val legendaryPool = allCreatures.filter { it.rarity == Rarity.LEGENDARY }

        var attempts = 0
        val maxAttempts = targetCount * 15

        while (resultSpawns.size < targetCount && attempts < maxAttempts) {
            attempts++

            val candidatePoint = GeoUtils.randomPointInBounds(bounds)

            // Check distance from player: must be within spawn radius
            val distFromPlayer = GeoUtils.calculateDistanceMeters(
                playerLatitude, playerLongitude,
                candidatePoint.latitude, candidatePoint.longitude
            )
            if (distFromPlayer > config.spawnRadiusMeters) continue

            // Check minimum distance from other spawns to prevent overlapping
            val tooClose = existingPositions.any {
                GeoUtils.calculateDistanceMeters(
                    candidatePoint.latitude, candidatePoint.longitude,
                    it.latitude, it.longitude
                ) < config.minDistanceBetweenSpawnsMeters
            }
            if (tooClose) continue

            // Weighted Rarity Selection: Common 70%, Uncommon 20%, Rare 9%, Legendary 1%
            val roll = Random.nextDouble(100.0)
            val selectedPool = when {
                roll < 70.0 -> commonPool.ifEmpty { allCreatures }
                roll < 90.0 -> uncommonPool.ifEmpty { commonPool }
                roll < 99.0 -> rarePool.ifEmpty { uncommonPool }
                else -> legendaryPool.ifEmpty { rarePool }
            }

            val creature = selectedPool.random()
            val now = System.currentTimeMillis()
            val spawn = Spawn(
                id = UUID.randomUUID().toString(),
                creatureId = creature.id,
                latitude = candidatePoint.latitude,
                longitude = candidatePoint.longitude,
                spawnedAt = now,
                expiresAt = now + config.spawnTtlMillis,
                isCaptured = false,
                creature = creature
            )

            resultSpawns.add(spawn)
            existingPositions.add(candidatePoint)
        }

        return resultSpawns
    }

    /**
     * Validates whether a player is close enough to interact with a spawn marker.
     */
    fun canInteractWithSpawn(
        playerLatitude: Double,
        playerLongitude: Double,
        spawn: Spawn
    ): Boolean {
        if (spawn.isExpired()) return false
        val distance = GeoUtils.calculateDistanceMeters(
            playerLatitude, playerLongitude,
            spawn.latitude, spawn.longitude
        )
        return distance <= config.interactionRadiusMeters
    }
}

class CaptureEngine {

    /**
     * Resolves capture attempt based on creature base catch rate, throw accuracy, power, and timing.
     */
    fun resolveThrow(
        creature: Creature,
        throwResult: ThrowResult
    ): CaptureResolution {
        // If throw completely missed the target area (accuracy < 0.15)
        if (throwResult.hitAccuracy < 0.15f) {
            return CaptureResolution(
                outcome = CaptureOutcome.MISSED,
                captureChance = 0.0,
                message = "You missed the target!"
            )
        }

        // Accuracy score bonus: up to +0.25 for a bullseye
        val accuracyBonus = (throwResult.hitAccuracy * 0.25).toDouble()

        // Timing bonus: up to +0.15 for hitting small ring
        val timingBonus = (throwResult.timingBonus * 0.15).toDouble()

        // Base catch rate weighted calculation
        val baseRate = creature.baseCaptureRate
        val totalChance = min(0.95, max(0.05, (baseRate * 0.65) + accuracyBonus + timingBonus))

        val roll = Random.nextDouble(1.0)
        return if (roll <= totalChance) {
            CaptureResolution(
                outcome = CaptureOutcome.CAPTURED,
                captureChance = totalChance,
                message = "Gotcha! ${creature.name} was caught!"
            )
        } else {
            // Check if creature flees or breaks free
            val fleeRoll = Random.nextDouble(1.0)
            if (fleeRoll <= creature.fleeRate) {
                CaptureResolution(
                    outcome = CaptureOutcome.ESCAPED,
                    captureChance = totalChance,
                    message = "Oh no! ${creature.name} ran away!"
                )
            } else {
                CaptureResolution(
                    outcome = CaptureOutcome.BROKE_FREE,
                    captureChance = totalChance,
                    message = "Aargh! Almost had it! It broke free!"
                )
            }
        }
    }
}
