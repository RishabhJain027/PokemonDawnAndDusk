package com.dawnanddusk.data.repository

import com.dawnanddusk.core.AppResult
import com.dawnanddusk.core.SecurityUtils
import com.dawnanddusk.data.local.catalog.StaticCreatureCatalog
import com.dawnanddusk.data.local.dao.*
import com.dawnanddusk.data.local.entity.*
import com.dawnanddusk.domain.engine.SpawnEngine
import com.dawnanddusk.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

interface AuthRepository {
    suspend fun login(username: String, password: String): AppResult<Player>
    suspend fun register(username: String, password: String, displayName: String, avatarGender: String): AppResult<Player>
    suspend fun getPlayer(playerId: Long): Player?
    suspend fun updateLocation(playerId: Long, latitude: Double, longitude: Double)
}

class AuthRepositoryImpl(
    private val playerDao: PlayerDao,
    private val sessionDao: SessionDao
) : AuthRepository {

    override suspend fun login(username: String, password: String): AppResult<Player> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val playerEntity = playerDao.getPlayerByUsername(cleanUser)
            ?: return@withContext AppResult.Error("Invalid username or password")

        val matches = SecurityUtils.verifyPassword(password, playerEntity.passwordSalt, playerEntity.passwordHash)
        if (!matches) {
            return@withContext AppResult.Error("Invalid username or password")
        }

        // Save active session
        sessionDao.clearActiveSessions()
        sessionDao.saveSession(
            SessionEntity(
                sessionId = UUID.randomUUID().toString(),
                playerId = playerEntity.id,
                loginAt = System.currentTimeMillis(),
                isActive = true
            )
        )

        AppResult.Success(mapPlayer(playerEntity))
    }

    override suspend fun register(
        username: String,
        password: String,
        displayName: String,
        avatarGender: String
    ): AppResult<Player> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val cleanName = displayName.trim()

        if (!SecurityUtils.isValidUsername(cleanUser)) {
            return@withContext AppResult.Error("Username must be 3-20 characters (letters, numbers, underscores)")
        }
        if (!SecurityUtils.isValidPassword(password)) {
            return@withContext AppResult.Error("Password must be at least 6 characters")
        }
        if (cleanName.isBlank()) {
            return@withContext AppResult.Error("Trainer name cannot be empty")
        }

        val count = playerDao.usernameExists(cleanUser)
        if (count > 0) {
            return@withContext AppResult.Error("Username '$cleanUser' is already taken")
        }

        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(password, salt)

        val newEntity = PlayerEntity(
            username = cleanUser,
            passwordHash = hash,
            passwordSalt = salt,
            displayName = cleanName,
            avatarGender = if (avatarGender.equals("F", ignoreCase = true)) "F" else "M"
        )

        val newId = playerDao.insertPlayer(newEntity)
        val savedPlayer = playerDao.getPlayerById(newId)
            ?: return@withContext AppResult.Error("Failed to create player")

        // Save session
        sessionDao.clearActiveSessions()
        sessionDao.saveSession(
            SessionEntity(
                sessionId = UUID.randomUUID().toString(),
                playerId = newId,
                loginAt = System.currentTimeMillis(),
                isActive = true
            )
        )

        AppResult.Success(mapPlayer(savedPlayer))
    }

    override suspend fun getPlayer(playerId: Long): Player? = withContext(Dispatchers.IO) {
        playerDao.getPlayerById(playerId)?.let { mapPlayer(it) }
    }

    override suspend fun updateLocation(playerId: Long, latitude: Double, longitude: Double) = withContext(Dispatchers.IO) {
        val player = playerDao.getPlayerById(playerId) ?: return@withContext
        playerDao.updatePlayer(player.copy(lastLatitude = latitude, lastLongitude = longitude))
    }

    private fun mapPlayer(entity: PlayerEntity): Player {
        return Player(
            id = entity.id,
            username = entity.username,
            displayName = entity.displayName,
            avatarGender = entity.avatarGender,
            level = entity.level,
            xp = entity.xp,
            lastLatitude = entity.lastLatitude,
            lastLongitude = entity.lastLongitude,
            createdAt = entity.createdAt
        )
    }
}

interface CreatureRepository {
    suspend fun getAllCreatures(): List<Creature>
    fun getAllCreaturesFlow(): Flow<List<Creature>>
    suspend fun getCreatureById(id: Int): Creature?
    suspend fun ensureCatalogLoaded()
}

