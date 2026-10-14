package com.kimmeldevs.margaritaone

import android.app.Application
import com.kimmeldevs.margaritaone.data.AppContainer

class MargaritaOneApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
