package com.persidius.eos.aurora.ui.login

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import com.persidius.eos.aurora.authorization.AuthorizationManager

class LoginViewModel(private val am: AuthorizationManager): ViewModel() {
    val email: MutableLiveData<String> = MutableLiveData(am.session.email.value!!)
    val locked: LiveData<Boolean> = am.session.locked
    val loggingIn: LiveData<Boolean> = am.session.loggingIn

    val loginError: LiveData<Boolean> = map<Boolean, Boolean>(am.session.loggingIn) {
        loggingIn ->  if(loggingIn) false else am.session.error.value === AuthorizationManager.ErrorCode.LOGIN_FAILED_INVALID_CREDENTIALS
    }
    val loginOtherError: LiveData<Boolean> = map<Boolean, Boolean>(am.session.loggingIn) {
        loggingIn -> if(loggingIn) false else am.session.error.value === AuthorizationManager.ErrorCode.LOGIN_FAILED_OTHER_ERROR
    }
    val pass: MutableLiveData<String> = MutableLiveData("")

    fun login(v: View) {
        Log.d("LOGIN","Authenticating...")
        am.login(email.value!!, pass.value!!)
    }
}



