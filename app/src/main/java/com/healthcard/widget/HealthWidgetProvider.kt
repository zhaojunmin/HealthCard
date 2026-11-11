package com.healthcard.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.healthcard.MainActivity
import com.healthcard.R

/**
 * 桌面小组件：展示今日打卡进度（n/4）与四个习惯状态，点击整块打开 App。
 * 数据来自 [HealthStatusStore]，由 App 在刷新今日状态后调用 [refresh] 主动更新。
 */
class HealthWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    companion object {
        private const val DONE_COLOR = 0xFF10B981.toInt()   // 翡翠绿
        private const val MISS_COLOR = 0xFF9AA0A6.toInt()   // 灰

        /** 主动刷新所有已添加的小组件。 */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, HealthWidgetProvider::class.java)
            )
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val s = HealthStatusStore.today(context)
            val views = RemoteViews(context.packageName, R.layout.widget_health)

            views.setTextViewText(R.id.widget_count, "${s.count}/4")
            setHabit(views, R.id.widget_exercise, "运动", s.exercise)
            setHabit(views, R.id.widget_diet, "饮食", s.diet)
            setHabit(views, R.id.widget_sleep, "睡觉", s.sleep)
            setHabit(views, R.id.widget_mood, "心态", s.mood)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            manager.updateAppWidget(id, views)
        }

        private fun setHabit(views: RemoteViews, viewId: Int, name: String, done: Boolean) {
            views.setTextViewText(viewId, if (done) "✓ $name" else "○ $name")
            views.setTextColor(viewId, if (done) DONE_COLOR else MISS_COLOR)
        }
    }
}
