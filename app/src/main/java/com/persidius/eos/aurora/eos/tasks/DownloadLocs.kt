package com.persidius.eos.aurora.eos.tasks

import android.util.Log
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.eos.UAT_CHUNK_SIZE
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object DownloadLocs {
    fun execute(progress: BehaviorSubject<Int>, uatIds: List<Int>): Completable {
        progress.onNext(0)

        val chunks = uatIds.chunked(UAT_CHUNK_SIZE)
        var inserted = 0

        return Observable.fromIterable(chunks)
            .map { chunk -> Eos.queryLocs(chunk).blockingGet() }
            .map { result -> result.data() }
            .map { data -> data.locs }
            .map { locs -> locs.map { l -> Loc(l.id, l.name, l.uat.id, l.county.id) }}
            .doOnNext {locs ->
                Log.d("SM", "Received next data item, size: ${locs.size}")

                Database.loc.insert(locs)
                    .subscribeOn(Schedulers.computation())
                    .blockingAwait()

                Log.d("SM", "Data observable done")
                ++inserted
                progress.onNext(inserted * 100 / chunks.size)
            }
            .ignoreElements()
    }
}
