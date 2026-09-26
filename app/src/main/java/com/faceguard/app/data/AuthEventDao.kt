package com.faceguard.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthEventDao {

    @Insert
    suspend fun insert(event: AuthEventEntity): Long

    @Update
    suspend fun update(event: AuthEventEntity)

    @Query("SELECT * FROM auth_events ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<AuthEventEntity>>

    @Query("SELECT * FROM auth_events WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC")
    fun observeSince(sinceMillis: Long): Flow<List<AuthEventEntity>>

    @Query("SELECT * FROM auth_events WHERE scheduledDeletionMillis IS NOT NULL AND scheduledDeletionMillis <= :nowMillis")
    suspend fun getExpiredEvents(nowMillis: Long): List<AuthEventEntity>

    @Query("SELECT * FROM auth_events WHERE backupStatus = :status")
    suspend fun getByBackupStatus(status: BackupStatus): List<AuthEventEntity>

    @Delete
    suspend fun delete(event: AuthEventEntity)

    @Query("DELETE FROM auth_events")
    suspend fun deleteAll()

    @Query("""
        SELECT COUNT(*) FROM auth_events
        WHERE timestampMillis BETWEEN :dayStartMillis AND :dayEndMillis
        AND eventType = :type
    """)
    suspend fun countTodayByType(dayStartMillis: Long, dayEndMillis: Long, type: AuthEventType): Int
}
