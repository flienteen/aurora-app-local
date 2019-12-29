package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.GroupsQuery
import com.persidius.eos.aurora.database.entities.Groups
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.util.Base32
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object SyncGroups {
    fun execute(progress: BehaviorSubject<Int>, updatedAfter: Int = 0): Completable {
        progress.onNext(0)

        val data = Observable.create<List<GroupsQuery.RawGroup>> { emitter ->
            var pageAfter: String? = null
            var items: List<GroupsQuery.RawGroup>
            try {
                do {
                    items = Eos.queryGroups(updatedAfter, pageAfter)
                        .map { result -> result.data() }
                        .map { data -> data.rawGroups }
                        .blockingGet()

                    if(items.isNotEmpty()) {
                        pageAfter = items.last().id
                        emitter.onNext(items)
                    }
                } while (items.isNotEmpty())
                emitter.onComplete()

            } catch(t: Throwable) {
                if(!emitter.isDisposed) {
                    emitter.onError(t)
                }
            }
        }

        return data.map { rawGroups ->
            rawGroups.map { g ->
                Groups(
                    g.id,
                    g.labels,
                    g.comments,
                    g.type.name,
                    g.updatedAt.toLong(),
                    g.active,
                    g.uat.id,
                    g.county.id
                )
            }
        }.doOnNext { groups ->
            Database.groups.insert(groups)
                .subscribeOn(Schedulers.computation())
                .blockingAwait()

            progress.onNext((progress.value ?: 0) + groups.size)
        }.ignoreElements()
    }
}

