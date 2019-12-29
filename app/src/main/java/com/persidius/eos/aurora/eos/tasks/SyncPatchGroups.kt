package com.persidius.eos.aurora.eos.tasks

import io.reactivex.Completable
import io.reactivex.subjects.BehaviorSubject

object SyncPatchGroups {
    fun execute(progress: BehaviorSubject<Int>): Completable {
        progress.onNext(0)

        return Completable.fromRunnable {
            progress.onNext(100)
        }
    }
}