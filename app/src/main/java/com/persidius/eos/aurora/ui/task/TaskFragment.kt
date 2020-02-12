package com.persidius.eos.aurora.ui.task

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Task
import com.persidius.eos.aurora.database.entities.TaskPatch
import com.persidius.eos.aurora.databinding.FragmentTaskBinding
import com.persidius.eos.aurora.util.ArrayOp
import com.persidius.eos.aurora.util.Tuple2
import com.persidius.eos.aurora.util.Tuple3
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

        loadData(taskId)
        return binding.root
    }

    @SuppressLint("CheckResult")
    private fun loadData(taskId: Int) {
        Database.task.getById(taskId)
            .subscribeOn(Schedulers.io())
//            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ data ->
                viewModel = ViewModelProviders.of(this).get(TaskViewModel::class.java)

                val task = data
                viewModel.task = data
                viewModel.comments.value = task.comments
                viewModel.recipients.value = task.recipients
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
        if (changes.hasChanges()) {
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.task_menu, menu)
        menu.findItem(R.id.action_done).setOnMenuItemClickListener {
            onSave()
            true
        }
        super.onCreateOptionsMenu(menu, inflater)
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
                changes.recipients
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
            posLat = r.posLat,
            posLng = r.posLng,
            updatedAt = r.updatedAt,
            updatedBy = r.updatedBy,
            status = r.status,
            uatId = r.uatId,
            locId = r.locId,
            countyId = r.countyId,
            groups = r.groups,
            users = r.users,
            recipients = p.recipients ?: r.recipients
        )

        return Database.task.insert(listOf(newTask)).subscribeOn(Schedulers.io())
    }

    private data class TaskChangedValues(val comments: String? = null, val recipients: List<String> = listOf()) {
        fun hasChanges() = comments != null || recipients.isNotEmpty()
    }

    // Generates a patch if necessary.
    private fun getChanges(): TaskChangedValues {
        val vmComments = viewModel.comments.value!!
        val vmRecipients = viewModel.recipients.value!!

        if (viewModel.task == null) {
            return TaskChangedValues()
        }
        val r = viewModel.task!!
        val ret = TaskChangedValues(
            comments = if (r.comments == vmComments) null else vmComments,
            recipients = if (r.recipients == vmRecipients) listOf() else vmRecipients
        )
        Log.d("TASK", "Changes: $ret")
        return ret
    }

}
