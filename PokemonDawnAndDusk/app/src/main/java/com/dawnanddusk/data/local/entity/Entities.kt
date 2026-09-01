package com.dawnanddusk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [Index(value = ["username"], unique = true)]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val displayName: String,
    val avatarGender: String, // "M" or "F"
    val level: Int = 1,
    val xp: Long = 0L,
    val lastLatitude: Double = 0.0,
    val lastLongitude: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "creatures")
data class CreatureEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val category: String,
    val primaryType: String,
    val secondaryType: String?,
    val rarityCode: String,
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

@Entity(
    tableName = "captures",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CreatureEntity::class,
            parentColumns = ["id"],
            childColumns = ["creatureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playerId"), Index("creatureId")]
)
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerId: Long,
    val creatureId: Int,
    val latitude: Double,
    val longitude: Double,
    val capturedAt: Long = System.currentTimeMillis(),
    val accuracyBonus: Double = 0.0
)

@Entity(tableName = "spawns")
data class SpawnEntity(
    @PrimaryKey
    val id: String,
    val creatureId: Int,
    val latitude: Double,
    val longitude: Double,
    val spawnedAt: Long,
    val expiresAt: Long,
    val isCaptured: Boolean = false
)

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playerId")]
)
data class SessionEntity(
    @PrimaryKey
    val sessionId: String,
    val playerId: Long,
    val loginAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
