package com.pengxh.daily.app.utils

import com.pengxh.daily.app.DailyTaskApplication
import com.pengxh.daily.app.bean.DailyTaskBean

object DatabaseWrapper {
    private val dailyTaskDao by lazy { DailyTaskApplication.get().dataBase.dailyTaskDao() }

    fun loadAll(): ArrayList<DailyTaskBean> {
        return dailyTaskDao.loadAll() as ArrayList<DailyTaskBean>
    }

    fun queryTaskByTime(time: String): Int {
        return dailyTaskDao.queryTaskByTime(time)
    }

    fun insert(bean: DailyTaskBean) {
        dailyTaskDao.insert(bean)
    }
}