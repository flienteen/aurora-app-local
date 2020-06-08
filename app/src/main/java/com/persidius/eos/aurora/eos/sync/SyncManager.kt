package com.persidius.eos.aurora.eos.sync

import android.util.Log
import com.apollographql.apollo.api.Response
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.GroupsQuery
import com.persidius.eos.aurora.RecipientTagsQuery
import com.persidius.eos.aurora.RecipientsQuery
import com.persidius.eos.aurora.auth.AuthManager
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.*
import com.persidius.eos.aurora.eos.EosGraphQL
import com.persidius.eos.aurora.util.FeatureManager
import com.persidius.eos.aurora.util.Preferences
import com.persidius.eos.aurora.util.then
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

class SyncManager(private val authMgr: AuthManager, private val featMgr: FeatureManager) {
    companion object {
        private const val SYNC_TICK_FREQ: Long = 15
        private const val SYNC_WAIT_TIME: Long = 15
        private const val tag: String = "SM"
    }
    private val eosGraphQL = EosGraphQL(authMgr)
    // SyncManager depends on authMgr, for without a Session we cannot have anything.
    // SyncManager also maintains an instance of eosGraphQL

    private fun reset() {
        if(pendingCompletable != null) {
            pendingSub?.dispose()
            pendingCompletable = null
            pendingSub = null
        }

        Preferences.smState.onNext(SyncState.WAIT_VALID_SESSION)
        updateNextTick()
    }

    private var pendingCompletable: Completable? = null
    private var pendingSub: Disposable? = null

    val progress: BehaviorSubject<Float> = BehaviorSubject.createDefault(0.0f)

    private fun downloadDefinitions() = Completable.create { emitter ->
        try {
            progress.onNext(0f)
            val defs = eosGraphQL.queryDefinitions().blockingGet()
            val uats = defs.data?.me?.uats?.map { u ->
                Uat(
                    id = u.id,
                    name = u.name,
                    countyId = u.county.id
                )
            }
            val counties = defs
                .data?.me?.uats
                ?.map { u -> u.county }
                ?.distinctBy { c -> c.id }
                ?.map { c ->
                    County(
                        id = c.id,
                        name = c.name,
                        seq = c.seq,
                        short_ = c.short_
                    )
                }
            val locs = defs.data?.me?.uats
                ?.flatMap { u -> u.locs.map { l -> l.then(u.id).then(u.county.id) } }
                ?.map { l ->
                    Loc(
                        id = l.first.id,
                        name = l.first.name,
                        uatId = l.second,
                        countyId = l.third
                    )
                }

            val arteries = defs.data?.me?.uats
                ?.flatMap { u -> u.locs }
                ?.flatMap { l -> l.arteries.map { a -> a.then(l.id) } }
                ?.map { a ->
                    Artery(
                        id = a.first.id,
                        name = a.first.name,
                        prefix = a.first.prefix,
                        locId = a.second)
                }

            val recLabels = defs.data?.recommendedLabels?.map { rl ->
                RecommendedLabel(
                    displayName = rl.displayName,
                    label = rl.label
                )
            }

            progress.onNext(1f)
            Database.county.insert(counties!!).blockingAwait()
            Database.uat.insert(uats!!).blockingAwait()
            Database.loc.insert(locs!!).blockingAwait()
            Database.artery.insert(arteries!!).blockingAwait()
            Database.recLabel.insert(recLabels!!).blockingAwait()
            emitter.onComplete()
        } catch(t: Throwable) {
            emitter.onError(t)
        }
    }.subscribeOn(Schedulers.io())
    .doFinally { finalizeCompletable() }

