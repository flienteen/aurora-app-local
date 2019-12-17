package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Uat
import com.persidius.eos.aurora.eos.COUNTY_CHUNK_SIZE
import com.persidius.eos.aurora.eos.Eos
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

object DownloadUats {
    fun execute(countyIds: List<Int>): Observable<Int> {
        val progress = BehaviorSubject.createDefault(0)

        val chunks = countyIds.chunked(COUNTY_CHUNK_SIZE)
        var inserted = 0

        Observable.fromIterable(chunks)
            .flatMap { chunk -> Eos.queryUats(chunk) }
            .map { resp -> resp.data() }
            .map { data -> data.uats }
            .map { uats -> uats.map { u -> Uat(u.id, u.name, u.county.id) } }
            .doOnNext {
                progress.onNext(inserted / chunks.size)
                ++inserted
            }
            .buffer(chunks.size)
            .map { uats -> Database.uat.insert(uats.flatten()) }
            .map { c -> c.blockingAwait() }
            .firstOrError()
            .subscribe({
                progress.onNext(100)
                progress.onComplete()
            }, { t ->
                progress.onError(t)
            })

        return progress
    }
}