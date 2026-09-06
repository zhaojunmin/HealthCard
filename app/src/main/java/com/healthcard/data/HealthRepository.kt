package com.healthcard.data

import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HealthRepository(private val healthDao: HealthDao) {
    private val insertMutex = Mutex()

    suspend fun insertHealthRecord(type: String) {
        insertMutex.withLock {
            if (isRecordedToday(type)) return@withLock
            val today = LocalDate.now().toString()
            val timestamp = System.currentTimeMillis()
            val record = HealthRecord(type = type, date = today, timestamp = timestamp)
            healthDao.insertRecord(record)
        }
    }

    suspend fun isRecordedToday(type: String): Boolean {
        val today = LocalDate.now().toString()
        return healthDao.getRecordByTypeAndDate(type, today) != null
    }

    suspend fun getTodayRecordCount(): Int {
        val today = LocalDate.now().toString()
        return healthDao.getTodayRecordCount(today).toInt()
    }

    suspend fun getAllRecords(): List<HealthRecord> {
        return healthDao.getAllRecords()
    }

    suspend fun getRecordsByDate(date: String): List<HealthRecord> {
        return healthDao.getRecordsByDate(date)
    }

    suspend fun getAllDates(): List<String> {
        return healthDao.getAllDates()
    }
}
