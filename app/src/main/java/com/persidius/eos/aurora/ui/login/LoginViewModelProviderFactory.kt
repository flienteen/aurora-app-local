package com.persidius.eos.aurora.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.persidius.eos.aurora.authorization.AuthorizationManager

class LoginViewModelProviderFactory<T>(val am: AuthorizationManager): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass != LoginViewModel::class.java) {
            throw InstantiationError("Cannot use LoginViewModelProviderFactory to instantiate any class other than LoginViewModel")
        }
        return LoginViewModel(am) as T
    }
}