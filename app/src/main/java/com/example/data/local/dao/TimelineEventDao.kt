package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {

    @Query("SELECT * FROM timeline_events WHERE patientId = :patientId ORDER BY eventDate DESC")
    fun getEventsForPatient(patientId: String): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE id = :id LIMIT 1")
    fun getEventById(id: String): Flow<TimelineEventEntity?>

    @Query("SELECT * FROM timeline_events WHERE id = :id LIMIT 1")
    suspend fun getEventDirect(id: String): TimelineEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TimelineEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<TimelineEventEntity>)

    @Update
    suspend fun updateEvent(event: TimelineEventEntity)

    @Query("DELETE FROM timeline_events WHERE id = :id")
    suspend fun deleteEvent(id: String)

    @Query("SELECT COUNT(*) FROM timeline_events WHERE patientId = :patientId")
    suspend fun getEventCountForPatient(patientId: String): Int
}
