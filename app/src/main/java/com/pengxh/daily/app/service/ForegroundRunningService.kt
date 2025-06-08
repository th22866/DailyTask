package com.pengxh.daily.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pengxh.daily.app.R
import com.pengxh.daily.app.utils.Constant

/**
 * APP前台服务，降低APP被系统杀死的可能性
 * */
class ForegroundRunningService : Service() {

    private val notificationId = Int.MAX_VALUE
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, "foreground_running_service_channel").apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentText(Constant.FOREGROUND_RUNNING_SERVICE_TITLE)
            setPriority(NotificationCompat.PRIORITY_HIGH) // 设置通知优先级
            setOngoing(true)
            setOnlyAlertOnce(true)
            setSilent(true)
            setCategory(NotificationCompat.CATEGORY_SERVICE)
            setShowWhen(true)
            setSound(null) // 禁用声音
            setVibrate(null) // 禁用振动
        }
    }

    override fun onCreate() {
        super.onCreate()
        val name = "${resources.getString(R.string.app_name)}前台服务"
        val channel = NotificationChannel(
            "foreground_running_service_channel", name, NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Channel for Foreground Running Service"
        notificationManager.createNotificationChannel(channel)
        startForeground(notificationId, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}