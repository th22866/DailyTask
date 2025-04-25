package com.pengxh.daily.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.pengxh.daily.app.R
import com.pengxh.daily.app.utils.Constant

/**
 * APP前台服务，降低APP被系统杀死的可能性
 * */
class ForegroundRunningService : Service() {

    private val notificationId = 1
    private val updateHandler = Handler(Looper.getMainLooper())
    private var notificationManager: NotificationManager? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var runningTime = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val name = "${resources.getString(R.string.app_name)}前台服务"
        val channel = NotificationChannel(
            "foreground_running_service_channel", name, NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Channel for Foreground Running Service"
        notificationManager?.createNotificationChannel(channel)
        notificationBuilder = NotificationCompat.Builder(this, "foreground_running_service_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("已运行0小时0分钟")
            .setContentText(Constant.FOREGROUND_RUNNING_SERVICE_TITLE)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 设置通知优先级
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notification = notificationBuilder?.build()
        startForeground(notificationId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //记录通知被创建的时间
        runningTime = System.currentTimeMillis()
        val updateRunnable = object : Runnable {
            override fun run() {
                updateNotification()
                updateHandler.postDelayed(this, 1000L * 60)
            }
        }
        updateHandler.post(updateRunnable)
        return START_STICKY
    }

    private fun updateNotification() {
        // 计算运行时长
        val elapsedTime = System.currentTimeMillis() - runningTime
        val hours = (elapsedTime / (1000 * 60 * 60)).toInt()
        val minutes = (elapsedTime % (1000 * 60 * 60) / (1000 * 60)).toInt()
        val notification = notificationBuilder?.let {
            it.setContentTitle("已运行${hours}小时${minutes}分钟")
            it.build()
        }
        notificationManager?.notify(notificationId, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHandler.removeCallbacksAndMessages(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}