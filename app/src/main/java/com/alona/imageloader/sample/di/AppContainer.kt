package com.alona.imageloader.sample.di

import android.content.Context
import com.alona.imageloader.ImageLoader
import com.alona.imageloader.sample.data.DefaultImageRepository
import com.alona.imageloader.sample.data.ImageCacheCleaner
import com.alona.imageloader.sample.data.ImageListApi
import com.alona.imageloader.sample.data.ImageRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Manual DI: app-wide dependencies. */
class AppContainer(context: Context) {

    val imageRepository: ImageRepository = DefaultImageRepository(ImageListApi(IMAGE_LIST_URL))

    val imageLoader: ImageLoader = ImageLoader.getInstance(context)

    val imageCacheCleaner: ImageCacheCleaner = object : ImageCacheCleaner {
        override suspend fun clear() = suspendCancellableCoroutine { continuation ->
            imageLoader.invalidate { continuation.resume(Unit) }
        }
    }

    private companion object {
        /** Demo data kept in this repository: see sample-data/image_list.json. */
        const val IMAGE_LIST_URL =
            "https://raw.githubusercontent.com/gr-alia/image-loader/main/sample-data/image_list.json"
    }
}
