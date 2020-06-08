package com.persidius.eos.aurora.ui.searchRecipient


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.bluetooth.BTService
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.databinding.FragmentSearchRecipientBinding
import com.persidius.eos.aurora.ui.cameraScanner.CameraScannerFragment
import com.persidius.eos.aurora.ui.recipient.RecipientFragment
import com.persidius.eos.aurora.util.AutoDisposeFragment
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


open class SearchRecipientFragment : AutoDisposeFragment() {
    private var btSubscriptions: Disposable? = null
    private var isModal = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val binding: FragmentSearchRecipientBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_search_recipient, container, false)
        binding.lifecycleOwner = this

        val adapter = SearchRecipientAdapter(itemClickListener = { r ->
            val navController = (activity as MainActivity).navController
            val args = Bundle()
            args.putString(RecipientFragment.ARG_RECIPIENT_ID, r.eosId)
            navController.navigate(R.id.nav_recipient, args)
        })

        val viewModel = ViewModelProvider(this, SearchRecipientViewModelProviderFactory(adapter)).get(SearchRecipientViewModel::class.java)
        binding.model = viewModel

        viewModel.searchTerm.observe(viewLifecycleOwner, Observer { term ->
            if (term.length > 1) {
                Log.d("SearchRecipient", term)
                Database.recipient.search("*$term*")
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .map { results ->
                    Log.d("RecipientSearch", results.size.toString())
                    val locIds = results.map { r -> r.locId }.distinct()
                    val uatIds = results.map { r -> r.uatId }.distinct()

                    Log.d("RecipientSearch", locIds.joinToString(","))
                    Log.d("RecipientSearch", uatIds.joinToString(","))
                    val locs = Database.loc.getByIds(locIds)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .blockingGet()
                        .associateBy { l -> l.id }

                    val uats = Database.uat.getByIds(uatIds)
                        .subscribeOn(Schedulers.io())
                        .blockingGet()
                        .associateBy { u -> u.id }

                    results.map { r -> Triple(r, uats.getValue(r.uatId), locs.getValue(r.locId)) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(this)
                .subscribe({ results ->
                    viewModel.results.value = results
                    viewModel.adapter.setData(results)
                    Log.d("SearchRecipient", "${results.size} results")

                }, { t -> Log.e("SearchRecipient", "Search for recipient errored", t) })
            } else {
                viewModel.results.postValue(listOf())
            }
        })

        binding.resultList.adapter = viewModel.adapter
        binding.resultList.layoutManager = LinearLayoutManager(context)
        viewModel.results.observe(viewLifecycleOwner, Observer { data ->
            viewModel.adapter.setData(data)
        })
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        btSubscriptions?.dispose()
        btSubscriptions = null
    }

    @SuppressLint("AutoDispose")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isModal) {
            return
        }
        inflater.inflate(R.menu.search_recipient_menu, menu)
        menu.findItem(R.id.action_scan_camera).setOnMenuItemClickListener {
            // we want to pop stack when doing camera scan
            val args = Bundle()
            args.putBoolean(CameraScannerFragment.ARG_POP_NAV, true)
            (activity as MainActivity).navController.navigate(R.id.nav_cameraScanner, args)
            true
        }

        // RFID Scan Logic
        val rfidScanItem = menu.findItem(R.id.action_scan_rfid)
        val btSvc = (activity as MainActivity).btSvc

        btSvc.stateLiveData.observe(this, Observer<BTService.State> { newState ->
            // Change availability of button
            if (newState !== BTService.State.CONNECTED) {
                // Dispose any pending subscriptions
                if (btSubscriptions != null) {
                    btSubscriptions?.dispose()
                    btSubscriptions = null
                    Toast.makeText(activity, "Scanare cip anulată: scanner deconectat", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            rfidScanItem.isEnabled = (newState == BTService.State.CONNECTED)
        })

        rfidScanItem.setOnMenuItemClickListener {
            // Push toast stating that we will listen for 30sec
            Toast.makeText(activity, "Scan cip in așteptare", Toast.LENGTH_SHORT)
                .show()

            btSubscriptions?.dispose()
            btSubscriptions = null

            // Bind to rfidService
            btSubscriptions = btSvc.tags
                .flatMapMaybe { tag ->
                    Database.recipientTags.getByTag(tag)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete {
                            // If maybe is empty.
                            Toast.makeText(activity, "Nu s-a găsit niciun recipient", Toast.LENGTH_SHORT)
                                .show()
                        }
                        .flatMap { t ->
                            Database.recipient.getById(t.recipientId ?: "")
                                .subscribeOn(Schedulers.io())
                        }
                }
                .timeout(30, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ recipient ->
                    Log.d("recipientFragment", recipient.eosId)
                    btSubscriptions?.dispose()
                    btSubscriptions = null

                    val args = Bundle()
                    args.putString(RecipientFragment.ARG_RECIPIENT_ID, recipient.eosId)
                    (activity as MainActivity).navController.navigate(R.id.nav_recipient, args)
                }, { err ->
                    Log.e("RECIPIENT", "Error scanning tag", err)
                    Toast.makeText(activity, "Scan cip terminat", Toast.LENGTH_SHORT)
                        .show()
                })
            true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }
}
