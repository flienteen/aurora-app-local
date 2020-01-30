package com.persidius.eos.aurora.ui.login

import android.os.Bundle
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
        val activity: MainActivity = activity as MainActivity
        val viewModel = ViewModelProviders.of(this, LoginViewModelProviderFactory(activity.am)).get(LoginViewModel::class.java)
        binding.model = viewModel
        initNavigation()
        return binding.root
    }

    private fun initNavigation() {
        val activity = activity as MainActivity
        activity.am.session.tokenValid.observe(this, Observer { tkn ->
            if (tkn != null && activity.am.noError()) {
                if (tkn.hasRole(Role.LOGISTICS_VIEW_TASK)) {
                    activity.navController.navigate(R.id.nav_searchTask)
                } else if (tkn.hasRole(Role.LOGISTICS_VIEW_RECIPIENT)) {
                    activity.navController.navigate(R.id.nav_searchRecipient)
                } else if (tkn.hasRole(Role.LOGISTICS_VIEW_USER)) {
                    activity.navController.navigate(R.id.nav_searchUser)
                } else if (tkn.hasRole(Role.LOGISTICS_VIEW_GROUPS)) {
                    activity.navController.navigate(R.id.nav_searchUser)
                }
            }
        })
    }

}
