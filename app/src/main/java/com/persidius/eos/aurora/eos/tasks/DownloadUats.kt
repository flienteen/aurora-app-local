package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Uat
import com.persidius.eos.aurora.eos.COUNTY_CHUNK_SIZE
import com.persidius.eos.aurora.eos.Eos
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object DownloadUats {
    fun execute(progress: BehaviorSubject<Int>, countyIds: List<Int>): Completable {
        progress.onNext(0)

        val chunks = countyIds.chunked(COUNTY_CHUNK_SIZE)
        var inserted = 0

        return Observable.fromIterable(chunks)
            .map { chunk -> Eos.queryUats(chunk).blockingGet() }
            .map { resp -> resp.data() }
            .map { data -> data.uats }
            .map { uats -> uats.map { u -> Uat(u.id, u.name, u.county.id) } }
            .doOnNext {uats ->
                Database.uat.insert(uats)
                    .subscribeOn(Schedulers.computation())
                    .blockingAwait()

                ++inserted
                progress.onNext(inserted * 100 / chunks.size)
            }
            .ignoreElements()
    }
}