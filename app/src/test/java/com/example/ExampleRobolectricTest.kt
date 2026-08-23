package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DefaultOrthoData
import com.example.data.local.OrthoDatabase
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.repository.OrthoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: OrthoDatabase
    private lateinit var repository: OrthoRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OrthoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OrthoRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ortho Tips", appName)
    }

    @Test
    fun verifyDatabaseSeedingAndChapters() = runBlocking {
        repository.ensureDatabaseSeeded()
        val chapters = repository.getAllChaptersWithCounts().first()
        assertTrue("Chapters should not be empty", chapters.isNotEmpty())
        assertTrue("Should contain Upper Limb chapter", chapters.any { it.chapter.id == "chap_upper_limb" })
    }

    @Test
    fun verifyHighYieldBones() = runBlocking {
        repository.ensureDatabaseSeeded()
        val highYield = repository.getHighYieldBones().first()
        assertTrue("High yield bones should not be empty", highYield.isNotEmpty())
        assertTrue(highYield.any { it.name.contains("Radius", ignoreCase = true) })
    }

    @Test
    fun verifyBookmarkToggle() = runBlocking {
        repository.ensureDatabaseSeeded()
        val boneId = "bone_distal_radius_orif"

        // Initially not bookmarked
        var bookmarkedList = repository.getBookmarkedBones().first()
        assertTrue(!bookmarkedList.any { it.id == boneId })

        // Toggle bookmark ON
        repository.toggleBookmark(boneId)
        bookmarkedList = repository.getBookmarkedBones().first()
        assertTrue(bookmarkedList.any { it.id == boneId })

        // Toggle bookmark OFF
        repository.toggleBookmark(boneId)
        bookmarkedList = repository.getBookmarkedBones().first()
        assertTrue(!bookmarkedList.any { it.id == boneId })
    }

    @Test
    fun verifyAddAndRetrieveNotes() = runBlocking {
        repository.ensureDatabaseSeeded()
        val testNote = NoteEntity(
            id = "note_123",
            boneId = "bone_distal_radius_orif",
            title = "Plate screw length reminder",
            content = "Use 20mm distal screws to avoid dorsal tendon irritation.",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            colorTag = "amber"
        )
        repository.saveNote(testNote)

        val notes = repository.getAllNotes().first()
        assertEquals(1, notes.size)
        assertEquals("Plate screw length reminder", notes[0].title)

        val detail = repository.getBoneDetail("bone_distal_radius_orif").first()
        assertNotNull(detail)
        assertEquals(1, detail?.notes?.size)
    }
}
