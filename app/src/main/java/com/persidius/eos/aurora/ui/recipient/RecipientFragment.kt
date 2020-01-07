package com.persidius.eos.aurora.ui.recipient

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.*
import com.persidius.eos.aurora.databinding.FragmentRecipientBinding
import com.persidius.eos.aurora.ui.util.FoldingArrayAdapter
import com.persidius.eos.aurora.ui.util.GroupIdAdapter
import com.persidius.eos.aurora.util.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.sentry.Sentry
import kotlinx.android.synthetic.main.app_bar.*

class RecipientFragment: Fragment() {
    companion object {
        const val ARG_RECIPIENT_ID = "recipientId"
        const val ARG_SESSION_ID = "sessionId"
    }

    private lateinit var viewModel: RecipientViewModel
    private var sessionId: Int? = null

    private fun onBack() {
        val changes = getChanges()
        if(changes.hasChanges()) {
            // Pop an alert dialogue.
            // on "OK" continue w/ nav back
            // else forget about it
            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle("Schimbări Nesalvate")
            builder.setMessage("Există modificări făcute la recipientul actual. Sigur dorești să navighezi inapoi fără le salvezi?")
            builder.setPositiveButton("Da") { _, _ ->
                (activity as MainActivity).navController.popBackStack()
            }

            builder.setNegativeButton("Nu") {_, _ -> }
            builder.show()
        } else {
            (activity as MainActivity).navController.popBackStack()
        }
    }

    private var subscription: Disposable? = null
    @SuppressLint("CheckResult")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        // Get the arguments
        val recipientId = arguments?.getString(ARG_RECIPIENT_ID)
        sessionId = arguments?.getInt(ARG_SESSION_ID)
        if(sessionId == 0) { sessionId = null }

        val mainActivity = (requireActivity() as MainActivity)
        val toolbar = mainActivity.toolbar
        toolbar?.title = recipientId ?: "Recipient"

