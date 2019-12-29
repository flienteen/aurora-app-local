package com.persidius.eos.aurora.eos

enum class SyncState {
    INIT,

    // A failure in these is equivalent to ABORT'ing.
    DL_COUNTY,
    DL_UAT,
    DL_LOC,
    DL_ARTERY,
    DL_LABELS,

    SYNC_RECIPIENTS,
    SYNC_RECIPIENTS_WAIT_INTERNET,

    SYNC_GROUPS,
    SYNC_GROUPS_WAIT_INTERNET,

    SYNC_USERS,
    SYNC_USERS_WAIT_INTERNET,

    SYNC_TASKS,
    SYNC_TASKS_WAIT_INTERNET,

    SYNCHRONIZED,

    OUT_OF_SYNC,

    SYNC_PATCHES,
    UPDATE_TASKS,

    WAIT_INTERNET_BACKOFF,
    ABORTED;

    data class NextState(
        // When User initiates abort
        val onAbort: SyncState? = null,

        // When internet is restored
        val onInternet: SyncState? = null,

        // When an internet-related error occurs
        // or connectivity is lost
        val onInternetError: SyncState? = null,

        // When previous state completes successfully
        val onNext: SyncState? = null,

        // Other allowable states, triggered by external actions
        val other: List<SyncState?> = listOf()) {

        val legalStates: List<SyncState> = listOf(onAbort, onInternet, onNext).union(other).filterNotNull()

        fun canEnterState(nextState: SyncState?): Boolean {
            return if(nextState == null) false else nextState in legalStates
        }
    }

    companion object {
        val Next = mapOf(
            INIT to NextState(
                other = listOf(DL_COUNTY),
                onAbort = INIT      // A bit odd but when we "abort" init we're back at init.
            ),

            DL_COUNTY to NextState(
                onNext = DL_UAT,
                onInternetError = WAIT_INTERNET_BACKOFF,
                onAbort = ABORTED
            ),

            DL_UAT to NextState(
                onNext = DL_LOC,
                onInternetError = WAIT_INTERNET_BACKOFF,
                onAbort = ABORTED
            ),

            DL_LOC to NextState (
                onNext = DL_ARTERY,
                onInternetError = WAIT_INTERNET_BACKOFF,
                onAbort = ABORTED
            ),

            DL_ARTERY to NextState(
                onNext = DL_LABELS,
                onInternetError = WAIT_INTERNET_BACKOFF,
                onAbort = ABORTED
            ),

            DL_LABELS to NextState(
                onNext = SYNC_RECIPIENTS,
                onInternetError = WAIT_INTERNET_BACKOFF,
                onAbort = ABORTED
            ),

            SYNC_RECIPIENTS to NextState(
                onNext = SYNC_GROUPS,
                onInternetError = SYNC_RECIPIENTS_WAIT_INTERNET,
                onAbort = ABORTED
            ),

            SYNC_RECIPIENTS_WAIT_INTERNET to NextState(
                onInternet = SYNC_RECIPIENTS,
                onAbort = ABORTED
            ),

            SYNC_GROUPS to NextState(
                onNext = SYNC_USERS,
                onInternetError = SYNC_GROUPS_WAIT_INTERNET,
                onAbort = ABORTED
            ),

            SYNC_GROUPS_WAIT_INTERNET to NextState(
                onInternet = SYNC_GROUPS,
                onAbort = ABORTED
            ),

            SYNC_USERS to NextState(
                onNext = SYNC_TASKS,
                onInternetError = SYNC_USERS_WAIT_INTERNET,
                onAbort = ABORTED
            ),

            SYNC_USERS_WAIT_INTERNET to NextState(
                onInternet = SYNC_USERS,
                onAbort = ABORTED
            ),

            SYNC_TASKS to NextState(
                onNext = SYNCHRONIZED,
                onInternetError = SYNC_TASKS_WAIT_INTERNET,
                onAbort = ABORTED
            ),

            SYNC_TASKS_WAIT_INTERNET to NextState(
                onInternet = SYNC_TASKS,
                onAbort = ABORTED
            ),

            SYNCHRONIZED to NextState(
                onAbort = ABORTED,
                other = listOf(OUT_OF_SYNC)
            ),

            OUT_OF_SYNC to NextState(
                onInternet = SYNC_PATCHES,
                onAbort = ABORTED
            ),

            SYNC_PATCHES to NextState(
                onNext = UPDATE_TASKS,
                onInternetError = OUT_OF_SYNC
            ),

            UPDATE_TASKS to NextState(
                onNext = SYNC_RECIPIENTS,
                onInternetError = OUT_OF_SYNC
            ),

            WAIT_INTERNET_BACKOFF to NextState (
                //
                onAbort = ABORTED,

                // Immediately retry when internet is regained
                onInternetError = ABORTED,

                // onNext gets triggered in 30 seconds
                onNext = ABORTED
            ),

            ABORTED to NextState(
                onNext = INIT
            )
        )
    }
}

