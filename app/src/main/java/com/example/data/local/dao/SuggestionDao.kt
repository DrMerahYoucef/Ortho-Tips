package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FieldSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestionDao {

    @Query(
        "SELECT * FROM field_suggestions WHERE fieldKey = :fieldKey " +
            "ORDER BY useCount DESC, lastUsedAt DESC LIMIT :limit"
    )
    fun getSuggestions(fieldKey: String, limit: Int = 30): Flow<List<FieldSuggestionEntity>>

    @Query("SELECT useCount FROM field_suggestions WHERE fieldKey = :fieldKey AND value = :value LIMIT 1")
    suspend fun getUseCount(fieldKey: String, value: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FieldSuggestionEntity)

    @Query("DELETE FROM field_suggestions WHERE fieldKey = :fieldKey AND value = :value")
    suspend fun delete(fieldKey: String, value: String)

    @Query("DELETE FROM field_suggestions")
    suspend fun deleteAll()
}
