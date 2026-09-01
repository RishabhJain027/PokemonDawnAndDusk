package com.dawnanddusk.data.local.dao

import androidx.room.*
import com.dawnanddusk.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getPlayerById(id: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE username = :username LIMIT 1")
    suspend fun getPlayerByUsername(username: String): PlayerEntity?

    @Query("SELECT COUNT(*) FROM players WHERE username = :username")
    suspend fun usernameExists(username: String): Int
}

@Dao
interface CreatureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(creatures: List<CreatureEntity>)

    @Query("SELECT * FROM creatures ORDER BY id ASC")
    fun getAllCreaturesFlow(): Flow<List<CreatureEntity>>

    @Query("SELECT * FROM creatures ORDER BY id ASC")
    suspend fun getAllCreatures(): List<CreatureEntity>

    @Query("SELECT * FROM creatures WHERE id = :id LIMIT 1")
    suspend fun getCreatureById(id: Int): CreatureEntity?

    @Query("SELECT * FROM creatures WHERE rarityCode = :rarityCode")
    suspend fun getCreaturesByRarity(rarityCode: String): List<CreatureEntity>

    @Query("SELECT COUNT(*) FROM creatures")
    suspend fun getCreatureCount(): Int
}

@Dao
interface SpawnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpawns(spawns: List<SpawnEntity>)

    @Query("SELECT * FROM spawns WHERE isCaptured = 0 AND expiresAt > :currentTime")
    suspend fun getActiveSpawns(currentTime: Long): List<SpawnEntity>

    @Query("SELECT * FROM spawns WHERE isCaptured = 0 AND expiresAt > :currentTime")
    fun getActiveSpawnsFlow(currentTime: Long): Flow<List<SpawnEntity>>

    @Query("SELECT * FROM spawns WHERE id = :id LIMIT 1")
    suspend fun getSpawnById(id: String): SpawnEntity?

    @Query("UPDATE spawns SET isCaptured = 1 WHERE id = :id")
    suspend fun markSpawnCaptured(id: String)

    @Query("DELETE FROM spawns WHERE expiresAt <= :currentTime OR isCaptured = 1")
    suspend fun deleteExpiredSpawns(currentTime: Long)
}

@Dao
interface CaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: CaptureEntity): Long

    @Query("SELECT * FROM captures WHERE playerId = :playerId ORDER BY capturedAt DESC")
    fun getCapturesForPlayerFlow(playerId: Long): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE playerId = :playerId ORDER BY capturedAt DESC")
    suspend fun getCapturesForPlayer(playerId: Long): List<CaptureEntity>

    @Query("SELECT * FROM captures WHERE playerId = :playerId AND creatureId = :creatureId ORDER BY capturedAt DESC")
    suspend fun getCapturesByCreature(playerId: Long, creatureId: Int): List<CaptureEntity>

    @Query("SELECT COUNT(*) FROM captures WHERE playerId = :playerId")
    suspend fun getTotalCaptureCount(playerId: Long): Int

    @Query("SELECT COUNT(DISTINCT creatureId) FROM captures WHERE playerId = :playerId")
    suspend fun getUniqueCreaturesCount(playerId: Long): Int

    @Query("SELECT DISTINCT creatureId FROM captures WHERE playerId = :playerId")
    suspend fun getCapturedCreatureIds(playerId: Long): List<Int>

    @Query("SELECT DISTINCT creatureId FROM captures WHERE playerId = :playerId")
    fun getCapturedCreatureIdsFlow(playerId: Long): Flow<List<Int>>
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE isActive = 1 ORDER BY loginAt DESC LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Query("UPDATE sessions SET isActive = 0")
    suspend fun clearActiveSessions()
}