    private fun downloadRecipients() = Completable.create { emitter ->
        try {
            progress.onNext(0f)
            var pageNumber = 0
            var results: Response<RecipientsQuery.Data>?

            do {
                results = eosGraphQL.queryRecipients(pageNumber).blockingGet()
                val downloadedItems = (pageNumber * EosGraphQL.RECIPIENT_PAGE_SIZE + (results?.data?.recipients?.items?.size ?: 0)).toFloat()
                val totalItems = max((results?.data?.recipients?.total ?: 1).toFloat(), 1f)
                progress.onNext(downloadedItems / totalItems)
                val recipients = results?.data?.recipients?.items?.map { r ->
                    Recipient(
                        eosId = r.eosId,
                        id = r.id,
                        addressNumber = r.addressNumber,
                        addressStreet = r.addressStreet,
                        posLat = r.posLat,
                        posLng = r.posLng,
                        labels = r.labels.map { l -> Pair(l.name, l.value) }.toMap(),
                        comments = r.comments,
                        size = r.size,
                        stream = r.stream.toString(),
                        groupId = r.groups.firstOrNull()?.eosId,
                        uatId = r.uat.id,
                        locId = r.loc.id,
                        countyId = r.county.id
                    )
                }

                Log.d(tag, "Downloaded page ${pageNumber}, got ${results?.data?.recipients?.items?.size}")
                Database.recipient.insert(recipients!!).blockingAwait()

                ++pageNumber
            } while ((results?.data?.recipients?.items?.size ?: 0) == EosGraphQL.RECIPIENT_PAGE_SIZE)
            emitter.onComplete()
        } catch(t: Throwable) {
            emitter.onError(t)
        }
    }.subscribeOn(Schedulers.io())
    .doFinally { finalizeCompletable() }

    private fun downloadGroups() = Completable.create { emitter ->
        try {
            progress.onNext(0f)
            var pageNumber = 0
            Log.d(tag, "entering results loop")
            var results: Response<GroupsQuery.Data>?
            do {
                results = eosGraphQL.queryGroups(pageNumber).blockingGet()
                val downloadedItems = (pageNumber * EosGraphQL.GROUP_PAGE_SIZE + (results?.data?.groups?.items?.size ?: 0)).toFloat()
                val totalItems = max((results?.data?.groups?.total ?: 1).toFloat(), 1f)
                progress.onNext(downloadedItems / totalItems)

                val groups = results?.data?.groups?.items?.map { g ->
                    Group(
                        eosId = g.eosId,
                        id = g.id,
                        type = g.type.toString(),
                        uatId = g.uat.id,
                        locId = g.loc.id,
                        countyId = g.county.id
                    )
                }

                Log.d(tag, "Downloaded page ${pageNumber}, got ${results?.data?.groups?.items?.size}")
                Database.group.insert(groups!!).blockingAwait()
                Log.d(tag, "Inserted into DB")
                ++pageNumber
            } while ((results?.data?.groups?.items?.size ?: 0) == EosGraphQL.GROUP_PAGE_SIZE)
            emitter.onComplete()
        } catch(t: Throwable) {
            emitter.onError(t)
        }
    }.subscribeOn(Schedulers.io())
    .doFinally { finalizeCompletable() }

    private fun downloadRecipientTags() = Completable.create { emitter ->
        try {
            progress.onNext(0f)
            var pageNumber = 0
            Log.d(tag, "entering results loop")
            var results: Response<RecipientTagsQuery.Data>?
            do {
                results = eosGraphQL.queryRecipientTags(pageNumber).blockingGet()
                val downloadedItems = (pageNumber * EosGraphQL.RECIPIENT_TAG_PAGE_SIZE + (results?.data?.recipientTags?.items?.size ?: 0)).toFloat()
                val totalItems = max((results?.data?.recipientTags?.total ?: 1).toFloat(), 1f)
                progress.onNext(downloadedItems / totalItems)

                val recipientTags = results?.data?.recipientTags?.items?.map { rt ->
                    RecipientTag(
                        id = rt.id,
                        recipientId = rt.recipient.firstOrNull()?.eosId,
                        tag = rt.tag,
                        slot = rt.slot
                    )
                }

                Log.d(tag, "Downloaded page ${pageNumber}, got ${results?.data?.recipientTags?.items?.size}")
                Database.recipientTags.insert(recipientTags!!).blockingAwait()
                Log.d(tag, "Inserted into DB")
                ++pageNumber
            } while ((results?.data?.recipientTags?.items?.size ?: 0) == EosGraphQL.RECIPIENT_TAG_PAGE_SIZE)
            emitter.onComplete()
        } catch(t: Throwable) {
            emitter.onError(t)
        }
    }.subscribeOn(Schedulers.io())
        .doFinally { finalizeCompletable() }

