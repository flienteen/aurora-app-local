package com.persidius.eos.aurora.ui.login

import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.util.GenericViewModelProviderFactory

class LoginViewModelProviderFactory(val am: AuthorizationManager):
    GenericViewModelProviderFactory<LoginViewModel>(LoginViewModel::class.java) {
    override fun onCreate() = LoginViewModel(am)
}