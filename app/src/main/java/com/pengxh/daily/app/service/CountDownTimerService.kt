package com.pengxh.daily.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pengxh.daily.app.R
import com.pengxh.daily.app.extensions.formatTime
import com.pengxh.daily.app.extensions.openApplication
import com.pengxh.daily.app.utils.Constant

/**
 * APP倒计时服务，解决手机灭屏后倒计时会出现延迟的问题
 * */
class CountDownTimerService : Service() {

    private val kTag = "CountDownTimerService"
    private val binder by lazy { LocaleBinder() }
    private val notificationId = Int.MAX_VALUE
    private var notificationManager: NotificationManager? = null
    private var notificationBuilder: NotificationCompat.Builder? = null

    inner class LocaleBinder : Binder() {
        fun getService(): CountDownTimerService = this@CountDownTimerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        val name = "${resources.getString(R.string.app_name)}倒计时服务"
        val channel = NotificationChannel(
            "countdown_timer_service_channel", name, NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Channel for CountDownTimer Service"
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager?.createNotificationChannel(channel)
        notificationBuilder = NotificationCompat.Builder(
            this, "countdown_timer_service_channel"
        ).run {
            setSmallIcon(R.mipmap.ic_launcher)
            setPriority(NotificationCompat.PRIORITY_MIN) // 设置通知优先级
            setOngoing(true)
            setOnlyAlertOnce(true)
            setSilent(true)
            setCategory(NotificationCompat.CATEGORY_SERVICE)
            setShowWhen(true)
            setSound(null) // 禁用声音
            setVibrate(null) // 禁用振动
        }
        notificationBuilder?.build().also {
            startForeground(notificationId, it)
        }
    }

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false

    fun startCountDown(index: Int, seconds: Int) {
        if (isTimerRunning) {
            countDownTimer?.cancel()
            isTimerRunning = false
        }
        Log.d(kTag, "startCountDown: 倒计时任务开始，执行第${index}个任务")
        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                notificationBuilder?.let {
                    it.setContentText(
                        "${(millisUntilFinished / 1000).toInt().formatTime()}后执行第${index}个任务"
                    )
                    it.build()
                }.also {
                    notificationManager?.notify(notificationId, it)
                }
            }

            override fun onFinish() {
                isTimerRunning = false
                openApplication(Constant.DING_DING, true)
            }
        }.apply {
            start()
        }
        isTimerRunning = true
    }

    fun cancelCountDown() {
        if (isTimerRunning) {
            countDownTimer?.cancel()
            isTimerRunning = false
        }
        Log.d(kTag, "cancelCountDown: 倒计时任务取消")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        cancelCountDown()
    }
}