    private fun deltaSyncRecipients() = Completable.create { emitter ->
        try {
            progress.onNext(0f)
            var pageNumber = 0
            val afterId = Database.recipient.maxId().blockingGet()
            Log.d(tag, "entering results loop")
            var results: Response<RecipientsQuery.Data>?
            do {
                results = eosGraphQL.deltaQueryRecipients(afterId, pageNumber).blockingGet()
                val downloadedItems = (pageNumber * EosGraphQL.RECIPIENT_PAGE_SIZE + (results?.data?.recipients?.items?.size ?: 0)).toFloat()
                val totalItems = max((results?.data?.recipients?.total ?: 1).toFloat(), 1f)
                progress.onNext(downloadedItems / totalItems)

                val recipients = results?.data?.recipients?.items?.map { r ->
                    Recipient(
                        eosId = r.eosId,
                        id = r.id,
                        addressNumber = r.addressNumber,
                        addressStreet = r.addressStreet,
                        posLat = r.posLat,
                        posLng = r.posLng,
                        labels = r.labels.map { l -> Pair(l.name, l.value) }.toMap(),
                        comments = r.comments,
                        size = r.size,
                        stream = r.stream.toString(),
                        groupId = r.groups.firstOrNull()?.eosId,
                        uatId = r.uat.id,
                        locId = r.loc.id,
                        countyId = r.county.id
                    )
                }

                Log.d(tag, "Downloaded page ${pageNumber}, got ${results?.data?.recipients?.items?.size}")
                Database.recipient.insert(recipients!!).blockingAwait()
                ++pageNumber
            } while ((results?.data?.recipients?.items?.size ?: 0) == EosGraphQL.RECIPIENT_PAGE_SIZE)
            emitter.onComplete()
        } catch(t: Throwable) {
            emitter.onError(t)
        }
    }.subscribeOn(Schedulers.io())
    .doFinally { finalizeCompletable() }

    private fun deltaSyncGroups() = Completable.create { emitter ->
        try {
            progress.onNext(0f)
            val afterId = Database.group.maxId().blockingGet()
            var pageNumber = 0
            Log.d(tag, "entering results loop")
            var results: Response<GroupsQuery.Data>?
            do {
                results = eosGraphQL.deltaQueryGroups(afterId, pageNumber).blockingGet()
                val downloadedItems = (pageNumber * EosGraphQL.GROUP_PAGE_SIZE + (results?.data?.groups?.items?.size ?: 0)).toFloat()
                val totalItems = max((results?.data?.groups?.total ?: 1).toFloat(), 1f)
                progress.onNext(downloadedItems / totalItems)

                val groups = results?.data?.groups?.items?.map { g ->
                    Group(
                        eosId = g.eosId,
                        id = g.id,
                        type = g.type.toString(),
                        uatId = g.uat.id,
                        locId = g.loc.id,
                        countyId = g.county.id
                    )
                }

                Log.d(tag, "Downloaded page ${pageNumber}, got ${results?.data?.groups?.items?.size}")
                Database.group.insert(groups!!).blockingAwait()
                Log.d(tag, "Inserted into DB")
                ++pageNumber
            } while ((results?.data?.groups?.items?.size ?: 0) == EosGraphQL.GROUP_PAGE_SIZE)
            emitter.onComplete()
        } catch(t: Throwable) {
            emitter.onError(t)
        }
    }.subscribeOn(Schedulers.io())
    .doFinally { finalizeCompletable() }

