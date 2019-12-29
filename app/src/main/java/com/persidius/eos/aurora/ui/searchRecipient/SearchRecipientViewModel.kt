package com.persidius.eos.aurora.ui.searchRecipient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.database.entities.Recipient
import com.persidius.eos.aurora.database.entities.Uat

class SearchRecipientViewModel: ViewModel() {
    val searchTerm: MutableLiveData<String> = MutableLiveData("")
    val results: MutableLiveData<List<Triple<Recipient, Uat, Loc>>> = MutableLiveData()
    val adapter = SearchRecipientAdapter()
}