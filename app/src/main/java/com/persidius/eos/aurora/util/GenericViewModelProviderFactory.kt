package com.persidius.eos.aurora.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.ui.login.LoginViewModel


abstract class GenericViewModelProviderFactory<T>(val targetClass: Class<T>): ViewModelProvider.Factory {

    override fun <U : ViewModel?> create(modelClass: Class<U>): U {
        if(!modelClass.isAssignableFrom(targetClass)) {
            throw InstantiationError("Cannot use ${this::class.java.name} to instantiate classes other than ${targetClass.name}")
        }
        return this.onCreate() as U
    }

    // Just return/construct your T2 normally here.
    abstract fun onCreate(): T
}