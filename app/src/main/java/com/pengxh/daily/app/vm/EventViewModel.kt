package com.pengxh.daily.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EventViewModel : ViewModel() {
    private val _eventFlow = MutableSharedFlow<Int>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun sendEvent(code: Int) {
        viewModelScope.launch {
            _eventFlow.emit(code)
        }
    }
}