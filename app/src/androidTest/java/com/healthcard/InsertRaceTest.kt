package com.healthcard

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.healthcard.data.HealthDatabase
import com.healthcard.data.HealthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class InsertRaceTest {
    private lateinit var db: HealthDatabase
    private lateinit var repo: HealthRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = HealthRepository(db.healthDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun today(): String = LocalDate.now().toString()

    private suspend fun rowCountFor(date: String, type: String): Int {
        val rows = db.healthDao().getRecordsByDate(date)
        return rows.count { it.type == type }
    }

    @Test
    fun twoConcurrentInsertsForSameType_createSingleRow() = runBlocking {
        val race = List(2) {
            async(Dispatchers.IO) { repo.insertHealthRecord("exercise") }
        }
        race.awaitAll()

        assertEquals(1, rowCountFor(today(), "exercise"))
        assertEquals(1, repo.getTodayRecordCount())
    }

    @Test
    fun concurrentInsertsForDifferentTypes_eachInsertedExactlyOnce() = runBlocking {
        val types = listOf("exercise", "diet", "sleep", "mood")
        val race = types.map { type ->
            async(Dispatchers.IO) { repo.insertHealthRecord(type) }
        }
        race.awaitAll()

        types.forEach { type ->
            assertEquals("$type should have exactly one row", 1, rowCountFor(today(), type))
            assertTrue("$type should be recorded", repo.isRecordedToday(type))
        }
        assertEquals(4, repo.getTodayRecordCount())
    }

    @Test
    fun sequentialDuplicateInsert_sameType_keepsSingleRow() = runBlocking {
        repo.insertHealthRecord("exercise")
        repo.insertHealthRecord("exercise")
        repo.insertHealthRecord("exercise")

        assertEquals(1, rowCountFor(today(), "exercise"))
        assertEquals(1, repo.getTodayRecordCount())
    }
}
