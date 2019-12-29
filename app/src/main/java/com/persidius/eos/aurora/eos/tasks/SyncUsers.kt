package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.UsersQuery
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Groups
import com.persidius.eos.aurora.database.entities.User
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.type.BillingType
import com.persidius.eos.aurora.util.Base32
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

object SyncUsers {
    /**
     * Retrieve & insert users in chunks of 2.5k
     * Progress is reported as total number of users inserted.
     */
    fun execute(progress: BehaviorSubject<Int>, updatedAfter: Int = 0): Completable {
        progress.onNext(0)

        val data = Observable.create<List<UsersQuery.RawUser>> { emitter ->
            var pageAfter: String? = null
            var items: List<UsersQuery.RawUser>

            try {
                do {
                    items = Eos.queryUsers(updatedAfter, pageAfter)
                        .map { result -> result.data() }
                        .map { data -> data.rawUsers }
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

        return data.map { rawUsers ->
            rawUsers.map { u ->
                User(
                    u.id,
                    u.name,
                    u.idNumber,
                    u.labels,
                    u.id,
                    u.active,
                    u.updatedAt.toLong(),
                    u.groupId,
                    u.type.name,
                    u.billingType.name,
                    when(u.billingType) {
                        BillingType.PER_PERSON -> {
                            u.billingData?.asUserBillingData_PerPerson?.persons
                        }
                        BillingType.PER_PERSON_RA -> {
                            u.billingData?.asUserBillingData_PerPerson_RA?.persons
                        }
                        else -> {
                            null
                        }
                    },
                    u.county.id,
                    u.uat.id,
                    u.loc.id
                )
            }
        }.doOnNext { users ->
            Database.user.insert(users)
                .subscribeOn(Schedulers.computation())
                .blockingAwait()

            progress.onNext((progress.value ?: 0) + users.size)
        }.ignoreElements()
    }
}