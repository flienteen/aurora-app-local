package com.persidius.eos.aurora.ui.tasks

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.database.entities.Task
import com.persidius.eos.aurora.database.entities.Uat

class TasksViewModel(val adapter: TasksAdapter) : ViewModel() {
    val searchTerm: MutableLiveData<String> = MutableLiveData("")
    val results: MutableLiveData<List<Triple<Task, Uat, Loc>>> = MutableLiveData()
}