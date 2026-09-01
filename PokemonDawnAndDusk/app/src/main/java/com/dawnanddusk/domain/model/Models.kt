package com.dawnanddusk.domain.model

data class Creature(
    val id: Int,
    val name: String,
    val category: String,
    val primaryType: CreatureType,
    val secondaryType: CreatureType? = null,
    val rarity: Rarity,
    val heightMeters: Double,
    val weightKg: Double,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val baseCaptureRate: Double,
    val fleeRate: Double,
    val spriteUrl: String,
    val description: String
)

data class Player(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarGender: String, // "M" or "F"
    val level: Int = 1,
    val xp: Long = 0L,
    val totalCaptures: Int = 0,
    val uniqueCreaturesCaptured: Int = 0,
    val lastLatitude: Double = 0.0,
    val lastLongitude: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Spawn(
    val id: String,
    val creatureId: Int,
    val latitude: Double,
    val longitude: Double,
    val spawnedAt: Long,
    val expiresAt: Long,
    val isCaptured: Boolean = false,
    val creature: Creature? = null
) {
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime >= expiresAt || isCaptured
    }
}

data class Capture(
    val id: Long = 0,
    val playerId: Long,
    val creatureId: Int,
    val latitude: Double,
    val longitude: Double,
    val capturedAt: Long = System.currentTimeMillis(),
    val accuracyBonus: Double = 0.0,
    val creature: Creature? = null
)

data class Encounter(
    val id: String,
    val spawn: Spawn,
    val creature: Creature,
    val startedAt: Long = System.currentTimeMillis(),
    var attemptsLeft: Int = 5,
    var isFinished: Boolean = false
)

data class ThrowResult(
    val velocityX: Float,
    val velocityY: Float,
    val durationMs: Long,
    val hitAccuracy: Float, // 0.0 to 1.0 (closeness to target bullseye)
    val timingBonus: Float  // 0.0 to 1.0 (ring contraction timing)
)

enum class CaptureOutcome {
    CAPTURED,
    BROKE_FREE,
    ESCAPED,
    MISSED
}

data class CaptureResolution(
    val outcome: CaptureOutcome,
    val captureChance: Double,
    val message: String
)
