package com.persidius.eos.aurora.eos

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.apollographql.apollo.exception.ApolloNetworkException
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.authorization.Role
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.eos.tasks.*
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.Preferences
import com.persidius.eos.aurora.util.asLiveData
import com.persidius.eos.aurora.util.asOptional
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min


// SyncManager performs crap related to
// keeping the remote server & local DB in synchrony.

// This is performed via 2 channels:
// 1. OnDemand - in response to user actions
// 2. Periodic (scheduled via WorkManager)

object SyncManager: ConnectivityManager.OnNetworkActiveListener {

    private val interval = Observable.interval(0L, if(BuildConfig.DEBUG) 5L else 300L, TimeUnit.SECONDS)
        .subscribeOn(Schedulers.io())

    private val state: BehaviorSubject<SyncState> = Preferences.smSyncState
    private val lastSync: BehaviorSubject<Long> = Preferences.smLastSync

    private val lastRecipientUpdate = Preferences.smLastRecipientUpdate
    private val lastUserUpdate = Preferences.smLastUserUpdate
    private val lastGroupUpdate = Preferences.smLastGroupUpdate

    private val transitionSub: AtomicReference<Disposable?> = AtomicReference(null)

    private val internetActive = AtomicBoolean(false)

    val progress: BehaviorSubject<Int> = BehaviorSubject.createDefault(0)

    object LiveData {
        val progress: androidx.lifecycle.LiveData<Int> = SyncManager.progress.asLiveData()
        val lastSync: androidx.lifecycle.LiveData<Long> = SyncManager.lastSync.asLiveData()
        val state: androidx.lifecycle.LiveData<SyncState> = SyncManager.state.asLiveData()
    }

    private lateinit var am: AuthorizationManager


    private fun toState(nextState: SyncState?, force: Boolean = false) {
        if(nextState != null) {
            if (state.value != nextState) {
                state.onNext(nextState)
            }
        }
    }

    private fun onInternet() {
        internetActive.set(true)
        toState(SyncState.Next[state.value!!]?.onInternet)
    }

    private fun onInternetError() {
        internetActive.set(false)
        toState(SyncState.Next[state.value!!]?.onInternetError)
    }

    private fun onNext() = toState(SyncState.Next[state.value!!]?.onNext)

    private fun onAbort() {
        val transition = transitionSub.getAndSet(null)
        transition?.dispose()
        toState(SyncState.Next[state.value!!]?.onAbort, true)
    }

    /**
     * Indicates whether we can abort current state.
     */
    fun isAbortable(which: SyncState = state.value!!) =
        SyncState.Next[which]?.onAbort != null ?: false

    fun doAbort(): Boolean {
        if (!isAbortable()) {
            return false
        }

        onAbort()
        return true
    }

    fun doInit(): Boolean {
        if (state.value != SyncState.INIT) {
            return false
        }

        // Transition to next state (which is other[0])
        toState(SyncState.Next.getValue(SyncState.INIT).other[0]!!)
        return true
    }

    // Desynchronize either because of time or
    // because of user edits
    fun doDesynchronize() {
        if (state.value == SyncState.SYNCHRONIZED) {
            toState(SyncState.OUT_OF_SYNC)
        }
    }

