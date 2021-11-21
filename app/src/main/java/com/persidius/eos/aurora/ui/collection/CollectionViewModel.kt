package com.persidius.eos.aurora.ui.collection

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.databinding.adapters.Converters.convertColorToDrawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.database.entities.*

class CollectionViewModel: ViewModel() {
  val state: MutableLiveData<CollectionFragmentReadState> = MutableLiveData(CollectionFragmentReadState.WAITING_READ)
  val vehicleLicensePlate: MutableLiveData<String> = MutableLiveData("")
}