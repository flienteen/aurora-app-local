package com.persidius.eos.aurora.ui.tasks

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.MenuItemCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.authorization.Role
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.database.entities.Task
import com.persidius.eos.aurora.database.entities.Uat
import com.persidius.eos.aurora.databinding.FragmentTasksBinding
import com.persidius.eos.aurora.ui.components.MultiSelectionSpinner
import com.persidius.eos.aurora.ui.components.MultiSelectionSpinner.OnSpinnerEventsListener
import com.persidius.eos.aurora.ui.task.TaskFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class TasksFragment : Fragment() {

    private lateinit var viewModel: TasksViewModel
    private lateinit var mainActivity: MainActivity

    private var statusSpinner: MultiSelectionSpinner? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)
        val binding = DataBindingUtil.inflate<FragmentTasksBinding>(inflater, R.layout.fragment_tasks, container, false)
        binding.lifecycleOwner = this

        val adapter = TasksAdapter(itemClickListener = { r ->
            if (isTaskEditable(r)) {
                val args = Bundle()
                args.putInt(TaskFragment.ARG_TASK_ID, r.id)
                mainActivity.navController.navigate(R.id.nav_task, args)
            }
        })

        viewModel = ViewModelProviders.of(this, TasksViewModelProviderFactory(adapter)).get(TasksViewModel::class.java)

        binding.model = viewModel

        viewModel.searchTerm.observe(this, Observer<String> { term ->
            doSearch(term)
        })

        binding.resultList.adapter = viewModel.adapter
        binding.resultList.layoutManager = LinearLayoutManager(context)
        viewModel.results.observe(this, Observer<List<Triple<Task, Uat, Loc>>> { data ->
            viewModel.adapter.setData(data)
        })

        return binding.root
    }

    private fun isTaskEditable(task: Task): Boolean {
        val token = mainActivity.am.session.tokenValid.value
        val email = mainActivity.am.session.email.value
        return token != null && token.hasRole(Role.LOGISTICS_EDIT_TASK) && task.assignedTo == email
    }

    private fun getStatusSearchTerm(): String {
        val statuses = statusSpinner?.selectedStrings
        var searchTerms = ""
        if (statuses != null && statuses.isNotEmpty()) {
            statuses.forEach { s -> searchTerms += "${s.toUpperCase()} OR " }
            searchTerms = searchTerms.dropLast(4)
        }
        return searchTerms
    }

    @SuppressLint("CheckResult")
    private fun doSearch(term: String) {
        if (term.length > 1) {
            val statusTerm = getStatusSearchTerm()
            val searchTerms = "(*$term*) ($statusTerm)"
            Log.d("SearchTask", searchTerms)
            Database.task.search(searchTerms)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .map { results ->
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

                    results.map { r -> Triple(r, uats.getValue(r.uatId), locs.getValue(r.locId)) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ results ->
                    viewModel.results.value = results
                    viewModel.adapter.setData(results)
                    Log.d("SearchTask", "${results.size} results")
                }, { t -> Log.e("SearchTask", "Search for task errored", t) })
        } else {
            viewModel.results.postValue(listOf())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_task_menu, menu)
        initStatusSpinner(menu)
        val itemOpenMap = menu.findItem(R.id.action_open_map)
        itemOpenMap.setOnMenuItemClickListener {
            mainActivity.navController.navigate(R.id.nav_taskMap)
            true
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun initStatusSpinner(menu: Menu) {
        val item = menu.findItem(R.id.action_status)
        val spinner = MenuItemCompat.getActionView(item) as MultiSelectionSpinner // get the spinner
        val statuses: MutableList<String> = ArrayList()
        statuses.add("New")
        statuses.add("Revisit")
        statuses.add("Unresolved")
        statuses.add("Resolved")
        spinner.setItems(statuses)

        spinner.addSpinnerListener(object : OnSpinnerEventsListener {
            override fun onSpinnerClosed() {
                doSearch(viewModel.searchTerm.value!!)
            }

            override fun onSpinnerOpened() {
            }
        })
        statusSpinner = spinner
    }

}
