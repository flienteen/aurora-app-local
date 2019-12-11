package com.persidius.eos.aurora.ui.login

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.persidius.eos.aurora.MainActivity

import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentLoginBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.lifecycleOwner = this

        Log.d("LOGIN", (activity as MainActivity).am.session.email.toString())
        val viewModel = ViewModelProviders
            .of(this, LoginViewModelProviderFactory<LoginViewModel>((activity as MainActivity).am))
            .get(LoginViewModel::class.java)
        binding.model = viewModel

        return binding.root
    }
}
