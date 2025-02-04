package com.reinvent.sarva.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel()
{

    private val _text = MutableLiveData<String>().apply {
        value = "Language control will comes here"
    }
    val text : LiveData<String> = _text
}