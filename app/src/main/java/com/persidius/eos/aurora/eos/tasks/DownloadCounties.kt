package com.persidius.eos.aurora.eos.tasks

import androidx.work.WorkRequest
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.EosDatabase
import com.persidius.eos.aurora.database.entities.County
import com.persidius.eos.aurora.eos.Eos
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object DownloadCounties {
    fun execute(progress: BehaviorSubject<Int>): Completable {
        progress.onNext(0)

        return Eos.queryCounties()
            .map { it.data() }
            .map { it.counties }
            .map { counties -> counties.map { c -> County(c.id, c.seq, c.short_, c.name) }}
            .doOnSuccess {counties ->
                Database.county.insert(counties).subscribeOn(Schedulers.computation())
                    .blockingAwait()
                progress.onNext(100)
            }
            .ignoreElement()
    }

}