class CreatureRepositoryImpl(
    private val creatureDao: CreatureDao
) : CreatureRepository {

    override suspend fun getAllCreatures(): List<Creature> = withContext(Dispatchers.IO) {
        ensureCatalogLoaded()
        creatureDao.getAllCreatures().map { StaticCreatureCatalog.toDomain(it) }
    }

    override fun getAllCreaturesFlow(): Flow<List<Creature>> {
        return creatureDao.getAllCreaturesFlow().map { list ->
            list.map { StaticCreatureCatalog.toDomain(it) }
        }
    }

    override suspend fun getCreatureById(id: Int): Creature? = withContext(Dispatchers.IO) {
        ensureCatalogLoaded()
        creatureDao.getCreatureById(id)?.let { StaticCreatureCatalog.toDomain(it) }
    }

    override suspend fun ensureCatalogLoaded() = withContext(Dispatchers.IO) {
        val count = creatureDao.getCreatureCount()
        if (count == 0) {
            creatureDao.insertAll(StaticCreatureCatalog.allCreatures)
        }
    }
}

interface SpawnRepository {
    suspend fun getActiveSpawns(): List<Spawn>
    fun getActiveSpawnsFlow(): Flow<List<Spawn>>
    suspend fun refreshSpawns(latitude: Double, longitude: Double, spawnEngine: SpawnEngine): List<Spawn>
    suspend fun markSpawnCaptured(spawnId: String)
    suspend fun getSpawnById(spawnId: String): Spawn?
}

class SpawnRepositoryImpl(
    private val spawnDao: SpawnDao,
    private val creatureRepository: CreatureRepository
) : SpawnRepository {

    override suspend fun getActiveSpawns(): List<Spawn> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        spawnDao.deleteExpiredSpawns(now)
        val entities = spawnDao.getActiveSpawns(now)
        val allCreatures = creatureRepository.getAllCreatures().associateBy { it.id }
        entities.map { entity ->
            Spawn(
                id = entity.id,
                creatureId = entity.creatureId,
                latitude = entity.latitude,
                longitude = entity.longitude,
                spawnedAt = entity.spawnedAt,
                expiresAt = entity.expiresAt,
                isCaptured = entity.isCaptured,
                creature = allCreatures[entity.creatureId]
            )
        }
    }

    override fun getActiveSpawnsFlow(): Flow<List<Spawn>> {
        val now = System.currentTimeMillis()
        return spawnDao.getActiveSpawnsFlow(now).map { entities ->
            entities.map { entity ->
                Spawn(
                    id = entity.id,
                    creatureId = entity.creatureId,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    spawnedAt = entity.spawnedAt,
                    expiresAt = entity.expiresAt,
                    isCaptured = entity.isCaptured
                )
            }
        }
    }

    override suspend fun refreshSpawns(
        latitude: Double,
        longitude: Double,
        spawnEngine: SpawnEngine
    ): List<Spawn> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        spawnDao.deleteExpiredSpawns(now)
        val currentActive = getActiveSpawns()

        if (currentActive.size < 4) {
            val allCreatures = creatureRepository.getAllCreatures()
            val newSpawns = spawnEngine.generateSpawns(latitude, longitude, allCreatures, currentActive)
            val spawnEntities = newSpawns.map {
                SpawnEntity(
                    id = it.id,
                    creatureId = it.creatureId,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    spawnedAt = it.spawnedAt,
                    expiresAt = it.expiresAt,
                    isCaptured = it.isCaptured
                )
            }
            spawnDao.insertSpawns(spawnEntities)
            return@withContext currentActive + newSpawns
        }

        currentActive
    }

    override suspend fun markSpawnCaptured(spawnId: String) = withContext(Dispatchers.IO) {
        spawnDao.markSpawnCaptured(spawnId)
    }

    override suspend fun getSpawnById(spawnId: String): Spawn? = withContext(Dispatchers.IO) {
        val entity = spawnDao.getSpawnById(spawnId) ?: return@withContext null
        val creature = creatureRepository.getCreatureById(entity.creatureId)
        Spawn(
            id = entity.id,
            creatureId = entity.creatureId,
            latitude = entity.latitude,
            longitude = entity.longitude,
            spawnedAt = entity.spawnedAt,
            expiresAt = entity.expiresAt,
            isCaptured = entity.isCaptured,
            creature = creature
        )
    }
}

