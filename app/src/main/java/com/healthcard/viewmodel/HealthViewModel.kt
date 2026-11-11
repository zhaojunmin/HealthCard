package com.healthcard.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthcard.config.ConfigRepository
import com.healthcard.data.HealthDatabase
import com.healthcard.data.HealthRecord
import com.healthcard.data.HealthRepository
import com.healthcard.model.ChatMessage
import com.healthcard.model.ModelException
import com.healthcard.model.ModelProviderFactory
import com.healthcard.widget.HealthStatusStore
import com.healthcard.widget.HealthWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val REPORT_SYSTEM_PROMPT =
    "你是一位温暖、专业的健康生活教练。请根据用户最近 7 天的习惯打卡数据，用中文写一段 150 字以内的周报，" +
        "包含三点：1) 值得肯定的坚持亮点；2) 需要改进的习惯；3) 下周一个具体、可执行的小建议。" +
        "语气积极鼓励，直接输出正文，不要使用 Markdown 标题或列表符号。"

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HealthRepository
    private val configRepository = ConfigRepository(application)

    val isExerciseChecked = mutableStateOf(false)
    val isDietChecked = mutableStateOf(false)
    val isSleepChecked = mutableStateOf(false)
    val isMoodChecked = mutableStateOf(false)
    val todayCount = mutableStateOf(0)
    val allRecords = mutableStateOf<List<HealthRecord>>(emptyList())
    val groupedRecords = mutableStateOf<Map<String, List<HealthRecord>>>(emptyMap())
    val isLoading = mutableStateOf(false)

    // AI 周报
    val reportLoading = mutableStateOf(false)
    val reportText = mutableStateOf<String?>(null)
    val reportError = mutableStateOf<String?>(null)

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

            // 写入今日快照并刷新桌面小组件
            val app = getApplication<Application>()
            HealthStatusStore.save(
                app,
                exercise = isExerciseChecked.value,
                diet = isDietChecked.value,
                sleep = isSleepChecked.value,
                mood = isMoodChecked.value,
            )
            HealthWidgetProvider.refresh(app)
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

    /** 调用 DeepSeek 根据最近 7 天打卡数据生成一段健康周报。 */
    fun generateWeeklyReport() {
        val apiKey = configRepository.deepSeekApiKey
        if (apiKey.isBlank()) {
            reportError.value = "请先在「模型配置」中填写 DeepSeek API Key"
            return
        }
        val model = configRepository.deepSeekModel
        viewModelScope.launch {
            reportLoading.value = true
            reportError.value = null
            reportText.value = null
            try {
                // 确保已加载历史数据
                if (allRecords.value.isEmpty()) {
                    val records = repository.getAllRecords()
                    allRecords.value = records
                    groupedRecords.value = records.groupBy { it.date }
                }
                val summary = buildWeeklySummary()
                val provider = ModelProviderFactory.deepSeek(apiKey, model)
                val reply = provider.chat(
                    listOf(
                        ChatMessage.system(REPORT_SYSTEM_PROMPT),
                        ChatMessage.user(summary),
                    )
                )
                reportText.value = reply.trim()
            } catch (e: ModelException) {
                reportError.value = "生成失败: ${e.message}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError.value = "出错了: ${e.message}"
            } finally {
                reportLoading.value = false
            }
        }
    }

    /** 把最近 7 天打卡数据整理成给模型的文本摘要。 */
    private fun buildWeeklySummary(): String {
        val typeNames = mapOf(
            "exercise" to "运动", "diet" to "饮食", "sleep" to "睡眠", "mood" to "心态",
        )
        val order = listOf("exercise", "diet", "sleep", "mood")
        val byDate = groupedRecords.value.mapValues { entry -> entry.value.map { it.type }.toSet() }
        val today = LocalDate.now()
        val sb = StringBuilder()
        sb.append("用户每日有 4 个健康习惯可打卡：运动、饮食、睡眠、心态。\n最近 7 天打卡情况：\n")
        var perfect = 0
        var total = 0
        for (i in 6 downTo 0) {
            val d = today.minusDays(i.toLong())
            val set = byDate[d.toString()] ?: emptySet()
            total += set.size
            if (set.size >= order.size) perfect++
            val marks = order.joinToString(" ") { t -> "${typeNames[t]}${if (t in set) "✓" else "✗"}" }
            sb.append("${d.monthValue}月${d.dayOfMonth}日: $marks (${set.size}/4)\n")
        }
        sb.append("本周全勤天数 $perfect 天，累计打卡 $total 次。\n请据此生成周报。")
        return sb.toString()
    }

    fun getRecordsByDate(date: String): List<HealthRecord> {
        return groupedRecords.value[date] ?: emptyList()
    }

    fun getRecordTypesByDate(date: String): Set<String> {
        return getRecordsByDate(date).map { it.type }.toSet()
    }
}
