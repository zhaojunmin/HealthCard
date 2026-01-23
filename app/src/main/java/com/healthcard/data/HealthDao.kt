package com.healthcard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HealthDao {
    @Insert
    suspend fun insertRecord(record: HealthRecord): Long

    @Query("SELECT * FROM health_records WHERE type = :type AND date = :date")
    suspend fun getRecordByTypeAndDate(type: String, date: String): HealthRecord?

    @Query("SELECT COUNT(*) FROM health_records WHERE date = :date")
    suspend fun getTodayRecordCount(date: String): Long

    @Query("SELECT * FROM health_records ORDER BY date DESC")
    suspend fun getAllRecords(): List<HealthRecord>

    @Query("SELECT * FROM health_records WHERE date = :date")
    suspend fun getRecordsByDate(date: String): List<HealthRecord>

    @Query("SELECT DISTINCT date FROM health_records ORDER BY date DESC")
    suspend fun getAllDates(): List<String>
}
