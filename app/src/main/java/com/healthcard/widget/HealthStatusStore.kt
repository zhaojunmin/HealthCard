package com.healthcard.widget

import android.content.Context
import java.time.LocalDate

/**
 * 今日打卡状态的轻量快照（SharedPreferences）。
 *
 * 供桌面 Widget 与定时提醒读取——它们运行在主线程 / 广播上下文，不适合直接查 Room。
 * 每次 [com.healthcard.viewmodel.HealthViewModel] 刷新今日状态时写入；读取时若日期非今天
 * 则视为全部未打卡。
 */
object HealthStatusStore {

    data class Snapshot(
        val exercise: Boolean,
        val diet: Boolean,
        val sleep: Boolean,
        val mood: Boolean,
    ) {
        val count: Int get() = listOf(exercise, diet, sleep, mood).count { it }
    }

    fun save(
        context: Context,
        exercise: Boolean,
        diet: Boolean,
        sleep: Boolean,
        mood: Boolean,
    ) {
        prefs(context).edit()
            .putString(KEY_DATE, LocalDate.now().toString())
            .putBoolean("exercise", exercise)
            .putBoolean("diet", diet)
            .putBoolean("sleep", sleep)
            .putBoolean("mood", mood)
            .apply()
    }

    fun today(context: Context): Snapshot {
        val p = prefs(context)
        if (p.getString(KEY_DATE, "") != LocalDate.now().toString()) {
            return Snapshot(exercise = false, diet = false, sleep = false, mood = false)
        }
        return Snapshot(
            exercise = p.getBoolean("exercise", false),
            diet = p.getBoolean("diet", false),
            sleep = p.getBoolean("sleep", false),
            mood = p.getBoolean("mood", false),
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "health_status"
    private const val KEY_DATE = "date"
}