    // What to do when a particular state is entered.
    private fun onEnterState(newState: SyncState) {
        Log.d("SM", "Enter state ${newState.name}. Thread: ${Thread.currentThread().id}")
        // Ensure AM is locked/unlocked correctly.
        if (!isAbortable(newState)) {
            am.lock()
        } else {
            am.unlock()
        }

        var exec: Completable? = null

        when (newState) {
            SyncState.DL_COUNTY -> {
                exec = DownloadCounties.execute(progress)
            }

            SyncState.DL_UAT -> {
                exec = Database.county.getAll()
                    .subscribeOn(Schedulers.computation())
                    .map { counties -> counties.map { c -> c.id } }
                    .flatMapCompletable { countyIds -> DownloadUats.execute(progress, countyIds) }
            }

            SyncState.DL_LOC -> {
                exec = Database.uat.getAll()
                    .subscribeOn(Schedulers.computation())
                    .map { uats -> uats.map { u -> u.id } }
                    .flatMapCompletable {uatIds-> DownloadLocs.execute(progress, uatIds) }
            }

            SyncState.DL_ARTERY -> {
                exec = Database.loc.getAll()
                    .subscribeOn(Schedulers.computation())
                    .flatMapCompletable { locs -> DownloadArteries.execute(progress, locs.map { l -> l.id }) }
            }

            SyncState.DL_LABELS -> {
                exec = DownloadRecLabels.execute(progress)
            }

            SyncState.SYNC_RECIPIENTS -> {
                val session = am.sessionToken.blockingFirst().value
                if(session == null) {
                    onAbort()
                    return
                }

                exec = if(session.hasRole(Role.LOGISTICS_VIEW_RECIPIENT)) {
                    val startTime = System.currentTimeMillis() / 1000L
                    SyncRecipients.execute(progress = progress,
                        withTags = session.hasRole(Role.LOGISTICS_VIEW_TAGS),
                        updatedAfter = lastRecipientUpdate.value?.toInt() ?: 0)
                        .doOnComplete {
                            val endTime = System.currentTimeMillis() / 1000L
                            val delta = endTime - startTime

                            val lastUpdate = Database.recipient.lastUpdatedAt().subscribeOn(Schedulers.io())
                                .blockingGet().result

                            lastRecipientUpdate.onNext(lastUpdate - delta)
                        }
                } else { null }
            }


            SyncState.SYNC_GROUPS -> {
                val session = am.sessionToken.blockingFirst().value
                if(session == null) {
                    onAbort()
                    return
                }

                exec = if(session.hasRole(Role.LOGISTICS_VIEW_GROUPS)) {
                    val startTime = System.currentTimeMillis() / 1000L

                    SyncGroups.execute(progress = progress,
                        updatedAfter = lastGroupUpdate.value?.toInt() ?: 0)
                        .doOnComplete {
                            // Update lastSyncUpdate
                            val endTime = System.currentTimeMillis() / 1000L
                            val delta = endTime - startTime

                            val lastUpdate = Database.groups.lastUpdatedAt().subscribeOn(Schedulers.io())
                                .blockingGet().result

                            lastGroupUpdate.onNext(lastUpdate - delta)
                        }
                } else { null }

            }

            SyncState.SYNC_USERS -> {
                val session = am.sessionToken.blockingFirst().value
                if(session == null) {
                    onAbort()
                    return
                }

                exec = if(session.hasRole(Role.LOGISTICS_VIEW_USER)) {
                    val startTime = System.currentTimeMillis() / 1000L
                    SyncUsers.execute(progress = progress,
                        updatedAfter = lastUserUpdate.value?.toInt() ?: 0)
                        .doOnComplete {
                            val endTime = System.currentTimeMillis() / 1000L
                            val delta = endTime - startTime

                            val lastUpdate = Database.user.lastUpdatedAt().subscribeOn(Schedulers.io())
                                .blockingGet().result

                            lastUserUpdate.onNext(lastUpdate - delta)
                        }
                } else { null }

                // Since SYNC_USERS is the last stage of a successful SYNC before entering
                // the SYNCHRONIZED state, we set
                // lastSyncTime here
                exec?.doOnComplete {
                    lastSync.onNext(System.currentTimeMillis() / 1000L)
                }

            }

            SyncState.SYNC_TASKS -> {
                // TODO: Implement me
            }

            SyncState.UPDATE_TASKS -> {
                // TODO: Implement Me
            }

            SyncState.SYNC_PATCHES -> {
                exec = SyncPatchGroups.execute(progress = progress)
            }

            SyncState.SYNCHRONIZED -> {
                exec = Completable.create {
                    lastSync.onNext(System.currentTimeMillis() / 1000L)
                    it.onComplete()
                }
                // Print a summary
                // of the Database

                Maybe.mergeArray(
                    Database.recipient.getCount().subscribeOn(Schedulers.io()).map {
                            r -> Pair("Recipients", r)
                    },
                    Database.recipientTag.getCount().subscribeOn(Schedulers.io()).map {
                            r -> Pair("Recipient Tags", r)
                    },
                    Database.groups.getCount().subscribeOn(Schedulers.io()).map {
                            r -> Pair("Groups", r)
                    },
                    Database.county.getCount().subscribeOn(Schedulers.io()).map {
                            r -> Pair("Counties", r)
                    },
                    Database.user.getCount().subscribeOn(Schedulers.io()).map {
                            r -> Pair("Users", r)
                    },
                    Database.uat.getCount().subscribeOn(Schedulers.io()).map {
                            r -> Pair("UATs", r)
                    },
                    Database.loc.getCount().subscribeOn(Schedulers.io()).map {
                            r -> Pair("Locs", r)
                    },
                    Database.artery.getCount().subscribeOn(Schedulers.io()).map {
                            r -> Pair("Arteries", r)
                    }
                ).subscribe { p ->
                    Log.d("SM", "DB Summary (${p.first}): ${p.second.result}")
                }
            }

            SyncState.ABORTED -> {
                // Clean entire database
                // EXCEPT patch & task update tables
                exec = Database.groups.deleteAll().subscribeOn(Schedulers.computation())
                    .andThen(
                        Database.recipient.deleteAll().subscribeOn(Schedulers.computation())
                    )
                    .andThen(
                        Database.artery.deleteAll().subscribeOn(Schedulers.computation())
                    )
                    .andThen(
                        Database.loc.deleteAll().subscribeOn(Schedulers.computation())
                    )
                    .andThen(
                        Database.uat.deleteAll().subscribeOn(Schedulers.computation())
                    )
                    .andThen(
                        Database.county.deleteAll().subscribeOn(Schedulers.computation())
                    )
                    .andThen(
                        Database.recLabel.deleteAll().subscribeOn(Schedulers.computation())
                    )
                    .andThen {
                        try {
                            Log.d("SM", "Finished cleaning DB. Resetting time markers")
                            lastSync.onNext(0L)
                            lastRecipientUpdate.onNext(0L)
                            lastUserUpdate.onNext(0L)
                            lastGroupUpdate.onNext(0L)
                            it.onComplete()
                        } catch (t: Throwable) {
                            it.onError(t)
                        }
                    }
            }
            SyncState.INIT -> {
                // If we entered INIT & we've got a solid token
                val session = am.sessionToken.blockingFirst().value
                if(session != null) {
                    // call doInit & exit exec flow.
                    doInit()
                    return
                }
            }
            SyncState.WAIT_INTERNET_BACKOFF -> {
                // Wait for a bit before entering ABORT state.
                exec = Observable.timer(30, TimeUnit.SECONDS).ignoreElements()
            }
            SyncState.OUT_OF_SYNC -> {
                exec = null
                if (internetActive.get()) {
                    // enter next state
                    onInternet()
                }
            }
            else -> {
                exec = null
            }
        }


        if(exec != null) {
            transitionSub.set(
                exec
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        Log.d("SM", "State executable done [for state = $newState]")
                        onNext()
                    }, { t ->
                        if(t is ApolloNetworkException) {
                            Log.e("SM", "Detected internet is down")
                            onInternetError()

                        } else {
                            // We're fucked.
                            if(isAbortable()) {
                                Log.e("SM", "Aborting due to error", t)
                                onAbort()
                            } else {
                                // Retry entering the state.
                                Log.e("SM", "Error in unabortable stage")
                            }
                        }
                    })
            )
        } else {
            // Just call onNext
            onNext()
        }
    }

    /// Initialize function, **NOT** to be confused with doInit.
    fun init(am: AuthorizationManager, applicationContext: Context) {
        // Subscribe to internet state changes
        val cs =
            applicationContext.getSystemService(Service.CONNECTIVITY_SERVICE) as ConnectivityManager
        cs.addDefaultNetworkActiveListener(this)

        // Start from the init state.
        this.am = am

        // Subscribe to state changes
        state.subscribe ({ newState -> onEnterState(newState) }, { t -> Log.e("SM", "Error: state error, $t", t) })

        // Monitor the token state, if we go from Null to a valid token, init sync.
        val sub = am.tokenObservable
            .scan(Pair(null.asOptional() as Optional<JWT>, null.asOptional() as Optional<JWT>),
                { p: Pair<Optional<JWT>, Optional<JWT>>, c: Optional<JWT> ->
                    Pair(p.second, c)
                })
            .subscribe { p ->
                val signedIn = !p.first.isPresent() && p.second.isPresent()
                val tokenRefreshed = p.first.isPresent() && p.second.isPresent()
                Log.d(
                    "SM",
                    "Token state change: $signedIn, $tokenRefreshed [${p.first.isPresent()}, ${p.second.isPresent()}]"
                )
                // val signedOut = p.first.isPresent() && !p.second.isPresent()

                if (signedIn || tokenRefreshed) {
                    doInit()
                }
            }
        progress.subscribe { newVal -> Log.d("SM", "Progress: $newVal") }

        // Every X minutes desync the app.
        interval.subscribe {doDesynchronize() }

        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        connectivityManager?.registerDefaultNetworkCallback(object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                this@SyncManager.onNetworkActive()
            }
        })
    }

    override fun onNetworkActive() {
        Log.d("SM", "Network Active, internetActive=${internetActive.get()}")
        if(!internetActive.get()) {
            onInternet()
        }
    }
}

