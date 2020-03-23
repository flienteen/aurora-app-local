package com.persidius.eos.aurora.eos.tasks

import android.util.Log
import com.apollographql.apollo.api.Input
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Session
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.type.*
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
                    // sync recipient patches
                    syncRecipients(session)
                    ++dataProcessed
                    progress.onNext(dataProcessed)

                    // sync task patches
                    syncTasks(session)
                    ++dataProcessed
                    progress.onNext(dataProcessed)
                }
                it.onComplete()
            } catch (t: Throwable) {
                it.onError(t)
            }
        }
    }

    private fun syncRecipients(session: Session) {
        Database.recipientPatch.getBySessionId(session.id.toInt())
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { recipientPatches ->
                /* In the future this will become more complex.
                At the moment *WE DO NOT* experience recipientPatches with size > 1
                For size == 1, we can do a simple editRecipient call and be done with it, close the session. */

                val completable =
                    if (recipientPatches.isEmpty()) {
                        // Do Nothing.
                        Log.d("SM", "No patches ${recipientPatches.size}")
                        Completable.create {
                            it.onComplete()
                        }
                    } else if (recipientPatches.size > 1) {
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
                        ).map { response -> response.data() }
                            .doOnSuccess {
                                Database.session.setUploaded(session.id, true, null)
                                    .subscribeOn(Schedulers.io())
                                    .blockingAwait()
                            }.ignoreElement()
                    }

                completable
            }.blockingAwait()
    }

    private fun syncTasks(session: Session) {
        Database.taskPatch.getBySessionId(session.id.toInt())
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { taskPatches ->
                val completable =
                    if (taskPatches.isEmpty()) {
                        // Do Nothing.
                        Log.d("SM", "No patches ${taskPatches.size}")
                        Completable.create {
                            it.onComplete()
                        }
                    } else if (taskPatches.size > 1) {
                        // Do Nothing.
                        Log.d("SM", "Too many patches ${taskPatches.size}")
                        Completable.error(Exception("Session ${session.id} has too many patches (${taskPatches.size}"))
                    } else {
                        val patch = taskPatches[0]
                        Eos.updateTask(
                            patch.gid ?: "",
                            patch.taskId,
                            patch.updatedAt,
                            TaskInput(
                                Input.optional(patch.assignedTo),
                                Input.optional(TaskStatus.safeValueOf(patch.taskStatus)),
                                Input.optional(patch.posLat),
                                Input.optional(patch.posLng),
                                Input.optional(patch.comments),
                                Input.optional(patch.recipients),
                                Input.absent(),
                                Input.absent(),
                                Input.optional(patch.locId),
                                Input.optional(patch.uatId),
                                Input.absent()
                            )
                        ).map { response ->
                            true // response.data is null which would give an error if returned
                        }.doOnSuccess {
                            Database.session.setUploaded(session.id, true, null)
                                .subscribeOn(Schedulers.io())
                                .blockingAwait()
                        }.ignoreElement()
                    }

                completable
            }.blockingAwait()

    }
}