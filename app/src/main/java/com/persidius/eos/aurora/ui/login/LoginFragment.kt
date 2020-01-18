package com.persidius.eos.aurora.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.authorization.Role
import com.persidius.eos.aurora.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentLoginBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.lifecycleOwner = this
        val activity: MainActivity = activity as MainActivity;
        val viewModel = ViewModelProviders.of(this, LoginViewModelProviderFactory(activity.am)).get(LoginViewModel::class.java)
        binding.model = viewModel
        activity.am.session.signedIn.observe(this, Observer<Boolean> { isSignedIn -> navigateAfterLogin(isSignedIn) })
        return binding.root
    }

    private fun navigateAfterLogin(isSignedIn: Boolean) {
        if (!isSignedIn) {
            return
        }
        val activity: MainActivity = activity as MainActivity
        val loginObs = Observer<AuthorizationManager.SessionToken?> { sessionToken ->
            if (sessionToken != null) {
//                if (sessionToken.hasRole(Role.LOGISTICS_VIEW_TASK)) {
//                    activity.navController.navigate(R.id.nav_searchTask)
//                } else
                if (sessionToken.hasRole(Role.LOGISTICS_VIEW_RECIPIENT)) {
                    activity.navController.navigate(R.id.nav_searchRecipient)
                } else if (sessionToken.hasRole(Role.LOGISTICS_VIEW_USER)) {
                    activity.navController.navigate(R.id.nav_searchUser)
                } else if (sessionToken.hasRole(Role.LOGISTICS_VIEW_GROUPS)) {
                    activity.navController.navigate(R.id.nav_searchUser)
                }
            }
        }
        activity.am.session.sessionToken.observe(this, loginObs)
    }
}
