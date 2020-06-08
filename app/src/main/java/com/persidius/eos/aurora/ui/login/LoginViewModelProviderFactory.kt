package com.persidius.eos.aurora.ui.login

import com.persidius.eos.aurora.auth.AuthManager
import com.persidius.eos.aurora.util.GenericViewModelProviderFactory

class LoginViewModelProviderFactory(private val authMgr: AuthManager):
    GenericViewModelProviderFactory<LoginViewModel>(LoginViewModel::class.java) {
    override fun onCreate() = LoginViewModel(authMgr)
}