interface CaptureRepository {
    suspend fun saveCapture(playerId: Long, creatureId: Int, latitude: Double, longitude: Double, accuracyBonus: Double): Long
    suspend fun getCapturesForPlayer(playerId: Long): List<Capture>
    fun getCapturesForPlayerFlow(playerId: Long): Flow<List<Capture>>
    suspend fun getCapturedCreatureIds(playerId: Long): Set<Int>
    fun getCapturedCreatureIdsFlow(playerId: Long): Flow<Set<Int>>
    suspend fun getStats(playerId: Long): PlayerStats
}

data class PlayerStats(
    val totalCaptures: Int,
    val uniqueSpecies: Int,
    val capturesByRarity: Map<Rarity, Int>
)

class CaptureRepositoryImpl(
    private val captureDao: CaptureDao,
    private val playerDao: PlayerDao,
    private val creatureRepository: CreatureRepository
) : CaptureRepository {

    override suspend fun saveCapture(
        playerId: Long,
        creatureId: Int,
        latitude: Double,
        longitude: Double,
        accuracyBonus: Double
    ): Long = withContext(Dispatchers.IO) {
        val entity = CaptureEntity(
            playerId = playerId,
            creatureId = creatureId,
            latitude = latitude,
            longitude = longitude,
            capturedAt = System.currentTimeMillis(),
            accuracyBonus = accuracyBonus
        )
        val captureId = captureDao.insertCapture(entity)

        // Award XP and level up if applicable
        val player = playerDao.getPlayerById(playerId)
        if (player != null) {
            val newXp = player.xp + 100L
            val newLevel = 1 + (newXp / 500L).toInt()
            playerDao.updatePlayer(player.copy(xp = newXp, level = newLevel))
        }

        captureId
    }

    override suspend fun getCapturesForPlayer(playerId: Long): List<Capture> = withContext(Dispatchers.IO) {
        val entities = captureDao.getCapturesForPlayer(playerId)
        val creatures = creatureRepository.getAllCreatures().associateBy { it.id }
        entities.map { entity ->
            Capture(
                id = entity.id,
                playerId = entity.playerId,
                creatureId = entity.creatureId,
                latitude = entity.latitude,
                longitude = entity.longitude,
                capturedAt = entity.capturedAt,
                accuracyBonus = entity.accuracyBonus,
                creature = creatures[entity.creatureId]
            )
        }
    }

    override fun getCapturesForPlayerFlow(playerId: Long): Flow<List<Capture>> {
        return captureDao.getCapturesForPlayerFlow(playerId).map { list ->
            list.map { entity ->
                Capture(
                    id = entity.id,
                    playerId = entity.playerId,
                    creatureId = entity.creatureId,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    capturedAt = entity.capturedAt,
                    accuracyBonus = entity.accuracyBonus
                )
            }
        }
    }

    override suspend fun getCapturedCreatureIds(playerId: Long): Set<Int> = withContext(Dispatchers.IO) {
        captureDao.getCapturedCreatureIds(playerId).toSet()
    }

    override fun getCapturedCreatureIdsFlow(playerId: Long): Flow<Set<Int>> {
        return captureDao.getCapturedCreatureIdsFlow(playerId).map { it.toSet() }
    }

    override suspend fun getStats(playerId: Long): PlayerStats = withContext(Dispatchers.IO) {
        val total = captureDao.getTotalCaptureCount(playerId)
        val unique = captureDao.getUniqueCreaturesCount(playerId)
        val captures = getCapturesForPlayer(playerId)
        val byRarity = captures.mapNotNull { it.creature?.rarity }.groupingBy { it }.eachCount()
        PlayerStats(
            totalCaptures = total,
            uniqueSpecies = unique,
            capturesByRarity = byRarity
        )
    }
}

interface SessionRepository {
    suspend fun getActivePlayerId(): Long?
    suspend fun clearSession()
}

class SessionRepositoryImpl(
    private val sessionDao: SessionDao
) : SessionRepository {

    override suspend fun getActivePlayerId(): Long? = withContext(Dispatchers.IO) {
        sessionDao.getActiveSession()?.playerId
    }

    override suspend fun clearSession() = withContext(Dispatchers.IO) {
        sessionDao.clearActiveSessions()
    }
}
