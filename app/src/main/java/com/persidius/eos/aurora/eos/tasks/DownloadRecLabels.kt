package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.RecommendedLabel
import com.persidius.eos.aurora.eos.Eos
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

object DownloadRecLabels {
    fun execute(): Observable<Int> {
        val progress = BehaviorSubject.createDefault(0)

        Eos.queryRecLabels()
            .map { resp -> resp.data() }
            .map { data -> data.recommendedLabels }
            .map { recLabels -> recLabels.map { l -> RecommendedLabel(l.label, l.displayName) }}
            .map { recLabels -> Database.recLabel.insert(recLabels) }
            .firstOrError()
            .subscribe( {
                progress.onNext(100)
                progress.onComplete()
            }, { t ->
                progress.onError(t) }
            )

        return progress
    }
}
