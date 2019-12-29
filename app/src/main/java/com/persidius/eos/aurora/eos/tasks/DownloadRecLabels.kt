package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.RecommendedLabel
import com.persidius.eos.aurora.eos.Eos
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object DownloadRecLabels {
    fun execute(progress: BehaviorSubject<Int>): Completable {
        progress.onNext(0)

        return Eos.queryRecLabels()
            .map { resp -> resp.data() }
            .map { data -> data.recommendedLabels }
            .map { recLabels -> recLabels.map { l -> RecommendedLabel(l.label, l.displayName) }}
            .doOnSuccess { recLabels ->
                Database.recLabel.insert(recLabels)
                    .subscribeOn(Schedulers.computation())
                    .blockingAwait()

                progress.onNext(100)
            }
            .ignoreElement()
    }
}