    private fun deltaSyncTags() = Completable.create { emitter ->
        try {
            progress.onNext(0f)
            val afterId = Database.recipientTags.maxId().blockingGet()
            var pageNumber = 0
            Log.d(tag, "entering results loop")
            var results: Response<RecipientTagsQuery.Data>?
            do {
                results = eosGraphQL.deltaQueryRecipientTags(afterId, pageNumber).blockingGet()
                val downloadedItems = (pageNumber * EosGraphQL.RECIPIENT_TAG_PAGE_SIZE + (results?.data?.recipientTags?.items?.size ?: 0)).toFloat()
                val totalItems = max((results?.data?.recipientTags?.total ?: 1).toFloat(), 1f)
                progress.onNext(downloadedItems / totalItems)

                val recipientTags = results?.data?.recipientTags?.items?.map { rt ->
                    RecipientTag(
                        id = rt.id,
                        recipientId = rt.recipient.firstOrNull()?.eosId,
                        tag = rt.tag,
                        slot = rt.slot
                    )
                }

                Log.d(tag, "Downloaded page ${pageNumber}, got ${results?.data?.recipientTags?.items?.size}")
                Database.recipientTags.insert(recipientTags!!).blockingAwait()
                Log.d(tag, "Inserted into DB")
                ++pageNumber
            } while ((results?.data?.recipientTags?.items?.size ?: 0) == EosGraphQL.RECIPIENT_TAG_PAGE_SIZE)
            emitter.onComplete()
        } catch(t: Throwable) {
            emitter.onError(t)
        }
    }.subscribeOn(Schedulers.io())
    .doFinally { finalizeCompletable() }

    private fun updateRecipients() = Completable.create {emitter ->
        try {
            val updates = Database.recipientUpdates.getPendingUpload().blockingGet()
            progress.onNext(0f)
            val total = updates.size + 1f
            var ctr = 0f

            for (update in updates) {
                val result = eosGraphQL.updateRecipient(update).blockingGet()
                if (result.data != null) {
                    progress.onNext(ctr / total)
                    Database.recipientUpdates.markUploaded(update.updateId).blockingAwait()
                    ++ctr
                } else {
                    break
                }
            }
        } catch(t: Throwable) {
            emitter.onError(t)
        }
        emitter.onComplete()
    }.subscribeOn(Schedulers.io())
    .doFinally { finalizeCompletable() }

    private fun updateTags() = Completable.create {emitter ->
        try {
            val updates = Database.recipientTagUpdates.getPendingUpload().blockingGet()
            progress.onNext(0f)
            val total = updates.size + 1f
            var ctr = 0f

            for (update in updates) {
                val result = eosGraphQL.updateTag(update).blockingGet()
                if (result.data != null) {
                    progress.onNext(ctr / total)
                    Database.recipientTagUpdates.markUploaded(update.updateId).blockingAwait()
                    ++ctr
                } else {
                    break
                }
            }
        } catch(t: Throwable) {
            emitter.onError(t)
        }
        emitter.onComplete()
    }.subscribeOn(Schedulers.io())
    .doFinally { finalizeCompletable() }

    private fun updateNextTick(src: TickSource = TickSource.Update) {
        var sub: Disposable? = null
        val obs = Observable.timer(100, TimeUnit.MILLISECONDS)
        .doOnComplete { sub?.dispose(); }
        sub = obs.subscribe { tick.onNext(src) }
    }


    private fun clearDatabase(): Completable =
        Database.recLabel.deleteAll()
        .andThen(Database.recipient.deleteAll())
        .andThen(Database.group.deleteAll())
        .andThen(Database.artery.deleteAll())
        .andThen(Database.loc.deleteAll())
        .andThen(Database.uat.deleteAll())
        .andThen(Database.county.deleteAll())
        .subscribeOn(Schedulers.io())
        .doFinally { progress.onNext(1f); finalizeCompletable() }

