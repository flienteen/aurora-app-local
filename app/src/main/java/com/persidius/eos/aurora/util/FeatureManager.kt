package com.persidius.eos.aurora.auth

import io.reactivex.Observable

class FeatureManager(authMgr: AuthManager) {

    val createBinEnabled: Observable<Boolean> = authMgr.session.roles.map {
        Role.VIEW_RECIPIENT in it && Role.UPDATE_RECIPIENT in it && Role.UPDATE_TAGS in it
    }

    val searchBinEnabled: Observable<Boolean> = authMgr.session.roles.map {
        Role.VIEW_RECIPIENT in it
    }

    val searchBinByTagEnabled: Observable<Boolean> = authMgr.session.roles.map {
        Role.VIEW_RECIPIENT in it && Role.VIEW_TAGS in it
    }

    val setGroupBinEnabled: Observable<Boolean> = authMgr.session.roles.map {
        Role.VIEW_GROUP in it
    }
}
