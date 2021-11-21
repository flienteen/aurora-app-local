package com.persidius.eos.aurora.util

import com.persidius.eos.aurora.auth.AuthManager
import com.persidius.eos.aurora.auth.Role
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

class FeatureManager(authMgr: AuthManager) {

    val createBinEnabled: Observable<Boolean> = authMgr.session.roles.map {
        Role.VIEW_RECIPIENT in it && Role.UPDATE_RECIPIENT in it && Role.UPDATE_TAGS in it
    }.observeOn(AndroidSchedulers.mainThread())

    val searchBinEnabled: Observable<Boolean> = authMgr.session.roles.map {
        Role.VIEW_RECIPIENT in it
    }.observeOn(AndroidSchedulers.mainThread())

    val searchBinByTagEnabled: Observable<Boolean> = authMgr.session.roles.map {
        Role.VIEW_RECIPIENT in it && Role.VIEW_TAGS in it
    }.observeOn(AndroidSchedulers.mainThread())

    val setGroupBinEnabled: Observable<Boolean> = authMgr.session.roles.map {
        Role.VIEW_GROUP in it
    }.observeOn(AndroidSchedulers.mainThread())

    val createCollectionEnabled: Observable<Boolean> = authMgr.session.roles.map {
      Role.CREATE_COLLECTION in it
    }.observeOn(AndroidSchedulers.mainThread())
}
