package com.pengxh.daily.app.utils

import com.pengxh.daily.app.DailyTaskApplication
import com.pengxh.daily.app.bean.DailyTaskBean
import com.pengxh.daily.app.bean.NotificationBean

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

    /*****************************************************************************************/
    private val noticeDao by lazy { DailyTaskApplication.get().dataBase.noticeDao() }

    fun deleteAllNotice() {
        noticeDao.deleteAll()
    }

    fun loadNoticeByTime(pageSize: Int, offset: Int): MutableList<NotificationBean> {
        return noticeDao.loadNoticeByTime(pageSize, offset)
    }

    fun insertNotice(bean: NotificationBean) {
        noticeDao.insert(bean)
    }
}