package com.persidius.eos.aurora.ui.tasks

import com.persidius.eos.aurora.util.GenericViewModelProviderFactory

class TasksViewModelProviderFactory(private val adapter: TasksAdapter) :
    GenericViewModelProviderFactory<TasksViewModel>(TasksViewModel::class.java) {
    override fun onCreate() = TasksViewModel(adapter)
}