package com.pengxh.daily.app.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * 只能用于Activity或者Fragment激活状态下，其他状态，数据会丢失，不具备粘性
 * */
class SharedDataViewModel : ViewModel() {
    var addTaskCode = MutableLiveData<Int>()
}