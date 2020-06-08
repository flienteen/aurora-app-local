package com.persidius.eos.aurora.ui.login

import android.app.AlertDialog
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.persidius.eos.aurora.auth.AuthManager
import com.persidius.eos.aurora.util.AutoDisposeViewModel
import com.persidius.eos.aurora.util.Preferences
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers

class LoginViewModel(private val authMgr: AuthManager): AutoDisposeViewModel() {
    val userLocked: MutableLiveData<Boolean> = MutableLiveData(Preferences.amLocked.value!!)
    val username: MutableLiveData<String> = MutableLiveData(Preferences.amUsername.value!!)
    val formEnabled: MutableLiveData<Boolean> = MutableLiveData(authMgr.session.isValid.blockingFirst().not())
    val password: MutableLiveData<String> = MutableLiveData("")

    fun login(v: View)  {
        if(formEnabled.value == false) {
            // we're already logging in?
            return
        }
        formEnabled.value = false

        if(username.value?.trim()?.length ?: 0 < 1 ||
                password.value?.trim()?.length ?: 0 < 1) {

            val builder = AlertDialog.Builder(v.context)
                builder.setTitle("Eroare")
                builder.setMessage("Campul de email si de parola trebuie completate")
            builder.show()
            formEnabled.value = true
            return
        }

        authMgr.login(username.value!!, password.value!!)
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally {
            password.value = ""
        }
        .autoDispose(this)
        .subscribe({
            formEnabled.value = false
        }, {
            formEnabled.value = true
            val err = authMgr.parseTokenError(it)
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle("Eroare")

            when (err.type) {
                AuthManager.LoginErrorType.Network -> {
                    builder.setMessage("Nu s-a putut contacta serverul.")
                }
                AuthManager.LoginErrorType.InvalidCredentials -> {
                    builder.setMessage("Combinatia de utilizator/parola este invalida.")
                }
                AuthManager.LoginErrorType.Other -> {
                    builder.setMessage("Eroare necunoscuta. Reincearca.")
                }
            }
            builder.show()
            Log.d("LOGIN", err.toString(), err.cause)
        })
    }
}



