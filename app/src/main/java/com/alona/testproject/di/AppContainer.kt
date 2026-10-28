package com.alona.testproject.di

import android.content.Context
import com.alona.imageloader.ImageLoader
import com.alona.testproject.data.DefaultImageRepository
import com.alona.testproject.data.ImageListApi
import com.alona.testproject.data.ImageRepository

/** Manual DI: app-wide dependencies. */
class AppContainer(context: Context) {

    val imageRepository: ImageRepository = DefaultImageRepository(ImageListApi(IMAGE_LIST_URL))

    val imageLoader: ImageLoader = ImageLoader.getInstance(context)

    private companion object {
        const val IMAGE_LIST_URL =
            "https://zipoapps-storage-test.nyc3.digitaloceanspaces.com/image_list.json"
    }
}
