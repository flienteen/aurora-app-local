package com.persidius.eos.aurora.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentLoginBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.lifecycleOwner = this
        val activity: MainActivity = activity as MainActivity
        val viewModel = ViewModelProvider(this, LoginViewModelProviderFactory(activity.authMgr)).get(LoginViewModel::class.java)
        binding.model = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigateIfLoggedIn()
    }

    private fun navigateIfLoggedIn() {
        val activity = activity as MainActivity
        // 1. Figure out if we're logged in (session.valid = true)
        Log.d("LoginFragment", "Navigate")
        if(activity.authMgr.session.isValid.blockingFirst()) {
            //if(activity.featMgr.searchBinEnabled.blockingFirst()) {
                Log.d("LoginFragment", "all resolved, navigating")
                // navigate to recipient mgmt
                activity.navController.navigate(R.id.action_nav_login_to_nav_searchRecipient)
         //   }
            /* TODO: else {
                // Navigate to not enough permissions, please logout.
            } */
        }
        Log.d("loginFragment", "Not navigating")
    }
}
