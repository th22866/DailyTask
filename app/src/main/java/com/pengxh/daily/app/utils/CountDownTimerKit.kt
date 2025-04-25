package com.pengxh.daily.app.utils

import android.os.Handler
import android.os.Looper
import android.util.Log

class CountDownTimerKit(
    private val secondsInFuture: Int, private val callback: OnTimeCountDownCallback
) {
    private val kTag = "CountDownTimerKit"
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var isTimerRunning = false

    fun start() {
        if (isTimerRunning) {
            cancel()
        }
        handler.post(runnable)
        isTimerRunning = true
        Log.d(kTag, "start: 倒计时任务开始")
    }

    private val runnable = object : Runnable {
        var remainingSeconds = secondsInFuture
        override fun run() {
            remainingSeconds--
            if (remainingSeconds > 0) {
                callback.updateCountDownSeconds(remainingSeconds)
                handler.postDelayed(this, 1000)
            } else {
                callback.onFinish()
                isTimerRunning = false
                Log.d(kTag, "run: 倒计时任务结束")
            }
        }
    }

    fun cancel() {
        handler.removeCallbacks(runnable)
        isTimerRunning = false
        Log.d(kTag, "cancel: 倒计时任务取消")
    }
}