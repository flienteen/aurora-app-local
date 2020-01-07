package com.persidius.eos.aurora.ui.recipient

import android.widget.AutoCompleteTextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.persidius.eos.aurora.database.entities.*
import com.persidius.eos.aurora.util.Tuple2

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
    var originalTags: List<RecipientTag> = listOf()
    var tags: List<RecipientTag> = listOf()

    // tags used w/ the list
    val displayTags: MutableLiveData<List<Tuple2<MutableLiveData<String>, MutableLiveData<Boolean>>>> = MutableLiveData(listOf())

    val addressStreet: MutableLiveData<String> = MutableLiveData()
    val addressNumber: MutableLiveData<String> = MutableLiveData()
    val groupId: MutableLiveData<String> = MutableLiveData()

    val stream: MutableLiveData<String> = MutableLiveData()
    val size: MutableLiveData<String> = MutableLiveData()

    val comments: MutableLiveData<String> = MutableLiveData()
    val labels: MutableLiveData<List<String>> = MutableLiveData()

    val uat: MutableLiveData<String> = MutableLiveData()
    val loc: MutableLiveData<String> = MutableLiveData()

    var uats: MutableLiveData<List<Uat>> = MutableLiveData(listOf())
    var locs: MutableLiveData<List<Loc>> = MutableLiveData(listOf())
    var recipient: Recipient? = null
    val recLabels: MutableLiveData<List<RecommendedLabel>> = MutableLiveData(listOf())

    val streamSelectable: MutableLiveData<Boolean> = MutableLiveData(true)
    val sizeSelectable: MutableLiveData<Boolean> = MutableLiveData(true)
}