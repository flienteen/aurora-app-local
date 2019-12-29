package com.persidius.eos.aurora.ui.searchRecipient

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.database.entities.Recipient
import com.persidius.eos.aurora.database.entities.Uat
import com.persidius.eos.aurora.databinding.FragmentLoginBinding
import com.persidius.eos.aurora.databinding.FragmentSearchRecipientBinding
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SearchRecipientFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        val binding: FragmentSearchRecipientBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_search_recipient, container, false)
        binding.lifecycleOwner = this

        val viewModel = ViewModelProviders.of(this)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_recipient_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
}
