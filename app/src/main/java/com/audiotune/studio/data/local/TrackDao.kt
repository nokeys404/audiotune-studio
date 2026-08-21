package com.audiotune.studio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE playedAt > 0 ORDER BY playedAt DESC LIMIT 20")
    fun getRecentlyPlayed(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Query("UPDATE tracks SET playedAt = :timestamp WHERE id = :trackId")
    suspend fun updatePlayedAt(trackId: String, timestamp: Long)
}
