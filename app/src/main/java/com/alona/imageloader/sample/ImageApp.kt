package com.alona.imageloader.sample

import android.app.Application
import com.alona.imageloader.sample.di.AppContainer

class ImageApp : Application() {
    val container by lazy { AppContainer(this) }
}
