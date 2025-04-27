package com.pengxh.daily.app.extensions

import java.util.Locale

fun Int.formatTime(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val secs = this % 60
    return String.format(Locale.getDefault(), "%02d小时%02d分钟%02d秒", hours, minutes, secs)
}