package com.dawnanddusk.domain.usecase

import com.dawnanddusk.core.AppResult
import com.dawnanddusk.data.repository.*
import com.dawnanddusk.domain.engine.CaptureEngine
import com.dawnanddusk.domain.engine.SpawnEngine
import com.dawnanddusk.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class LoginUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): AppResult<Player> {
        return authRepository.login(username, password)
    }
}

class RegisterUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        username: String,
        password: String,
        displayName: String,
        avatarGender: String
    ): AppResult<Player> {
        return authRepository.register(username, password, displayName, avatarGender)
    }
}

class GetSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Player? {
        val playerId = sessionRepository.getActivePlayerId() ?: return null
        return authRepository.getPlayer(playerId)
    }
}

class LogoutUserUseCase(private val sessionRepository: SessionRepository) {
    suspend operator fun invoke() {
        sessionRepository.clearSession()
    }
}

class GenerateSpawnsUseCase(
    private val spawnRepository: SpawnRepository,
    private val spawnEngine: SpawnEngine
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): List<Spawn> {
        return spawnRepository.refreshSpawns(latitude, longitude, spawnEngine)
    }

    fun getActiveSpawnsFlow(): Flow<List<Spawn>> {
        return spawnRepository.getActiveSpawnsFlow()
    }
}

class ValidateEncounterUseCase(
    private val spawnEngine: SpawnEngine,
    private val spawnRepository: SpawnRepository
) {
    suspend operator fun invoke(
        playerLatitude: Double,
        playerLongitude: Double,
        spawnId: String
    ): EncounterValidationResult {
        val spawn = spawnRepository.getSpawnById(spawnId)
            ?: return EncounterValidationResult.NotFound

        if (spawn.isExpired()) {
            return EncounterValidationResult.Expired
        }

        val canInteract = spawnEngine.canInteractWithSpawn(playerLatitude, playerLongitude, spawn)
        return if (canInteract && spawn.creature != null) {
            EncounterValidationResult.Allowed(
                Encounter(
                    id = java.util.UUID.randomUUID().toString(),
                    spawn = spawn,
                    creature = spawn.creature
                )
            )
        } else {
            EncounterValidationResult.TooFar
        }
    }
}

sealed interface EncounterValidationResult {
    data class Allowed(val encounter: Encounter) : EncounterValidationResult
    data object TooFar : EncounterValidationResult
    data object Expired : EncounterValidationResult
    data object NotFound : EncounterValidationResult
}

class ResolveCaptureUseCase(
    private val captureEngine: CaptureEngine,
    private val captureRepository: CaptureRepository,
    private val spawnRepository: SpawnRepository
) {
    suspend operator fun invoke(
        playerId: Long,
        encounter: Encounter,
        throwResult: ThrowResult,
        playerLatitude: Double,
        playerLongitude: Double
    ): CaptureResolution {
        val resolution = captureEngine.resolveThrow(encounter.creature, throwResult)

        if (resolution.outcome == CaptureOutcome.CAPTURED) {
            // Save capture record in database transaction
            captureRepository.saveCapture(
                playerId = playerId,
                creatureId = encounter.creature.id,
                latitude = playerLatitude,
                longitude = playerLongitude,
                accuracyBonus = throwResult.hitAccuracy.toDouble()
            )
            // Mark spawn as consumed so it disappears from map
            spawnRepository.markSpawnCaptured(encounter.spawn.id)
            encounter.isFinished = true
        } else if (resolution.outcome == CaptureOutcome.ESCAPED) {
            encounter.isFinished = true
        }

        return resolution
    }
}

data class PokedexEntry(
    val creature: Creature,
    val isCaptured: Boolean,
    val captureCount: Int = 0
)

class GetPokedexUseCase(
    private val creatureRepository: CreatureRepository,
    private val captureRepository: CaptureRepository
) {
    suspend operator fun invoke(playerId: Long): List<PokedexEntry> {
        val creatures = creatureRepository.getAllCreatures()
        val capturedIds = captureRepository.getCapturedCreatureIds(playerId)
        val playerCaptures = captureRepository.getCapturesForPlayer(playerId)
        val countsByCreature = playerCaptures.groupingBy { it.creatureId }.eachCount()

        return creatures.map { creature ->
            PokedexEntry(
                creature = creature,
                isCaptured = creature.id in capturedIds,
                captureCount = countsByCreature[creature.id] ?: 0
            )
        }
    }

    fun getPokedexFlow(playerId: Long): Flow<List<PokedexEntry>> {
        return combine(
            creatureRepository.getAllCreaturesFlow(),
            captureRepository.getCapturedCreatureIdsFlow(playerId)
        ) { creatures, capturedIds ->
            creatures.map { creature ->
                PokedexEntry(
                    creature = creature,
                    isCaptured = creature.id in capturedIds
                )
            }
        }
    }
}

class GetCaptureHistoryUseCase(private val captureRepository: CaptureRepository) {
    suspend operator fun invoke(playerId: Long): List<Capture> {
        return captureRepository.getCapturesForPlayer(playerId)
    }

    fun getFlow(playerId: Long): Flow<List<Capture>> {
        return captureRepository.getCapturesForPlayerFlow(playerId)
    }
}

class GetProfileStatsUseCase(
    private val authRepository: AuthRepository,
    private val captureRepository: CaptureRepository
) {
    suspend operator fun invoke(playerId: Long): ProfileData? {
        val player = authRepository.getPlayer(playerId) ?: return null
        val stats = captureRepository.getStats(playerId)
        return ProfileData(
            player = player,
            stats = stats
        )
    }
}

data class ProfileData(
    val player: Player,
    val stats: PlayerStats
)
