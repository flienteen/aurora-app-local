package com.persidius.eos.aurora.ui.recipient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecipientViewModel: ViewModel() {

    // All props of the recipient?

    // What are the props & what can we edit?

    // so we can edit the following:
    // show county!

    // 1. uat/locId
    // 6. addrStr/adrNr (based on uat/loc) - spinner
    // 2. posLat/posLng (under the hood)
    // 3. size
    // 4. tags
    // 5. stream (selected earlier?) - spinner
    // this is it.

    // Tags represented by slots (index -1), e.g. slot 1 = index 0; there is no slot 0.
    val tags: MutableLiveData<List<String>> = MutableLiveData()

    val addressStreet: MutableLiveData<String> = MutableLiveData()
    val addressNumber: MutableLiveData<String> = MutableLiveData()

    val stream: MutableLiveData<String> = MutableLiveData()
    val size: MutableLiveData<String> = MutableLiveData()

    val comments: MutableLiveData<String> = MutableLiveData()
    val labels: MutableLiveData<List<String>> = MutableLiveData()

    fun setActiveTag(slot: Int) {
        // Set the active editing tag.
    }

}