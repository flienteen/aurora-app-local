package com.persidius.eos.aurora.ui.collection

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.viewModelScope
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.core.collection.DuplicateCollectionError
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.databinding.FragmentCollectionBinding
import com.persidius.eos.aurora.type.RecipientLifecycle
import com.persidius.eos.aurora.ui.util.FoldingArrayAdapter
import com.persidius.eos.aurora.util.AutoDisposeFragment
import com.persidius.eos.aurora.util.Preferences
import com.uber.autodispose.autoDispose
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

class CollectionFragment : AutoDisposeFragment() {
  companion object {
    private const val tag = "CollectionFragment"
  }
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {

    val binding: FragmentCollectionBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_collection, container, false)
    binding.lifecycleOwner = this
    val viewModel = CollectionViewModel();

    binding.vehicleLicensePlate.inputType = InputType.TYPE_NULL

    binding.vehicleLicensePlate.setAdapter(
      FoldingArrayAdapter(
        requireActivity(),
        android.R.layout.select_dialog_item,
        listOf("b-181-tst")
      )
    )

    Database.vehicle.rxFindAll()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.io())
      .autoDispose(this)
      .subscribe { it ->
        binding.vehicleLicensePlate.setAdapter(
          FoldingArrayAdapter(
            requireActivity(),
            android.R.layout.select_dialog_item,
            it.map { v -> v.vehicleLicensePlate }
          )
        )
      }
    binding.vehicleLicensePlate.validator = (binding.vehicleLicensePlate.adapter as FoldingArrayAdapter).getValidator(RecipientLifecycle.LABEL_ONLY.toString())
    viewModel.vehicleLicensePlate.value = Preferences.vehicleLicensePlate.value
    viewModel.vehicleLicensePlate.observe(viewLifecycleOwner, { newVehicleLicensePlate ->
      Preferences.vehicleLicensePlate.onNext(newVehicleLicensePlate)
    })

    binding.model = viewModel

    (activity as MainActivity).btSvc.tags
      .observeOn(Schedulers.io())
      .subscribeOn(Schedulers.io())
      .autoDispose(this)
      .subscribe tagSub@ { scannedTag ->
        Log.d(CollectionFragment.tag, "Got read on tag $scannedTag")

        try {
          (activity as MainActivity).collections.recordCollection(scannedTag, viewModel.vehicleLicensePlate.value ?: "").blockingAwait()
          viewModel.state.postValue(CollectionFragmentReadState.READ_OK)
          Completable.complete()
            .delay(5, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this)
            .subscribe {
              viewModel.state.value = CollectionFragmentReadState.WAITING_READ
            }
        } catch(t: Throwable) {

          if(t is RuntimeException && t.cause is DuplicateCollectionError) {
            Log.d(CollectionFragment.tag, "Collection duplicate", t)
            viewModel.state.postValue(CollectionFragmentReadState.READ_DUPLICATED)
          } else {
            Log.e(CollectionFragment.tag, "Error recording collection", t)
            viewModel.state.postValue(CollectionFragmentReadState.READ_OTHER_ERROR)
          }
        }
      }
    return binding.root
  }
}