package com.kimmeldevs.margaritaone

import android.app.Application
import com.kimmeldevs.margaritaone.data.AppContainer
import com.kimmeldevs.margaritaone.push.Push

class MargaritaOneApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Push.init(this)
    }
}
