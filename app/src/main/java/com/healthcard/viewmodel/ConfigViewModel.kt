package com.healthcard.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.healthcard.config.ConfigRepository
import com.healthcard.model.DeepSeekModels
import com.healthcard.service.ReminderScheduler

/**
 * 模型配置界面的 ViewModel：加载/编辑/保存 DeepSeek Key 与模型名，以及每日提醒开关。
 */
class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository(application)

    val apiKey = mutableStateOf(repository.deepSeekApiKey)
    val model = mutableStateOf(repository.deepSeekModel)
    val saved = mutableStateOf(false)

    /** 每日提醒开关（固定 21:00）。 */
    val reminderEnabled = mutableStateOf(repository.reminderEnabled)

    fun onReminderToggle(enabled: Boolean) {
        reminderEnabled.value = enabled
        repository.reminderEnabled = enabled
        val app = getApplication<Application>()
        if (enabled) ReminderScheduler.schedule(app) else ReminderScheduler.cancel(app)
    }

    /** 可选模型列表。 */
    val availableModels = listOf(DeepSeekModels.CHAT, DeepSeekModels.REASONER)

    fun onApiKeyChange(value: String) {
        apiKey.value = value
        saved.value = false
    }

    fun onModelChange(value: String) {
        model.value = value
        saved.value = false
    }

    fun save() {
        repository.deepSeekApiKey = apiKey.value.trim()
        repository.deepSeekModel = model.value
        saved.value = true
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ConfigViewModel(application) as T
            }
    }
}
