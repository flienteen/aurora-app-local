package com.persidius.eos.aurora.eos.tasks

import android.util.Log
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Artery
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.eos.LOC_CHUNK_SIZE
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object DownloadArteries {
    fun execute(progress: BehaviorSubject<Int>, locIds: List<Int>): Completable {
        progress.onNext(0)

        val chunks = locIds.chunked(LOC_CHUNK_SIZE)
        var inserted = 0

        return Observable.fromIterable(chunks)
            .map { chunk ->
                Log.d("SM", "Fetching...")
                Eos.queryArteries(chunk).blockingGet() }
            .map { result ->
                Log.d("AM", "Got result")
                result.data() }
            .map { data -> data.arteries }
            .map { arts -> arts.map { a -> Artery(a.id, a.name, a.prefix, a.loc.id) }}
            .doOnNext {arts ->
                Log.d("SM", "Inserting data")
                Database.artery.insert(arts)
                    .subscribeOn(Schedulers.io())
                    .blockingAwait()
                Log.d("SM", "Inserted. Next process.")
                ++inserted
                progress.onNext(inserted * 100 / chunks.size)
            }
            .ignoreElements()
    }
}