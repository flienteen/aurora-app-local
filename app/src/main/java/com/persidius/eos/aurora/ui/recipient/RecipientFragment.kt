package com.persidius.eos.aurora.ui.recipient

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.analytics.ktx.logEvent
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.*
import com.persidius.eos.aurora.databinding.FragmentRecipientBinding
import com.persidius.eos.aurora.type.RecipientLifecycle
import com.persidius.eos.aurora.ui.util.FoldingArrayAdapter
import com.persidius.eos.aurora.util.*
import com.uber.autodispose.autoDispose
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sentry.Sentry

class RecipientFragment: AutoDisposeFragment() {
  companion object {
    const val ARG_RECIPIENT_ID = "recipientId"
    const val tag = "RecipientFragment"
  }

  private lateinit var viewModel: RecipientViewModel

  private fun onBack() {
    // Protect against remaining back listeners
    if (this.activity == null) {
      return
    }

    val changes = getChanges()
    if (changes.hasChanges()) {
      // Pop an alert dialogue.
      // on "OK" continue w/ nav back
      // else forget about it
      val builder = AlertDialog.Builder(requireActivity())
      builder.setTitle("Schimbări Nesalvate")
      builder.setMessage("Există modificări făcute la recipientul actual. Sigur dorești să navighezi inapoi fără le salvezi?")
      builder.setPositiveButton("Da") { _, _ ->
        (activity as MainActivity).clearOnBackListener()
        (activity as MainActivity).navController.popBackStack()
      }

      builder.setNegativeButton("Nu") { _, _ -> }
      builder.show()
    } else {
      (activity as MainActivity).clearOnBackListener()
      (activity as MainActivity).navController.popBackStack()
    }
  }

  @SuppressLint("CheckResult")
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    setHasOptionsMenu(true)

    // Get the arguments
    val recipientId = arguments?.getString(ARG_RECIPIENT_ID)

    val mainActivity = (requireActivity() as MainActivity)
    val toolbar = mainActivity.toolbar
    toolbar.title = recipientId ?: "Recipient"

