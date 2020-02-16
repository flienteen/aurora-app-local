package com.persidius.eos.aurora.ui.task

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.persidius.eos.aurora.database.entities.Task

class TaskViewModel : ViewModel() {

    val comments: MutableLiveData<String> = MutableLiveData()
    val groups: MutableLiveData<String> = MutableLiveData()
    val users: MutableLiveData<String> = MutableLiveData()
    val recipients: MutableLiveData<MutableList<String>> = MutableLiveData(ArrayList())
    var task: Task? = null
}