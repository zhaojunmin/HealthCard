package com.healthcard.config

import android.content.Context
import com.healthcard.model.DeepSeekModels

/**
 * 大模型相关配置的持久化（基于 SharedPreferences）。
 *
 * 目前保存 DeepSeek 的 API Key 与所用模型名。Key 以明文存储于应用私有空间；
 * 如需更高安全性可后续换用 EncryptedSharedPreferences。
 */
class ConfigRepository(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var deepSeekApiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_API, value).apply()
        }

    var deepSeekModel: String
        get() = prefs.getString(KEY_MODEL, DeepSeekModels.CHAT) ?: DeepSeekModels.CHAT
        set(value) {
            prefs.edit().putString(KEY_MODEL, value).apply()
        }

    /** 是否已配置可用的 Key。 */
    val isConfigured: Boolean
        get() = deepSeekApiKey.isNotBlank()

    /** 是否开启每日打卡提醒（固定 21:00）。 */
    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_REMINDER, value).apply()
        }

    private companion object {
        const val PREFS_NAME = "model_config"
        const val KEY_API = "deepseek_api_key"
        const val KEY_MODEL = "deepseek_model"
        const val KEY_REMINDER = "reminder_enabled"
    }
}
