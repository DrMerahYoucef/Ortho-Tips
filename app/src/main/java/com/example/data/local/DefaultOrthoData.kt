package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BoneDao
import com.example.data.local.dao.ChapterDao
import com.example.data.local.dao.ContentDao
import com.example.data.local.dao.ImageDao
import com.example.data.local.dao.SearchDao
import com.example.data.local.dao.UserDataDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.BoneEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.ImageEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.SectionEntity

@Database(
    entities = [
        ChapterEntity::class,
        BoneEntity::class,
        SectionEntity::class,
        ImageEntity::class,
        BookmarkEntity::class,
        NoteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class OrthoDatabase : RoomDatabase() {
    abstract fun chapterDao(): ChapterDao
    abstract fun boneDao(): BoneDao
    abstract fun contentDao(): ContentDao
    abstract fun imageDao(): ImageDao
    abstract fun userDataDao(): UserDataDao
    abstract fun searchDao(): SearchDao

    companion object {
        @Volatile
        private var INSTANCE: OrthoDatabase? = null

        fun getInstance(context: Context): OrthoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OrthoDatabase::class.java,
                    "ortho_tips.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
