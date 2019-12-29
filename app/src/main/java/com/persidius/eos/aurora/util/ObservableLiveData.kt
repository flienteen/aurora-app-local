package com.persidius.eos.aurora.util

import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject


fun <T> Observable<T>.asLiveData(): LiveData<T?> {
    val ld = MutableLiveData<T>(null)

    val sub = this.subscribe { newVal ->
        Log.d("UTIL/OSLD", "postValue $newVal. Thread: ${Thread.currentThread().id}")
        if(Looper.getMainLooper().isCurrentThread) {
            Log.d("UTIL/OSLD","On main thread. Setting immediate")
            ld.value = newVal
        } else {
            Log.d("UTIL/OSLD", "Not on main thread, using postValue")
            ld.postValue(newVal)
        }
    }
    this.doFinally { sub.dispose() }

    return ld
}

fun <T> Observable<T>.asLiveData(defaultValue: T): LiveData<T> {
    val ld = MutableLiveData<T>(defaultValue)

    val sub = this.subscribe { newVal ->
        if(Looper.getMainLooper().isCurrentThread) {
            ld.value = newVal
        } else {
            ld.postValue(newVal)
        }
    }
    this.doFinally { sub.dispose() }

    return ld
}

fun <T> BehaviorSubject<T>.asLiveData(): LiveData<T> {
    val ld = MutableLiveData<T>(this.value!!)
    val sub = this.subscribe { newVal ->
        if(Looper.getMainLooper().isCurrentThread) {
            ld.value = newVal
        } else {
            ld.postValue(newVal)
        }
    }
    this.doFinally { sub.dispose() }

    return ld
}