        // Generate the binder
        val binding: FragmentRecipientBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_recipient, container, false)
        binding.lifecycleOwner = this

        if(recipientId == null) {
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Eroare")
                .setMessage("Recipientul are valoarea nulă (recipientId = null)")
            builder.setNegativeButton("Am Înțeles") { _, _ ->
                (activity as MainActivity).navController.navigateUp()
            }
            builder.create().show()
            return binding.root
        }

        mainActivity.setOnBackListener { this@RecipientFragment.onBack() }
        mainActivity.onBackPressedDispatcher.addCallback {this@RecipientFragment.onBack() }

        Database.recipient.getById(recipientId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .flatMap { recipient ->
                Database.uat.getByCountyIds(listOf(recipient.countyId, 0))
                    .map { uats -> recipient then uats }
            }
            .flatMap { data ->
                Database.recipientTag.getByRecipientId(data.first.id)
                    .subscribeOn(Schedulers.io())
                    .map { tags -> data then tags }
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
            .flatMap {
                data ->
                Database.recLabel.getAll()
                    .subscribeOn(Schedulers.io())
                    .map { labels -> data then labels }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ data ->
                viewModel = ViewModelProviders
                .of(this)
                .get(RecipientViewModel::class.java)

                val recipient = data.first

                viewModel.recipient = data.first
                viewModel.addressStreet.value = recipient.addressStreet
                viewModel.addressNumber.value = recipient.addressNumber
                viewModel.comments.value = recipient.comments
                viewModel.labels.value = recipient.labels
                viewModel.tags = data.third

                // Clone by mapping & reinit-ing objects
                viewModel.originalTags = data.third.map {
                    rt -> RecipientTag(rt.tag, rt.recipientId, rt.slot)
                }

                viewModel.uat.observe(this, Observer<String> { newVal ->

                    val uat = viewModel.uats.value?.filter { u -> u.name == newVal }?.firstOrNull()
                    if(uat != null) {
                        Database.loc.getByUatIds(listOf(uat.id))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { locs ->
                                viewModel.locs.value = locs

                                // If loc is not found in locs array, choose.
                                if(!locs.any { l -> l.name == viewModel.loc.value }) {
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
                    binding.uat.setAdapter(
                        FoldingArrayAdapter(
                            activity!!,
                            android.R.layout.select_dialog_item,
                            newVal.map { u -> u.name })
                    )
                    binding.uat.validator = (binding.uat.adapter as FoldingArrayAdapter).getValidator("Nedefinit")
                })

                viewModel.locs.observe(this, Observer<List<Loc>> { newVal ->
                    binding.loc.setAdapter(
                        FoldingArrayAdapter(
                            activity!!,
                            android.R.layout.select_dialog_item,
                            newVal.map { l -> l.name }
                        )
                    )
                    binding.loc.validator = (binding.loc.adapter as FoldingArrayAdapter).getValidator("Nedefinit")
                })


                viewModel.uats.value = data.second
                viewModel.uat.value = data.fifth[0].name
                viewModel.loc.value = data.fourth[0].name
                viewModel.recLabels.value = data.sixth

                viewModel.size.observe(this, Observer<String> { newSize ->
                    val slotCount = when(newSize) {
                        "120L", "240L" -> 1
                        "1.100L" -> 2
                        else -> 2
                    }


                    // init a mutable list
                    val displayTags = Array(slotCount) { Tuple2(MutableLiveData(""), MutableLiveData(false)) }
                    viewModel.tags.forEach { t ->
                        if(t.slot <= slotCount) {
                            displayTags[t.slot - 1].first.value = t.tag
                        }
                    }

                    displayTags[0].second.value = true
                    viewModel.displayTags.value = displayTags.asList()

                    binding.tagList.adapter = TagListAdapter(
                        context!!,
                        displayTags.asList() as List<Tuple2<LiveData<String>, LiveData<Boolean>>>,
                        this,
                        onTagClick = { slot ->
                            Log.d("tag", "Selecting slot ${slot}")
                            val displayTags = viewModel.displayTags.value
                            if(displayTags != null) {
                                // select tag in slot X, deselect everything else
                                for (tag in displayTags) {
                                    tag.second.postValue(false)
                                }
                                displayTags[slot - 1].second.postValue(true)
                            }
                        }
                    )
                })

                binding.stream.inputType = InputType.TYPE_NULL
                binding.stream.setAdapter(
                    FoldingArrayAdapter(
                        activity!!,
                        android.R.layout.select_dialog_item,
                        RecipientStream.values()
                            .filter { v -> v !== RecipientStream.UNK }
                            .map { v -> v.displayName })
                )
                binding.stream.validator = (binding.stream.adapter as FoldingArrayAdapter).getValidator(RecipientStream.UNK.displayName)

                binding.size.inputType = InputType.TYPE_NULL
                binding.size.setAdapter(
                    FoldingArrayAdapter(
                        activity!!,
                        android.R.layout.select_dialog_item,
                        RecipientSize.getVisibleSizes()
                    )
                )
                binding.size.validator = (binding.size.adapter as FoldingArrayAdapter).getValidator(RecipientSize.SIZE_120L.displayName)

                // Convert recipient stream to display name
                viewModel.stream.value = try { RecipientStream.valueOf(recipient.stream).displayName } catch (t: Throwable) { "" }
                viewModel.size.value = recipient.size

                subscription = (activity as MainActivity).rfidService.observableData
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { newVal ->
                    // find the selectedTag slot
                    viewModel.displayTags.value?.forEachIndexed { ix, t ->
                        if(t.second.value == true) {
                            t.first.value = newVal

                            // Find oldTag by slot & update it (or insert it on the contrary).
                            val slot = ix + 1
                            val oldTag = viewModel.tags.find { t -> t.slot == slot }
                            val newTag = RecipientTag(newVal, recipient.id, slot)
                            viewModel.tags = if(oldTag != null) { viewModel.tags.filter { t -> t.slot != slot }
                            } else { viewModel.tags } + newTag
                        }
                    }
                    val selected = viewModel.displayTags.value?.find { t -> t.second.value == true }

                    selected?.first?.value = newVal
                }

                viewModel.groupId.value = recipient.groupId ?: ""
                val groupAdapter = GroupIdAdapter(context!!, android.R.layout.select_dialog_item)
                binding.group.setAdapter(groupAdapter)
                binding.group.validator = groupAdapter.getValidator()

                binding.model = viewModel
            }, { t ->
                Log.e("RecipientFragment", "Error", t)
                Sentry.capture(t)
                val builder = AlertDialog.Builder(context!!)
                builder.setTitle("Eroare")
                builder.setMessage("A avut loc o eroare. ")
                builder.setNeutralButton("Am ințeles") { _, _ ->
                    (activity as MainActivity).navController.popBackStack()
                }
                builder.setOnCancelListener{
                    (activity as MainActivity).navController.popBackStack()
                }
                builder.show()
            }, {
                // OnComplete means some of the data wasn't loaded.
                val builder = AlertDialog.Builder(context!!)
                builder.setTitle("Recipient Inexistent")
                builder.setMessage("Recipientul cu codul $recipientId nu a fost găsit")
                builder.setNeutralButton("Am ințeles") { _, _ ->
                    (activity as MainActivity).navController.popBackStack()
                }
                builder.setOnCancelListener{
                    (activity as MainActivity).navController.popBackStack()
                }
                builder.show()
            })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity!! as MainActivity).clearOnBackListener()
        subscription?.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.done_menu, menu)

        menu.findItem(R.id.action_done).setOnMenuItemClickListener {
            onSave()
            true
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun onSave() {
        val changes = getChanges()


        // If no tags are scanned, warn the user
        val vmTags = viewModel.displayTags.value!!.mapIndexed { index, tuple2 ->
            Tuple2(index + 1, tuple2.first.value!!)
        }.filter { t -> t.second.isNotEmpty() }

        if(vmTags.isEmpty()) {
            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle("Atenție")
            builder.setMessage("Nu a fost scanat niciun tag pentru acest recipient")
            builder.setPositiveButton("Am înțeles") { _, _ ->
                // do all the save stuff
                if(changes.hasChanges()) {
                    executeSave(changes)
                }
            }
            builder.setNegativeButton("Anulează") { _, _ -> }
            builder.show()
        } else {
            if(changes.hasChanges()) {
                executeSave(changes)
            }
        }
    }

    // Called immediately after doSave finalises gathering user info
    // and IF necessary.
    private fun executeSave(
        changes: RecipientChangedValues
    ) {
        // Step 1> Obtain a SessionID
        Log.d("RECIPIENT", "sessionId = $sessionId")
        val obs = if(sessionId == null) {
            Database.session.createSession()
                .subscribeOn(Schedulers.io())
        } else {
            Database.session.getById(sessionId!!)
                .subscribeOn(Schedulers.io())
        }

        val sub = obs.flatMapSingle {session ->
            val mainActivity = (requireActivity() as MainActivity)
            val patch = RecipientPatch(
                0,
                viewModel.recipient!!.id,
                session.id,
                System.currentTimeMillis() / 1000,
                mainActivity.am.session.email.value!!,
                mainActivity.lat,
                mainActivity.lng,
                changes.stream,
                changes.size,
                changes.addressStreet,
                changes.addressNumber,
                changes.uatId,
                changes.locId,
                changes.comments,
                null,
                listOf(),
                changes.tags,
                if(changes.groupId == "") "000000" else changes.groupId
            )
            Log.d("RECIPIENT", "Creating patch")

            Database.recipientPatch.insert(patch)
                .subscribeOn(Schedulers.io())
                .map { id -> Tuple3(id, session, patch)}
        }
            .flatMapCompletable {
                Log.d("RECIPIENT", "Created patch id $it")

                Database.session.updateRefCount(it.second.id, -1)
                    .observeOn(Schedulers.io())
                    .andThen(applyPatch(it.third))
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( {
                // we're done saving it now.
                // Save is done. Nav back.
                (requireActivity() as MainActivity).navController.popBackStack()
            }, { t ->
                Log.d("RECIPIENT", "ERROR")
        })
    }

    private fun applyPatch(p: RecipientPatch): Completable {
        // Load up the Recipient and fuck with it.
        val r = viewModel.recipient!!

        // TODO: Compute labels array.
        // Sometime in the future. We don't support labels as of  yet.

        val newRecipient = Recipient(
            id = r.id,
            addressNumber = p.addressNumber ?: r.addressNumber,
            addressStreet = p.addressStreet ?: r.addressStreet,
            uatId = p.uatId ?: r.uatId,
            locId = p.locId ?: r.locId,
            size = p.size ?: r.size,
            stream = p.stream ?: r.stream,
            posLat = p.posLat ?: r.posLat,
            posLng =  p.posLng ?: r.posLng,
            active = p.active ?: r.active,
            groupId = if(p.groupId == "000000") { null } else (p.groupId ?: r.groupId),
            labels = r.labels,
            updatedAt = r.updatedAt,
            comments = p.comments ?: r.comments,
            countyId = r.countyId
        )

        // for each op in the tags array perform independent DB ops.

        val insertTags = Observable.fromIterable(p.tags)
                .flatMapCompletable { op ->
                    when (op.first) {
                        ArrayOp.REMOVE ->
                            Database.recipientTag.deleteSlot(r.id, op.second)
                                .subscribeOn(Schedulers.io())
                        ArrayOp.ADD ->
                            Database.recipientTag.insert(
                                listOf(
                                    RecipientTag(
                                        op.third,
                                        r.id,
                                        op.second
                                    )
                                )
                            ).subscribeOn(Schedulers.io())
                    }
                }

        return Database.recipient.insert(listOf(newRecipient))
            .subscribeOn(Schedulers.io())
            .andThen(insertTags)
    }


    private data class RecipientChangedValues(
        val locId: Int? = null,
        val uatId: Int? = null,
        val size: String? = null,
        val stream: String? = null,
        val addressStreet: String? = null,
        val addressNumber: String? = null,
        val comments: String? = null,
        val groupId: String? = null,
        val tags: List<Triple<ArrayOp, Int, String>> = listOf()
    ) {
        fun hasChanges() = (locId ?: uatId ?: size ?: stream?:
            addressStreet ?: addressNumber ?: comments ?: groupId) != null || tags.isNotEmpty()
    }

    // Generates a patch if necessary.
    private fun getChanges(): RecipientChangedValues {
        // Size, Stream
        // UAT, Loc,
        // AddressStreet, AddressNumber
        // groupId
        // comments

        val vmUatId = viewModel.uats.value?.find { u -> u.name == viewModel.uat.value }?.id ?: 0
        val vmLocId = viewModel.locs.value?.find { l -> l.name == viewModel.loc.value }?.id ?: 0
        val vmSize = viewModel.size.value
        val vmStream = RecipientStream.fromDisplayName(viewModel.stream.value!!).name
        val vmAddressStreet = viewModel.addressStreet.value!!
        val vmAddressNumber = viewModel.addressNumber.value!!
        val vmComments = viewModel.comments.value!!
        val vmGroupId = viewModel.groupId.value!!

        val vmTags = viewModel.displayTags.value!!.mapIndexed { index, tuple2 ->
            Tuple2(index + 1, tuple2.first.value!!)
        }.filter { t -> t.second.isNotEmpty() }

        val tagChanges: ArrayList<Triple<ArrayOp, Int, String>> = ArrayList()
        for(tag in vmTags) {
            val slot = tag.first
            val value = tag.second

            val eqTag = viewModel.originalTags.find { t -> t.slot == slot }
            if(eqTag == null || eqTag.tag != value) {
                tagChanges.add(Triple(ArrayOp.ADD, slot, value))
            }
        }

        for(tag in viewModel.originalTags) {
            // if there's a tag in originalTags but not vmTags, then it was REMOVED
            val eqTag = vmTags.find { t -> t.first == tag.slot }
            if(eqTag == null) {
                tagChanges.add(Triple(ArrayOp.REMOVE, tag.slot, ""))
            }
        }

        // now we want to compare these
        if(viewModel.recipient == null) {
            return RecipientChangedValues()
        }

        val r = viewModel.recipient!!


        val recGroupId = r.groupId ?: ""

        val ret = RecipientChangedValues(
            uatId = if(r.uatId == vmUatId) null else vmUatId,
            locId = if(r.locId == vmLocId) null else vmLocId,
            size = if(r.size == vmSize) null else vmSize,
            stream = if(r.stream == vmStream) null else vmStream,
            comments = if(r.comments == vmComments) null else vmComments,
            addressStreet =  if(r.addressStreet == vmAddressStreet) null else vmAddressStreet,
            addressNumber = if(r.addressNumber == vmAddressNumber) null else vmAddressNumber,
            groupId = if(recGroupId == vmGroupId) null else vmGroupId,
            tags = tagChanges
        )

        Log.d("RECIPIENT", "Changes: ${ret}")

        return ret
    }

}