    // from multiple sources. We want calls to exe sequentially.
    private val lock: ReentrantLock = ReentrantLock()
    private fun eventLoop(): Unit = lock.withLock{
        if(pendingCompletable != null) {
            Log.d(tag, "Update loop ${Preferences.smState.value}, skipping as there is a pending completable")
            return
        }

        Log.d(tag, "Update loop ${Preferences.smState.value}")
        if(authMgr.session.isValid.blockingFirst()) {
            if(!authMgr.session.isOnline.blockingFirst()) {
                // Skip as token is not online.
                Log.d(tag, "Skipping loop because token is not online")
                return
            }

            Log.d(tag, "Valid Session?")
            when(Preferences.smState.value) {
                null,
                SyncState.WAIT_VALID_SESSION -> {
                    // enter Definitions state
                    pendingCompletable = clearDatabase()
                    pendingSub = pendingCompletable
                    ?.subscribe({
                        Preferences.smState.onNext(SyncState.DEFINITIONS);
                        updateNextTick()
                    }, { t -> Log.e(tag, "error", t); })
                }
                SyncState.DEFINITIONS -> {
                    pendingCompletable = downloadDefinitions()
                    pendingSub = pendingCompletable
                    ?.subscribe({
                        Preferences.smState.onNext(SyncState.RECIPIENTS)
                        Log.d(tag, "SM Definitions finished")
                        Log.d(tag, "Counties: ${Database.county.getCount().blockingGet()}")
                        Log.d(tag, "Uats: ${Database.uat.getCount().blockingGet()}")
                        Log.d(tag, "Locs: ${Database.loc.getCount().blockingGet()}")
                        Log.d(tag, "Arteries: ${Database.artery.getCount().blockingGet()}")
                        updateNextTick()
                    }, { t -> Log.e(tag, "error", t); })
                }
                SyncState.RECIPIENTS -> {
                    // this means we're just downloading the initial sync.
                    // the initial sync is a completable of chained nextPage
                    pendingCompletable = downloadRecipients()
                    pendingSub = pendingCompletable
                    ?.subscribe ({
                        Preferences.smState.onNext(SyncState.TAGS)
                        Log.d(tag, "SM D/L Recipients finished")
                        Log.d(tag, "Recipients: ${Database.recipient.getCount().blockingGet()}")
                        updateNextTick()
                    }, { t -> Log.e(tag, "error", t); })
                }
                SyncState.TAGS -> {
                    // this means we're just downloading the initial sync.
                    // the initial sync is a completable of chained nextPage
                    pendingCompletable = downloadRecipientTags()
                    pendingSub = pendingCompletable
                        ?.subscribe ({
                            Preferences.smState.onNext(SyncState.GROUPS)
                            Log.d(tag, "SM D/L Tags finished [TAGS]")
                            Log.d(tag, "Recipients: ${Database.recipientTags.getCount().blockingGet()}")
                            updateNextTick()
                        }, { t -> Log.e(tag, "error", t); })
                }
                SyncState.GROUPS -> {
                    // this means we're just downloading the initial sync.
                    pendingCompletable = downloadGroups()
                    pendingSub = pendingCompletable
                        ?.subscribe({
                            Preferences.smState.onNext(SyncState.UPDATE_RECIPIENTS)
                            Log.d(tag, "SM D/L Groups finished")
                            Log.d(tag, "Groups: ${Database.group.getCount().blockingGet()}")
                            updateNextTick()
                        }, { t -> Log.e(tag, "error", t); })
                }

                SyncState.UPDATE_RECIPIENTS -> {
                    pendingCompletable = updateRecipients()
                    pendingSub = pendingCompletable
                    ?.subscribe({
                        Preferences.smState.onNext(SyncState.UPDATE_TAGS)
                        Log.d(tag, "Update Recipients finished")
                        Log.d(tag, "Pending Upload: ${Database.recipientUpdates.getPendingUploadCount().blockingGet()}")
                        updateNextTick()
                    }, { t -> Log.e(tag, "error", t); })
                }
                SyncState.UPDATE_TAGS -> {
                    pendingCompletable = updateTags()
                    pendingSub = pendingCompletable
                        ?.subscribe({
                            Preferences.smState.onNext(SyncState.SYNC_RECIPIENTS)
                            Log.d(tag, "Update recipient tags finished")
                            Log.d(tag, "Pending Upload: ${Database.recipientTagUpdates.getPendingUploadCount().blockingGet()}")
                            updateNextTick()
                        }, { t -> Log.e(tag, "error", t); })
                }

                SyncState.SYNC_RECIPIENTS -> {
                    pendingCompletable = deltaSyncRecipients()
                    pendingSub = pendingCompletable
                        ?.doFinally { finalizeCompletable() }
                        ?.subscribe({
                            Preferences.smState.onNext(SyncState.SYNC_TAGS)
                            Log.d(tag, "SM Recipients finished")
                            Log.d(tag, "Recipients: ${Database.recipient.getCount().blockingGet()}")
                            updateNextTick()
                        }, { t -> Log.e(tag, "error", t); })
                }

                SyncState.SYNC_TAGS -> {
                    pendingCompletable = deltaSyncTags()
                    pendingSub = pendingCompletable
                        ?.doFinally { finalizeCompletable() }
                        ?.subscribe({
                            Preferences.smState.onNext(SyncState.SYNC_GROUPS)
                            Log.d(tag, "SM Tags finished")
                            Log.d(tag, "Tags: ${Database.recipientTags.getCount().blockingGet()}")
                            updateNextTick()
                        }, { t -> Log.e(tag, "error", t); })
                }

                SyncState.SYNC_GROUPS -> {
                    pendingCompletable = deltaSyncGroups()
                    pendingSub = pendingCompletable
                        ?.doFinally { finalizeCompletable() }
                        ?.subscribe({
                            Preferences.smState.onNext(SyncState.SYNC_WAIT)
                            Log.d(tag, "SM Groups finished")
                            Log.d(tag, "Groups: ${Database.group.getCount().blockingGet()}")
                            updateNextTick()
                        },{ t -> Log.e(tag, "error", t); })
                }

                SyncState.SYNC_WAIT -> {
                    progress.onNext(1f)
                    Preferences.smLastSync.onNext(
                        SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.US).format(Date())
                    )
                    pendingCompletable = Observable.timer(SYNC_WAIT_TIME, TimeUnit.SECONDS)
                        .ignoreElements().doFinally { finalizeCompletable() }

                    pendingSub = pendingCompletable
                    ?.subscribe {
                        Preferences.smState.onNext(SyncState.UPDATE_RECIPIENTS)
                        updateNextTick()
                    }

                }
            }
        } else {
            // if we're not in the 'NOT_READY' state then migrate to it.
            if(Preferences.smState.value != SyncState.WAIT_VALID_SESSION) {
                reset()
            }
        }
    }

    private fun finalizeCompletable() {
        pendingCompletable = null
        pendingSub?.dispose()
        pendingSub = null
    }

    private fun printStatus() {
        var sub = Completable.create {
            Log.d(tag, "Arteries: ${Database.artery.getCount().blockingGet()}")
            Log.d(tag, "Locs: ${Database.loc.getCount().blockingGet()}")
            Log.d(tag, "Uats: ${Database.uat.getCount().blockingGet()}")
            Log.d(tag, "Counties: ${Database.county.getCount().blockingGet()}")
            Log.d(tag, "Recipients: ${Database.recipient.getCount().blockingGet()}")
            Log.d(tag, "Recipient Tags: ${Database.recipientTags.getCount().blockingGet()}")
            Log.d(tag, "Groups: ${Database.group.getCount().blockingGet()}")
            Log.d(tag, "Recipient Updates: ${Database.recipientUpdates.getCount().blockingGet()}")
            Log.d(tag, "Recipient Tag Updates: ${Database.recipientTagUpdates.getCount().blockingGet()}")
            it.onComplete()
        }.subscribeOn(Schedulers.io()).subscribe { }
    }

    private enum class TickSource {
        Update,
        Other,
        Timer
    }

    private val tick: BehaviorSubject<TickSource> = BehaviorSubject.createDefault(TickSource.Other)

    init {
        if(BuildConfig.DEBUG) { printStatus() }
        val cdisp = CompositeDisposable()
        cdisp.add(Observable.interval(0L, SYNC_TICK_FREQ, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe { tick.onNext(TickSource.Timer) })

        cdisp.add(tick.subscribe { src -> eventLoop() })

        cdisp.add(authMgr.session.isValid
            .observeOn(Schedulers.io())
            .subscribe {tick.onNext(TickSource.Other)})
    }
}
