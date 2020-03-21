package com.persidius.eos.aurora.ui.task

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ListView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.database.entities.Task
import com.persidius.eos.aurora.database.entities.TaskPatch
import com.persidius.eos.aurora.database.entities.Uat
import com.persidius.eos.aurora.databinding.FragmentTaskBinding
import com.persidius.eos.aurora.ui.searchRecipient.SearchRecipientFragment
import com.persidius.eos.aurora.ui.util.FoldingArrayAdapter
import com.persidius.eos.aurora.util.Tuple3
import com.persidius.eos.aurora.util.then
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sentry.Sentry
import kotlinx.android.synthetic.main.app_bar.*


class TaskFragment : Fragment() {

    companion object {
        const val ARG_TASK_ID = "taskId"
        const val ARG_SESSION_ID = "sessionId"
    }

    private lateinit var viewModel: TaskViewModel
    private lateinit var mainActivity: MainActivity
    private lateinit var listView: ListView
    private var sessionId: Int? = null

    @SuppressLint("CheckResult")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val binding = DataBindingUtil.inflate<FragmentTaskBinding>(inflater, R.layout.fragment_task, container, false)
        binding.lifecycleOwner = this
        viewModel = ViewModelProviders.of(this).get(TaskViewModel::class.java)
        binding.model = viewModel

        val taskId = arguments?.getInt(ARG_TASK_ID)
        sessionId = arguments?.getInt(ARG_SESSION_ID)
        if (sessionId == 0) {
            sessionId = null
        }

        val toolbar = mainActivity.toolbar
        toolbar?.title = taskId?.toString() ?: "Task"

        if (taskId == null) {
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Eroare")
                .setMessage("Taskul are valoarea nulă (taskId = null)")
                .setNegativeButton("Am Înțeles") { _, _ ->
                    mainActivity.navController.navigateUp()
                }
                .create().show()
            return binding.root
        }

