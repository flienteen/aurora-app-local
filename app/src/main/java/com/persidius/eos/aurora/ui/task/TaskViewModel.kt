package com.persidius.eos.aurora.ui.task

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.database.entities.Task
import com.persidius.eos.aurora.database.entities.Uat

class TaskViewModel : ViewModel() {

    val comments: MutableLiveData<String> = MutableLiveData()
    val groups: MutableLiveData<String> = MutableLiveData()
    val users: MutableLiveData<String> = MutableLiveData()
    val recipients: MutableLiveData<MutableList<String>> = MutableLiveData(ArrayList())
    val uat: MutableLiveData<String> = MutableLiveData()
    val loc: MutableLiveData<String> = MutableLiveData()
    var task: Task? = null

    var uats: MutableLiveData<List<Uat>> = MutableLiveData(listOf())
    var locs: MutableLiveData<List<Loc>> = MutableLiveData(listOf())
}