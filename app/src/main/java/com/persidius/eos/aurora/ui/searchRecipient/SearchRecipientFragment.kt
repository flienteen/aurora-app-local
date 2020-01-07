package com.persidius.eos.aurora.ui.searchRecipient

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.database.entities.Recipient
import com.persidius.eos.aurora.database.entities.Uat
import com.persidius.eos.aurora.databinding.FragmentSearchRecipientBinding
import com.persidius.eos.aurora.rfidService.RFIDService
import com.persidius.eos.aurora.ui.cameraScanner.CameraScannerFragment
import com.persidius.eos.aurora.ui.recipient.RecipientFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class SearchRecipientFragment : Fragment() {
    private var rfidSubscriptions: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        val binding: FragmentSearchRecipientBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_search_recipient, container, false)
        binding.lifecycleOwner = this

        val adapter = SearchRecipientAdapter(itemClickListener = { r ->
            val navController = (activity as MainActivity).navController
            val args = Bundle()
            args.putString(RecipientFragment.ARG_RECIPIENT_ID, r.id)
            // TODO: Put SessionID if we have one here.

            navController.navigate(R.id.nav_recipient, args)
        })

        val viewModel = ViewModelProviders.of(this, SearchRecipientViewModelProviderFactory(adapter))
            .get(SearchRecipientViewModel::class.java)

        binding.model = viewModel

        viewModel.searchTerm.observe(this, Observer<String>{ term ->
            if(term.length > 1) {
                Log.d("SearchRecipient", term)
                // add wildcards
                Database.recipient.search("*$term*")
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.io())
                    .map {results ->
                        val locIds = results.map { r -> r.locId }.distinct()
                        val uatIds = results.map { r -> r.uatId }.distinct()

                        val locs = Database.loc.getByIds(locIds)
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .blockingGet()
                            .associateBy { l -> l.id }

                        val uats = Database.uat.getByIds(uatIds)
                            .subscribeOn(Schedulers.io())
                            .blockingGet()
                            .associateBy { u -> u.id }

                        results.map { r -> Triple(r, uats.getValue(r.uatId), locs.getValue(r.locId))}
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ({
                            results ->
                        viewModel.results.value = results
                        viewModel.adapter.setData(results)
                        Log.d("SearchRecipient", "${results.size} results")

                    }, { t -> Log.e("SearchRecipient", "Search for recipient errored", t)})
            } else {
                viewModel.results.postValue(listOf())
            }
        })

        binding.resultList.adapter = viewModel.adapter
        binding.resultList.layoutManager = LinearLayoutManager(context)
        viewModel.results.observe(this, Observer<List<Triple<Recipient, Uat, Loc>>> { data ->
            viewModel.adapter.setData(data)
        })
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rfidSubscriptions?.dispose()
        rfidSubscriptions = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
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
        val rfidService = (activity as MainActivity).rfidService

        rfidService.liveState.observe(this, Observer<RFIDService.State> {    newState ->
          // Change availability of button
            if(newState !== RFIDService.State.CONNECTED) {
                // Dispose any pending subscriptions
                if(rfidSubscriptions != null) {
                    rfidSubscriptions?.dispose()
                    rfidSubscriptions = null
                    Toast.makeText(activity, "Scanare cip anulată: scanner deconectat", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            rfidScanItem.isEnabled = (newState == RFIDService.State.CONNECTED)
        })

        rfidScanItem.setOnMenuItemClickListener {
            // Push toast stating that we will listen for 30sec
            Toast.makeText(activity, "Scan cip in așteptare", Toast.LENGTH_SHORT)
                .show()

            rfidSubscriptions?.dispose()
            rfidSubscriptions = null

            // Bind to rfidService
            rfidSubscriptions = rfidService.observableData
                .flatMapMaybe { tag ->
                    Database.recipientTag.getByTag(tag)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete {
                            // If maybe is empty.
                            Toast.makeText(activity,"Nu s-a găsit niciun recipient", Toast.LENGTH_SHORT)
                                .show()
                        }
                }
                .timeout(30, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({tag ->

                    rfidSubscriptions?.dispose()
                    rfidSubscriptions = null

                    val args = Bundle()
                    args.putString(RecipientFragment.ARG_RECIPIENT_ID, tag.recipientId)
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
