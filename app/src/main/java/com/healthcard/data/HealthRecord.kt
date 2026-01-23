package com.healthcard.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val date: String,
    val timestamp: Long
)
