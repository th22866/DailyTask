package com.pengxh.daily.app.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.pengxh.daily.app.bean.NotificationBean;

import java.util.List;

@Dao
public interface NotificationBeanDao {
    @Query("DELETE FROM notice_record_table")
    void deleteAll();

    /**
     * @Parameter pageSize:每页显示多少条数据
     * @Parameter offset:从第几条数据开始显示
     */
    @Query("SELECT * FROM notice_record_table ORDER BY postTime DESC LIMIT :pageSize OFFSET :offset")
    List<NotificationBean> loadNoticeByTime(int pageSize, int offset);

    @Insert
    void insert(NotificationBean bean);
}
