package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.TaskSearchQuery
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Task
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.util.Base32
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*

object SyncTasks {
    fun execute(progress: BehaviorSubject<Int>, updatedAfter: Int = 0): Completable {
        progress.onNext(0)

        val data = Observable.create<List<TaskSearchQuery.Task>> { emitter ->
            var pageAfter: Int? = null
            var items: List<TaskSearchQuery.Task>
            try {
                do {
                    items = Eos.queryTasks(updatedAfter, pageAfter)
                        .observeOn(Schedulers.io())
                        .map { result ->
                            if (result.data() != null) result.data()!!.tasks else listOf()
                        }.blockingGet()

                    if (items.isNotEmpty()) {
                        pageAfter = items.last().id
                        emitter.onNext(items)
                    }
                } while (items.isNotEmpty())
                emitter.onComplete()
            } catch (t: Throwable) {
                if (!emitter.isDisposed) {
                    emitter.onError(t)
                }
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())

        // We do stuff in doOnNext so that only
        // when OBSERVING the operation is doOnNext executed.
        return data.doOnNext { results ->
            val tasks = results
                .map { r ->
                    Task(
                        r.id,
                        r.gid,
                        r.assignedTo,
                        r.validFrom,
                        r.validTo,
                        r.updatedBy,
                        r.updatedAt,
                        r.status.rawValue,
                        r.posLat,
                        r.posLng,
                        r.comments,
                        r.uat.id,
                        r.loc.id,
                        r.county.id,
                        r.groups.map { g -> g.id },
                        r.users.map { u -> u.id },
                        r.recipients.map { recipient -> recipient.id }
                    )
                }

            val completable = Database.task.insert(tasks).subscribeOn(Schedulers.computation())
            completable.blockingAwait()

            // Update Progress
            progress.onNext((progress.value ?: 0) + results.size)
        }.ignoreElements()
    }
}