    // Generate the binder
    val binding: FragmentRecipientBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_recipient, container, false)
    binding.lifecycleOwner = this

    if (recipientId == null) {
      val builder = AlertDialog.Builder(requireContext())
      builder.setTitle("Eroare")
        .setMessage("Recipientul are valoarea nulă (recipientId = null)")
      builder.setNegativeButton("Am Înțeles") { _, _ ->
        (activity as MainActivity).navController.navigateUp()
      }
      builder.create().show()
      return binding.root
    }

    mainActivity.setOnBackListener { this@RecipientFragment.onBack() }
    mainActivity.onBackPressedDispatcher.addCallback { this@RecipientFragment.onBack() }

    Database.recipient.getById(recipientId)
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .flatMap { recipient ->
        Database.uat.getByCountyIds(listOf(recipient.countyId, 0))
          .map { uats -> recipient then uats }
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
      .flatMap { data ->
        Database.recLabel.getAll()
          .subscribeOn(Schedulers.io())
          .map { labels -> data then labels }
      }
      .flatMap { data ->
        Database.recipientTags.getByRecipientId(data.first.eosId)
          .subscribeOn(Schedulers.io())
          .map { tags -> data then tags }.toMaybe()
      }
      .observeOn(AndroidSchedulers.mainThread())
      .autoDispose(this)
      .subscribe({ data ->
        viewModel = ViewModelProvider(this)
          .get(RecipientViewModel::class.java)

        val recipient = data.first

        viewModel.recipient = data.first
        viewModel.tags = data.sixth
        viewModel.addressStreet.value = recipient.addressStreet
        viewModel.addressNumber.value = recipient.addressNumber
        viewModel.comments.value = recipient.comments
        viewModel.labels.value = recipient.labels.keys.toList()

        val size = RecipientSize.fromDisplayName(recipient.size) ?: RecipientSize.SIZE_120L

        viewModel.tag0.value = viewModel.tags?.firstOrNull { t -> t.slot == 0 }?.tag ?: ""
        viewModel.tag1.value = viewModel.tags?.firstOrNull { t -> t.slot == 1 }?.tag ?: ""
        viewModel.tagSelected.value = 0
        viewModel.tagSlots.value = size.slots

        viewModel.uat.observe(viewLifecycleOwner, { newVal ->

          val uat = viewModel.uats.value?.firstOrNull { u -> u.name == newVal }
          if (uat != null) {
            Database.loc.getByUatIds(listOf(uat.id))
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .autoDispose(this)
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

        viewModel.uats.observe(viewLifecycleOwner, { newVal ->
          binding.uat.setAdapter(
            FoldingArrayAdapter(
              requireActivity(),
              android.R.layout.select_dialog_item,
              newVal.map { u -> u.name })
          )
          binding.uat.validator = (binding.uat.adapter as FoldingArrayAdapter).getValidator("Nedefinit")
        })

        viewModel.locs.observe(viewLifecycleOwner, { newVal ->
          binding.loc.setAdapter(
            FoldingArrayAdapter(
              requireActivity(),
              android.R.layout.select_dialog_item,
              newVal.map { l -> l.name }
            )
          )
          binding.loc.validator = (binding.loc.adapter as FoldingArrayAdapter).getValidator("Nedefinit")
        })

        viewModel.uats.value = data.second
        Log.d(RecipientFragment.tag, "${data.fourth}")
        viewModel.uat.value = data.fourth[0].name
        viewModel.loc.value = data.third[0].name
        viewModel.recLabels.value = data.fifth

        // Ensure we're always displaying the correct tags
        viewModel.size.observe(viewLifecycleOwner, { newSize ->
          val newRecipientSize = RecipientSize.fromDisplayName(newSize) ?: RecipientSize.SIZE_120L
          viewModel.tagSlots.value = newRecipientSize.slots
          if (newRecipientSize.slots - 1 <= (viewModel.tagSelected.value ?: 0)) {
            viewModel.tagSelected.value = 0
          }
        })

        binding.stream.inputType = InputType.TYPE_NULL
        binding.stream.setAdapter(
          FoldingArrayAdapter(
            requireActivity(),
            android.R.layout.select_dialog_item,
            RecipientStream.values()
              .filter { v -> v !== RecipientStream.UNK }
              .map { v -> v.displayName })
        )
        binding.stream.validator = (binding.stream.adapter as FoldingArrayAdapter).getValidator(RecipientStream.UNK.displayName)

        binding.size.inputType = InputType.TYPE_NULL
        binding.size.setAdapter(
          FoldingArrayAdapter(
            requireActivity(),
            android.R.layout.select_dialog_item,
            RecipientSize.getVisibleSizes()
          )
        )
        binding.size.validator = (binding.size.adapter as FoldingArrayAdapter).getValidator(RecipientSize.SIZE_120L.displayName)

        binding.lifecycle.inputType = InputType.TYPE_NULL
        binding.lifecycle.setAdapter(
          FoldingArrayAdapter(
            requireActivity(),
            android.R.layout.select_dialog_item,

            RecipientLifecycle.values().filter { it != RecipientLifecycle.UNKNOWN__ }.map {
              RecipientLifecycleDisplayName.get(it)
            }
          )
        )
        binding.lifecycle.validator = (binding.lifecycle.adapter as FoldingArrayAdapter).getValidator(RecipientLifecycle.LABEL_ONLY.toString())
        viewModel.lifecycle.value = RecipientLifecycleDisplayName.get(recipient.lifecycle)

        // Convert recipient stream to display name
        viewModel.stream.value = try {
          RecipientStream.valueOf(recipient.stream).displayName
        } catch (t: Throwable) {
          ""
        }
        viewModel.size.value = recipient.size

        (activity as MainActivity).btSvc.tags
          .observeOn(AndroidSchedulers.mainThread())
          .autoDispose(this)
          .subscribe tagSub@{ newVal ->

            // We need to do another thing:
            // Perform a DB lookup (if the warn reassignment option is on)
            // IF reassignment warning is enabled, then throw it out
            val reassignWarningEnabled = Preferences.reassignWarning.blockingFirst()
            val existingTag = Database.recipientTags.getByTag(newVal)
              .subscribeOn(Schedulers.io())
              .blockingGet()

            if (existingTag != null && existingTag.recipientId != viewModel.recipient?.eosId) {
              if (!reassignWarningEnabled) {
                // warning is disabled
                setCurrentTagValue(newVal)

                // but log analytics event
                (activity as MainActivity).firebaseAnalytics.logEvent(AuroraAnalytics.Event.REASSIGN_WARNING_IGNORED) {
                  param(AuroraAnalytics.Params.RECIPIENT_EOS_ID, viewModel.recipient?.eosId ?: "NULL")
                  param(AuroraAnalytics.Params.RFID_TAG, existingTag.tag)
                  param(AuroraAnalytics.Params.REASSIGN_WARNING_ENABLED, reassignWarningEnabled.toString())
                }
              } else {
                // Warn
                // TODO: Include 'time ago' when tag was reg'd
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Atenție")
                  .setMessage("Cipul scanat a fost deja asociat cu un alt recipient (${existingTag.recipientId ?: "NULL"})")
                // Proceed.
                builder.setPositiveButton("Continuă") { _, _ ->
                  (activity as MainActivity).firebaseAnalytics.logEvent(AuroraAnalytics.Event.REASSIGN_WARNING_IGNORED) {
                    param(AuroraAnalytics.Params.RECIPIENT_EOS_ID, viewModel.recipient?.eosId ?: "NULL")
                    param(AuroraAnalytics.Params.RFID_TAG, existingTag.tag)
                    param(AuroraAnalytics.Params.REASSIGN_WARNING_ENABLED, reassignWarningEnabled.toString())
                  }
                  setCurrentTagValue(newVal)
                }
                builder.setNegativeButton("Anulează") { _, _ ->
                  (activity as MainActivity).firebaseAnalytics.logEvent(AuroraAnalytics.Event.REASSIGN_WARNING_ACKNOWLEDGED) {
                    param(AuroraAnalytics.Params.RECIPIENT_EOS_ID, viewModel.recipient?.eosId ?: "NULL")
                    param(AuroraAnalytics.Params.RFID_TAG, existingTag.tag)
                  }
                }
                builder.create().show()
                (activity as MainActivity).firebaseAnalytics.logEvent(AuroraAnalytics.Event.REASSIGN_WARNING) {
                  param(AuroraAnalytics.Params.RECIPIENT_EOS_ID, viewModel.recipient?.eosId ?: "NULL")
                  param(AuroraAnalytics.Params.RFID_TAG, existingTag.tag)
                }
              }
              return@tagSub
            }
            setCurrentTagValue(newVal)
          }

        viewModel.groupId.value = recipient.groupId ?: ""
        val groupAdapter = GroupIdAdapter(requireContext(), android.R.layout.select_dialog_item)
        binding.group.setAdapter(groupAdapter)
        binding.group.validator = groupAdapter.getValidator()

        binding.model = viewModel
      }, { t ->
        Log.e(RecipientFragment.tag, "Error", t)
        Sentry.capture(t)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Eroare")
        builder.setMessage("A avut loc o eroare. ")
        builder.setNeutralButton("Am ințeles") { _, _ ->
          (activity as MainActivity).navController.popBackStack()
        }
        builder.setOnCancelListener {
          (activity as MainActivity).navController.popBackStack()
        }
        builder.show()
      }, {
        // OnComplete means some of the data wasn't loaded.
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Recipient Inexistent")
        builder.setMessage("Recipientul cu codul $recipientId nu a fost găsit")
        builder.setNeutralButton("Am ințeles") { _, _ ->
          (activity as MainActivity).navController.popBackStack()
        }
        builder.setOnCancelListener {
          (activity as MainActivity).navController.popBackStack()
        }
        builder.show()
      })
    return binding.root
  }

  private fun setCurrentTagValue(newVal: String) {
    when (viewModel.tagSelected.value ?: 0) {
      0 -> viewModel.tag0.value = newVal
      1 -> viewModel.tag1.value = newVal
    }

    if (viewModel.tag0.value == viewModel.tag1.value) {
      // If both tags have the same value,
      // null the one not selected.
      when (viewModel.tagSelected.value ?: 0) {
        0 -> viewModel.tag1.value = ""
        1 -> viewModel.tag0.value = ""
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    (requireActivity() as MainActivity).clearOnBackListener()
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
    if (!this::viewModel.isInitialized) {
      // FIXME: viewmodel not initialized.
      // probably still querying the database?
      val builder = AlertDialog.Builder(requireActivity())
      builder.setTitle("Eroare")
      builder.setMessage("Ai dat de un bug foarte rar. Te rug suna-l pe Paul si spune-i ce faceai.")
      builder.setPositiveButton("Ok") { _, _ -> }
      builder.show()
      return
    }

    val changes = getChanges()

    // If no tags are scanned, warn the user
    val noTags = (viewModel.tag0.value?.isEmpty() ?: true) && (
      viewModel.tag1.value?.isEmpty() ?: true)

    if (noTags) {
      val builder = AlertDialog.Builder(requireActivity())
      builder.setTitle("Atenție")
      builder.setMessage("Nu a fost scanat niciun tag pentru acest recipient")
      builder.setPositiveButton("Am înțeles") { _, _ ->
        // do all the save stuff
        if (changes.hasChanges()) {
          executeSave(changes)
        }
      }
      builder.setNegativeButton("Anulează") { _, _ -> }
      builder.show()
    } else {
      if (changes.hasChanges()) {
        executeSave(changes)
      }
    }
  }

  // Called immediately after doSave finalises gathering user info
  // and IF necessary.
  private fun executeSave(
    changes: RecipientChangedValues
  ) {
    Log.d(RecipientFragment.tag, "ExeSave")
    // Push the updates in the update db
    val mainActivity = (requireActivity() as MainActivity)
    val createdBy = mainActivity.authMgr.session.email.blockingFirst().value ?: "No Email"
    val posLat = mainActivity.location.lat
    val posLng = mainActivity.location.lng

    var c: Completable = Completable.complete()

    if (changes.hasRecipientUpdate()) {
      val upd = changes.getRecipientUpdate(createdBy, posLat, posLng)
      c = c
        .andThen(Database.recipientUpdates.insert(upd).subscribeOn(Schedulers.io()).ignoreElement())
        .andThen(applyRecipientUpdate(viewModel.recipient!!, upd))
    }

    if (changes.hasTagUpdates()) {
      val updates = changes.getTagUpdates(createdBy)
      // Insert the updates in the DB
      updates.forEach { upd ->
        c = c.andThen(Database.recipientTagUpdates.insert(upd).subscribeOn(Schedulers.io()).ignoreElement())
      }
      // And then apply them to the DB
      c = c.andThen(applyRecipientTagUpdates(viewModel.tags ?: listOf(), updates))
    }

    c.observeOn(AndroidSchedulers.mainThread())
      .autoDispose(this)
      .subscribe({
        // we're done saving it now.
        // Save is done. Nav back.
        (requireActivity() as MainActivity).navController.popBackStack()
      }, {
        Log.d(RecipientFragment.tag, "ERROR", it)
      })
  }

  private fun applyRecipientUpdate(r: Recipient, p: RecipientUpdate): Completable {
    val newRecipient = Recipient(
      id = r.id,
      addressNumber = p.addressNumber ?: r.addressNumber,
      addressStreet = p.addressStreet ?: r.addressStreet,
      uatId = p.uatId ?: r.uatId,
      locId = p.locId ?: r.locId,
      size = p.size ?: r.size,
      stream = p.stream ?: r.stream,
      posLat = p.posLat ?: r.posLat,
      posLng = p.posLng ?: r.posLng,
      lifecycle = p.lifecycle ?: r.lifecycle,
      groupId = if (p.groupId == "") {
        null
      } else (p.groupId ?: r.groupId),
      labels = r.labels,
      comments = p.comments ?: r.comments,
      countyId = r.countyId,
      eosId = r.eosId
    )
    return Database.recipient.rxInsert(listOf(newRecipient))
      .subscribeOn(Schedulers.io())
  }

  // TODO: if recipient has shed any tags, these are *NOT* updated
  // in the database.

  private fun applyRecipientTagUpdates(t: List<RecipientTag>, p: List<RecipientTagUpdate>): Completable {
    val (shedTags, remainingTags) = t.partition { tag -> p.none { it.tag == tag.tag } }
    Log.d(RecipientFragment.tag, "DELETE FOR TAGS = ${shedTags.map { it.tag }}")


    val updatedTags =
      remainingTags.mapNotNull { tag ->
        // Try to find an update for that slot.
        // if no update exists, it means nothing changed
        // so we are free to do nothing (return null).
        val u = p.firstOrNull { it.slot == tag.slot }
        if (u == null) {
          null
        } else {
          RecipientTag(
            tag = u.tag,
            slot = u.slot,
            recipientId = u.recipientId,
            id = tag.id,
            updatedAt = u.createdAt
          )
        }
      }.toMutableList()

    // Now check the updates, and see if there are any new tags in the updates
    p.forEach { u ->
      val tag = remainingTags.firstOrNull { it.slot == u.slot }
      if (tag == null) {
        updatedTags.add(RecipientTag(
          tag = u.tag,
          slot = u.slot,
          recipientId = u.recipientId,
          id = 0,
          updatedAt = u.createdAt
        ))
      }
    }

    Log.d(RecipientFragment.tag, "UPDATE FOR TAGS ${updatedTags.map { it.tag }}")

    return Database.recipientTags.deleteTags(shedTags.map { it.tag })
      .andThen(Database.recipientTags.rxInsert(updatedTags.toList()))
      .subscribeOn(Schedulers.io())
  }

  private data class RecipientChangedValues(
    val eosId: String,
    val id: Int,
    val tag0Val: String,            // Wtf is this?
    val tag1Val: String,            // WTF IS THIS?>!?!??!
    val tag0Id: Int,
    val tag1Id: Int,
    val locId: Int? = null,
    val uatId: Int? = null,
    val size: String? = null,
    val stream: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val comments: String? = null,
    val groupId: String? = null,        // Empty String represents EXPLICIT NULL in graphql
    val tag0: String? = null,           // Empty String represents EXPLICIT NULL in graphql
    val tag1: String? = null,           // Empty String represents EXPLICIT NULL in graphql
    val labels: Map<String, String?>? = null,
    val lifecycle: String? = null,
  ) {
    fun hasChanges() = (locId ?: uatId ?: size ?: stream ?: addressStreet ?: addressNumber ?: comments ?: groupId ?: tag0 ?: tag1 ?: labels ?: lifecycle) != null

    fun hasRecipientUpdate() = (locId ?: uatId ?: size ?: stream ?: addressStreet ?: addressNumber ?: comments ?: groupId ?: labels ?: lifecycle) != null

    fun hasTagUpdates() = (tag0 ?: tag1) != null

    fun getTagUpdates(createdBy: String): List<RecipientTagUpdate> {
      val rtn = mutableListOf<RecipientTagUpdate>()
      if (tag0 != null) {
        rtn.add(RecipientTagUpdate(
          tag = tag0Val,
          slot = 0,
          recipientId = eosId,
          uploaded = false,
          createdAt = DateUtil.nowISOTimestamp(),
          createdBy = createdBy,
          id = id,
          updateId = 0
        ))
      }
      if (tag1 != null) {
        rtn.add(RecipientTagUpdate(
          tag = tag1Val,
          slot = 1,
          recipientId = eosId,
          uploaded = false,
          createdAt = DateUtil.nowISOTimestamp(),
          createdBy = createdBy,
          id = id,
          updateId = 0
        ))
      }
      return rtn.toList()
    }

    fun getRecipientUpdate(createdBy: String, posLat: Double, posLng: Double): RecipientUpdate {
      return RecipientUpdate(
        locId = locId,
        uatId = uatId,
        groupId = groupId,
        size = size,
        stream = stream,
        labels = labels,
        comments = comments,
        lifecycle = lifecycle,
        addressStreet = addressStreet,
        addressNumber = addressNumber,
        posLat = if (posLat == 0.0) null else posLat,
        posLng = if (posLng == 0.0) null else posLng,

        eosId = eosId,
        uploaded = false,
        updateId = 0,
        id = id,
        createdAt = DateUtil.nowISOTimestamp(),
        createdBy = createdBy
      )
    }
  }

  // Generates a patch if necessary.
  private fun getChanges(): RecipientChangedValues {
    if (viewModel.recipient == null) {
      return RecipientChangedValues(
        eosId = "",
        id = 0,
        tag0Val = "",
        tag1Val = "",
        tag0Id = 0,
        tag1Id = 0,
      )
    }
    val r = viewModel.recipient!!


    val vmUatId = viewModel.uats.value?.find { u -> u.name == viewModel.uat.value }?.id ?: r.uatId
    val vmLocId = viewModel.locs.value?.find { l -> l.name == viewModel.loc.value }?.id ?: r.locId
    val vmSize = viewModel.size.value
    val vmStream = RecipientStream.fromDisplayName(viewModel.stream.value!!).name
    val vmAddressStreet = viewModel.addressStreet.value!!
    val vmAddressNumber = viewModel.addressNumber.value!!
    val vmComments = viewModel.comments.value!!
    val vmGroupId = viewModel.groupId.value!!

    val vmTag0 = viewModel.tag0.value!!
    val vmTag1 = viewModel.tag1.value!!

    val vmLifecycle = viewModel.lifecycle.value

    val tag0 = viewModel.tags?.firstOrNull { t -> t.slot == 0 }?.tag ?: ""
    val tag1 = viewModel.tags?.firstOrNull { t -> t.slot == 1 }?.tag ?: ""
    val rGroupId = r.groupId ?: ""

    val ret = RecipientChangedValues(
      eosId = r.eosId,
      id = r.id,
      tag0Val = if (vmTag0.isNotEmpty()) vmTag0 else tag0,
      tag1Val = if (vmTag1.isNotEmpty()) vmTag1 else tag1,
      tag0Id = viewModel.tags?.firstOrNull { t -> t.slot == 0 }?.id ?: 0,
      tag1Id = viewModel.tags?.firstOrNull { t -> t.slot == 1 }?.id ?: 0,
      uatId = if (r.uatId == vmUatId) null else vmUatId,
      locId = if (r.locId == vmLocId) null else vmLocId,
      size = if (r.size == vmSize) null else vmSize,
      stream = if (r.stream == vmStream) null else vmStream,
      comments = if (r.comments == vmComments) null else vmComments,
      lifecycle = if (vmLifecycle == r.lifecycle) null else RecipientLifecycleDisplayName.toValue(vmLifecycle).toString(),
      addressStreet = if (r.addressStreet == vmAddressStreet) null else vmAddressStreet,
      addressNumber = if (r.addressNumber == vmAddressNumber) null else vmAddressNumber,
      groupId = if (rGroupId == vmGroupId) null else vmGroupId,
      tag0 = if (vmTag0 == tag0) null else vmTag0,
      tag1 = if (vmTag1 == tag1) null else vmTag1,

      )

    Log.d(RecipientFragment.tag, "Changes: $ret")
    return ret
  }
}