package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.data.local.entity.BoneEntity
import com.example.data.local.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @Query("""
        SELECT * FROM bones 
        WHERE name LIKE '%' || :query || '%' 
           OR subtitle LIKE '%' || :query || '%'
           OR region LIKE '%' || :query || '%'
           OR traumaType LIKE '%' || :query || '%'
           OR classificationSummary LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN name LIKE :query || '%' THEN 1
                WHEN name LIKE '%' || :query || '%' THEN 2
                ELSE 3 
            END,
            orderIndex ASC
    """)
    fun searchBones(query: String): Flow<List<BoneEntity>>

    @Query("""
        SELECT * FROM sections 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%'
           OR pearls LIKE '%' || :query || '%'
           OR warnings LIKE '%' || :query || '%'
        ORDER BY orderIndex ASC
    """)
    fun searchSections(query: String): Flow<List<SectionEntity>>
}
