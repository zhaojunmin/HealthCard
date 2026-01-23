package com.healthcard.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthcard.data.HealthDatabase
import com.healthcard.data.HealthRecord
import com.healthcard.data.HealthRepository
import kotlinx.coroutines.launch

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HealthRepository

    val isExerciseChecked = mutableStateOf(false)
    val isDietChecked = mutableStateOf(false)
    val isSleepChecked = mutableStateOf(false)
    val isMoodChecked = mutableStateOf(false)
    val todayCount = mutableStateOf(0)
    val allRecords = mutableStateOf<List<HealthRecord>>(emptyList())
    val groupedRecords = mutableStateOf<Map<String, List<HealthRecord>>>(emptyMap())
    val isLoading = mutableStateOf(false)

    init {
        val healthDao = HealthDatabase.getDatabase(application).healthDao()
        repository = HealthRepository(healthDao)
        loadTodayStatus()
    }

    companion object {
        fun Factory(application: Application): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HealthViewModel(application) as T
            }
        }
    }

    private fun loadTodayStatus() {
        viewModelScope.launch {
            isExerciseChecked.value = repository.isRecordedToday("exercise")
            isDietChecked.value = repository.isRecordedToday("diet")
            isSleepChecked.value = repository.isRecordedToday("sleep")
            isMoodChecked.value = repository.isRecordedToday("mood")
            todayCount.value = repository.getTodayRecordCount()
        }
    }

    fun checkExercise() {
        viewModelScope.launch {
            repository.insertHealthRecord("exercise")
            loadTodayStatus()
        }
    }

    fun checkDiet() {
        viewModelScope.launch {
            repository.insertHealthRecord("diet")
            loadTodayStatus()
        }
    }

    fun checkSleep() {
        viewModelScope.launch {
            repository.insertHealthRecord("sleep")
            loadTodayStatus()
        }
    }

    fun checkMood() {
        viewModelScope.launch {
            repository.insertHealthRecord("mood")
            loadTodayStatus()
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val records = repository.getAllRecords()
                allRecords.value = records
                groupedRecords.value = records.groupBy { it.date }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun getRecordsByDate(date: String): List<HealthRecord> {
        return groupedRecords.value[date] ?: emptyList()
    }

    fun getRecordTypesByDate(date: String): Set<String> {
        return getRecordsByDate(date).map { it.type }.toSet()
    }
}
