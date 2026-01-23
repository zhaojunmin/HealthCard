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
}
