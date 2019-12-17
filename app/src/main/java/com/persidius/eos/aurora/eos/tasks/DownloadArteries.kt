package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Artery
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.eos.LOC_CHUNK_SIZE
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

object DownloadArteries {
    fun execute(locIds: List<Int>): Observable<Int> {
        val progress = BehaviorSubject.createDefault(0)

        val chunks = locIds.chunked(LOC_CHUNK_SIZE)
        var inserted = 0


        Observable.fromIterable(chunks)
            .flatMap { locIds -> Eos.queryArteries(locIds) }
            .map { result -> result.data() }
            .map { data -> data.arteries }
            .map { arts -> arts.map { a -> Artery(a.id, a.name, a.prefix, a.loc.id) }}
            .doOnNext {
                ++inserted
                progress.onNext(inserted / chunks.size)
            }
            .buffer(chunks.size)
            .map { arts -> Database.artery.insert(arts.flatten()) }
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