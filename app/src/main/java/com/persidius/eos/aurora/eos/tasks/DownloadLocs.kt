package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.eos.UAT_CHUNK_SIZE
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

object DownloadLocs {
    fun execute(uatIds: List<Int>): Observable<Int> {
        // Max 30 Uats
        val progress = BehaviorSubject.createDefault(0)

        val chunks = uatIds.chunked(UAT_CHUNK_SIZE)
        var inserted = 0

        Observable.fromIterable(chunks)
            .flatMap { uatIds -> Eos.queryLocs(uatIds) }
            .map { result -> result.data() }
            .map { data -> data.locs }
            .map { locs -> locs.map { l -> Loc(l.id, l.name, l.uat.id, l.county.id) }}
            .doOnNext {
                ++inserted
                progress.onNext(inserted / chunks.size)
            }
            .buffer(chunks.size)
            .map { locs -> Database.loc.insert(locs.flatten()) }
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
