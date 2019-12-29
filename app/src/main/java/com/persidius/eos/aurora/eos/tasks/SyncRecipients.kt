package com.persidius.eos.aurora.eos.tasks

import com.persidius.eos.aurora.RecipientsQuery
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Recipient
import com.persidius.eos.aurora.database.entities.RecipientTag
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.util.Base32
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object SyncRecipients {
    fun execute(progress: BehaviorSubject<Int>, withTags: Boolean = false, updatedAfter: Int = 0): Completable {
        progress.onNext(0)

        val data = Observable.create<List<RecipientsQuery.RawRecipient>> { emitter ->
            var pageAfter: String? = null
            var items: List<RecipientsQuery.RawRecipient>
            try {
                do {
                    items = Eos.queryRecipients(withTags, updatedAfter, pageAfter)
                        .observeOn(Schedulers.io())
                        .map { result -> result.data()!!.rawRecipients }
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
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())

        // We do stuff in doOnNext so that only
        // when OBSERVING the operation is doOnNext executed.
        return data.doOnNext { rawRecipients ->
            val recipients = rawRecipients
                .map { r -> Recipient(
                    r.id,
                    r.addressNumber,
                    r.addressStreet,
                    r.posLat,
                    r.posLng,
                    r.labels,
                    r.comments,
                    r.active,
                    r.updatedAt.toLong(),
                    r.groupId,
                    r.stream.rawValue,
                    r.size,
                    r.loc.id,
                    r.uat.id,
                    r.county.id
                ) }
            val recipientTags = rawRecipients
                .mapNotNull { r -> r.tags?.map { t -> RecipientTag(t.tag, r.id, t.slot) }}
                .flatten()

            var completable = Database.recipient.insert(recipients).subscribeOn(Schedulers.computation())

            if(recipientTags.isNotEmpty()) {
                completable = completable.andThen(
                    Database.recipientTag.insert(recipientTags).subscribeOn(Schedulers.computation())
                )
            }

            completable.blockingAwait()

            // Update Progress
            progress.onNext((progress.value ?: 0) + rawRecipients.size)
        }
        .ignoreElements()
    }
}

