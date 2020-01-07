package com.persidius.eos.aurora.eos.tasks
import android.util.Log
import com.apollographql.apollo.api.Input
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.type.*
import com.persidius.eos.aurora.util.then
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

object SyncPatchGroups {
    fun execute(progress: BehaviorSubject<Int>): Completable {
        progress.onNext(0)

        /*
            General algorithm is as follows:
            1. Retrieve sessions that need syncing
            2. Retrieve patches for each session
            3a. If patches = 1 -> upload w/o transaction
            3b. else upload with a generated TXID, ensuring TX is accepted [this is not implemented]
            4. mark session as uploaded, if upload OK, else skip it, leave for next sync session.
         */


        return Completable.create {
            var dataProcessed = 0

            val sessions = Database.session.getPendingUpload()
                .subscribeOn(Schedulers.io())
                .blockingGet()

            try {
                for (session in sessions) {
                    // 1st get the pending recipient patches
                    Database.recipientPatch.getBySessionId(session.id.toInt())
                        .subscribeOn(Schedulers.io())
                        .flatMapCompletable { recipientPatches ->
                            /*
                        In the future this will become more complex.
                        At the moment *WE DO NOT* experience recipientPatches with size > 1

                        For size == 1, we can do a simple editRecipient call and be done with it, close the session.
                         */

                        val completable =
                            if (recipientPatches.size > 1) {
                                // Do Nothing.
                                Log.d("SM", "Too many patches ${recipientPatches.size}")
                                Completable.error(Exception("Session ${session.id} has too many patches (${recipientPatches.size}"))
                            } else {
                                val patch = recipientPatches[0]
                                Eos.editRecipient(
                                    patch.recipientId,
                                    patch.createdAt.toInt(),
                                    RecipientInput(
                                        Input.optional(patch.posLat),
                                        Input.optional(patch.posLng),
                                        Input.optional(
                                            if (patch.stream != null) WasteStream.valueOf(
                                                patch.stream!!
                                            ) else null
                                        ),
                                        Input.optional(patch.size),
                                        Input.optional(patch.addressStreet),
                                        Input.optional(patch.addressNumber),
                                        Input.optional(patch.uatId),
                                        Input.optional(patch.locId),
                                        Input.optional(patch.comments),
                                        Input.optional(patch.active),
                                        Input.optional(patch.labels.map { t ->
                                            LabelEdit(
                                                t.second,
                                                ArrayOp.valueOf(t.first.name)
                                            )
                                        }),
                                        Input.optional(patch.tags.map { t ->
                                            TagEdit(
                                                t.third,
                                                Input.fromNullable(t.second),
                                                ArrayOp.valueOf(t.first.name)
                                            )
                                        }),
                                        Input.optional(patch.groupId)
                                    )
                                )
                                    .map { response -> response.data() }
                                    .doOnSuccess {
                                        Database.session.setUploaded(session.id, true, null)
                                            .subscribeOn(Schedulers.io())
                                            .blockingAwait()
                                    }.ignoreElement()
                            }

                        completable
                    }.blockingAwait()
                    ++dataProcessed
                    progress.onNext(dataProcessed)
                }
                it.onComplete()
            } catch(t: Throwable) { it.onError(t) }
        }
    }
}