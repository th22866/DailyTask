package com.pengxh.daily.app.utils

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.greenrobot.eventbus.EventBus

class CountDownWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val kTag = "CountDownWorker"

    override fun doWork(): Result {
        val secondsInFuture = inputData.getInt("secondsInFuture", 0)
        var remainingSeconds = secondsInFuture
        while (remainingSeconds > 0 && !isStopped) {
            Thread.sleep(1000)
            remainingSeconds--
            // 更新倒计时
            EventBus.getDefault().post(
                MessageEvent(
                    Constant.UPDATE_COUNT_DOWN_WORKER_CODE, secondsInFuture, remainingSeconds
                )
            )
        }
        if (isStopped) {
            Log.d(kTag, "doWork: 取消单个任务倒计时")
            return Result.failure()
        } else {
            EventBus.getDefault().post(MessageEvent(Constant.COUNT_DOWN_WORKER_COMPLETED_CODE))
            return Result.success()
        }
    }
}