        mainActivity.setOnBackListener { this@TaskFragment.onBack() }
        mainActivity.onBackPressedDispatcher.addCallback { this@TaskFragment.onBack() }
        createRecipientListView(binding.root)
        loadData(taskId, binding)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.task_menu, menu)
        menu.findItem(R.id.action_done).setOnMenuItemClickListener {
            onSave()
            true
        }

        menu.findItem(R.id.action_add_recipient).setOnMenuItemClickListener {
            val recipientSearch = SearchRecipientFragment()
            recipientSearch.show(fragmentManager!!, "Recipienti")
            recipientSearch.dismissListener = DialogInterface.OnDismissListener {
                if (recipientSearch.dialogResult != null) {
                    val recipientId = recipientSearch.dialogResult!!.id
                    if (!getRecipients().contains(recipientId)) {
                        getRecipients().add(recipientId)
                    }
                    listView.invalidateViews()
                }
            }
            true
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun getRecipients(): MutableList<String> {
        return viewModel.recipients.value!!
    }

    private fun createRecipientListView(root: View) {
        listView = root.findViewById(R.id.recipients) as ListView
        val adapter = RecipientListAdapter(mainActivity, getRecipients())
        listView.adapter = adapter
    }

    @SuppressLint("CheckResult")
    private fun loadData(taskId: Int, binding: FragmentTaskBinding) {
        Database.task.getById(taskId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .flatMap { task ->
                Database.uat.getByCountyIds(listOf(task.countyId, 0))
                    .map { uats -> task then uats }
            }
            .flatMap { data ->
                Database.loc.getByIds(listOf(data.first.locId))
                    .subscribeOn(Schedulers.io())
                    .map { loc -> data then loc }
            }
            .flatMap { data ->
                Database.uat.getByIds(listOf(data.first.uatId))
                    .subscribeOn(Schedulers.io())
                    .map { uat -> data then uat }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ data ->
                viewModel = ViewModelProviders.of(this).get(TaskViewModel::class.java)

                val task = data.first
                viewModel.task = task
                viewModel.comments.value = task.comments
                viewModel.uats.value = data.second
                viewModel.loc.value = data.third[0].name
                viewModel.uat.value = data.fourth[0].name

                val set = LinkedHashSet(getRecipients())
                set.addAll(task.recipients)
                getRecipients().clear()
                getRecipients().addAll(set)
                listView.invalidateViews()

                viewModel.uat.observe(this, Observer<String> { newVal ->
                    val uat = viewModel.uats.value?.filter { u -> u.name == newVal }?.firstOrNull()
                    if (uat != null) {
                        Database.loc.getByUatIds(listOf(uat.id))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { locs ->
                                viewModel.locs.value = locs

                                // If loc is not found in locs array, choose.
                                if (!locs.any { l -> l.name == viewModel.loc.value }) {
                                    if (locs.size == 1) {
                                        viewModel.loc.value = locs.first().name
                                    } else {
                                        viewModel.loc.value = ""
                                    }
                                }
                            }
                    } else {
                        viewModel.locs.value = listOf()
                    }
                })

                viewModel.uats.observe(this, Observer<List<Uat>> { newVal ->
                    binding.uat.setAdapter(FoldingArrayAdapter(activity!!, android.R.layout.select_dialog_item, newVal.map { u -> u.name }))
                    binding.uat.validator = (binding.uat.adapter as FoldingArrayAdapter).getValidator("Nedefinit")
                })

                viewModel.locs.observe(this, Observer<List<Loc>> { newVal ->
                    binding.loc.setAdapter(FoldingArrayAdapter(activity!!, android.R.layout.select_dialog_item, newVal.map { l -> l.name }))
                    binding.loc.validator = (binding.loc.adapter as FoldingArrayAdapter).getValidator("Nedefinit")
                })

            }, { t ->
                Log.e("TaskFragment", "Error", t)
                Sentry.capture(t)
                AlertDialog.Builder(context!!)
                    .setTitle("Eroare")
                    .setMessage("A avut loc o eroare. ")
                    .setNeutralButton("Am ințeles") { _, _ ->
                        mainActivity.navController.popBackStack()
                    }
                    .setOnCancelListener {
                        mainActivity.navController.popBackStack()
                    }
                    .show()
            }, {
                // OnComplete means some of the data wasn't loaded.
                AlertDialog.Builder(context!!)
                    .setTitle("Task inexistent")
                    .setMessage("Taskul cu codul $taskId nu a fost găsit")
                    .setNeutralButton("Am ințeles") { _, _ ->
                        mainActivity.navController.popBackStack()
                    }
                    .setOnCancelListener {
                        mainActivity.navController.popBackStack()
                    }
                    .show()
            })
    }

    private fun onBack() {
        val changes = getChanges()
        if (activity != null && changes.hasChanges()) {
            // Pop an alert dialogue. on "OK" continue w/ nav back else forget about it
            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle("Schimbări Nesalvate")
            builder.setMessage("Există modificări făcute la taskul actual. Sigur dorești să navighezi inapoi fără le salvezi?")
            builder.setPositiveButton("Da") { _, _ ->
                mainActivity.navController.popBackStack()
            }
            builder.setNegativeButton("Nu") { _, _ -> }
            builder.show()
        } else {
            mainActivity.navController.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity.clearOnBackListener()
    }

    private fun onSave() {
        val changes = getChanges()
        if (changes.hasChanges()) {
            executeSave(changes)
        }
    }

    @SuppressLint("CheckResult")
    private fun executeSave(changes: TaskChangedValues) {
        // Step 1> Obtain a SessionID
        Log.d("TASK", "sessionId = $sessionId")
        val obs = if (sessionId == null) {
            Database.session.createSession().subscribeOn(Schedulers.io())
        } else {
            Database.session.getById(sessionId!!).subscribeOn(Schedulers.io())
        }

        obs.flatMapSingle { session ->
            val patch = TaskPatch(
                0,
                viewModel.task!!.gid ?: "",
                viewModel.task!!.id,
                System.currentTimeMillis().toInt(),
                session.id,
                changes.comments,
                changes.recipients,
                changes.uatId,
                changes.locId,
                changes.posLat,
                changes.posLng
            )
            Log.d("TASK", "Creating patch")

            Database.taskPatch.insert(patch)
                .subscribeOn(Schedulers.io())
                .map { id -> Tuple3(id, session, patch) }
        }.flatMapCompletable {
            Log.d("TASK", "Created patch id $it")
            Database.session.updateRefCount(it.second.id, -1)
                .observeOn(Schedulers.io())
                .andThen(applyPatch(it.third))
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                // we're done saving it now. Save is done. Nav back.
                mainActivity.navController.popBackStack()
            }, { t ->
                Log.d("TASK", "ERROR")
            })
    }

    private fun applyPatch(p: TaskPatch): Completable {
        val r = viewModel.task!!
        val newTask = Task(
            id = r.id,
            gid = r.gid,
            comments = p.comments ?: r.comments,
            assignedTo = r.assignedTo,
            validFrom = r.validFrom,
            validTo = r.validTo,
            posLat = p.posLat ?: r.posLat,
            posLng = p.posLng ?: r.posLng,
            updatedAt = r.updatedAt,
            updatedBy = r.updatedBy,
            status = r.status,
            uatId = p.uatId ?: r.uatId,
            locId = p.locId ?: r.locId,
            countyId = r.countyId,
            groups = r.groups,
            users = r.users,
            recipients = p.recipients ?: r.recipients
        )

        return Database.task.insert(listOf(newTask)).subscribeOn(Schedulers.io())
    }

    private data class TaskChangedValues(
        val comments: String? = null,
        val recipients: List<String> = listOf(),
        val locId: Int? = null,
        val uatId: Int? = null,
        val posLat: Double? = null,
        val posLng: Double? = null
    ) {
        fun hasChanges() = comments != null || locId != null || uatId != null || recipients.isNotEmpty()
    }

    // Generates a patch if necessary.
    private fun getChanges(): TaskChangedValues {
        val vmComments = viewModel.comments.value!!
        val vmRecipients = getRecipients()
        val vmUatId = viewModel.uats.value?.find { u -> u.name == viewModel.uat.value }?.id ?: 0
        val vmLocId = viewModel.locs.value?.find { l -> l.name == viewModel.loc.value }?.id ?: 0

        if (viewModel.task == null) {
            return TaskChangedValues()
        }
        //val r = viewModel.task!!
        val ret = TaskChangedValues(
            comments = vmComments, //if (r.comments == vmComments) null else vmComments,
            recipients = vmRecipients, //if (r.recipients == vmRecipients) listOf() else vmRecipients,
            uatId = vmUatId, //if (r.uatId == vmUatId) null else vmUatId,
            locId = vmLocId //if (r.locId == vmLocId) null else vmLocId
        )
        Log.d("TASK", "Changes: $ret")
        return ret
    }

}
