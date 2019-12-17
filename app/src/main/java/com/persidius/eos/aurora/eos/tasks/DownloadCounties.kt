package com.persidius.eos.aurora.eos.tasks

import androidx.work.WorkRequest
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.EosDatabase
import com.persidius.eos.aurora.database.entities.County
import com.persidius.eos.aurora.eos.Eos
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object DownloadCounties {
    fun execute(): Observable<Int> {
        val disposables = CompositeDisposable()
        val progress = BehaviorSubject.createDefault(0)

        disposables.add(Eos.queryCounties()
                .map { it.data() }
                .map { it.counties }
                .map {
                    // now we need to turn the apollo county into a DB county
                    it.map { c ->
                        County(c.id, c.seq, c.short_, c.name)
                    }
                }
            .observeOn(Schedulers.io())
                .subscribe ({ counties ->
                    // Once we have county data we're @ 75%
                    progress.onNext(75)
                    // In here we map

                    disposables.add(Database.county.insert(counties)
                        .subscribe {
                            progress.onNext(100)
                            progress.onComplete()
                        })
                }, { err ->
                    progress.onError(err)
        }))

        // Dispose of all the subs
        progress.doFinally {
            disposables.dispose()
        }

        return progress
    }

}