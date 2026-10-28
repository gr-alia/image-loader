package com.alona.testproject

import android.app.Application
import com.alona.testproject.di.AppContainer

class ImageApp : Application() {
    val container by lazy { AppContainer(this) }
}
