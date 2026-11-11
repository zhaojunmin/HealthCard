package com.healthcard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.healthcard.MainActivity
import com.healthcard.R
import com.healthcard.config.ConfigRepository
import com.healthcard.widget.HealthStatusStore

/**
 * 接收每日提醒闹钟（[ReminderScheduler.ACTION_REMIND]）与开机广播。
 * - 闹钟触发：若今日未全部打卡，发一条提醒通知。
 * - 开机：若提醒已开启，重新登记闹钟（系统重启会清空 AlarmManager）。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (ConfigRepository(context).reminderEnabled) {
                ReminderScheduler.schedule(context)
            }
            return
        }

        val remaining = 4 - HealthStatusStore.today(context).count
        if (remaining <= 0) return
        notifyReminder(context, remaining)
    }

    private fun notifyReminder(context: Context, remaining: Int) {
        createChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("健康打卡提醒")
            .setContentText("今天还有 $remaining 项健康打卡未完成，别忘了哦")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // 发通知前确认有权限（Android 13+ 未授权则静默忽略）
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "打卡提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "每日健康打卡提醒" }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "healthcard_reminder"
        const val NOTIFICATION_ID = 2002
    }
}
