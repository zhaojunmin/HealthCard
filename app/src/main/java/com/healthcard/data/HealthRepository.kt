package com.healthcard.data

import java.time.LocalDate

class HealthRepository(private val healthDao: HealthDao) {
    suspend fun insertHealthRecord(type: String) {
        if (isRecordedToday(type)) return
        val today = LocalDate.now().toString()
        val timestamp = System.currentTimeMillis()
        val record = HealthRecord(type = type, date = today, timestamp = timestamp)
        healthDao.insertRecord(record)
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
