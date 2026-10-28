@file:JvmName("ImageViews")

package com.alona.imageloader

import android.widget.ImageView
import androidx.annotation.MainThread

/**
 * Loads [url] into this view.
 *
 * ```kotlin
 * imageView.load(url) { placeholder(R.drawable.placeholder) }
 * ```
 */
@MainThread
fun ImageView.load(
    url: String,
    loader: ImageLoader = ImageLoader.getInstance(context),
    configure: ImageRequest.() -> Unit = {},
) {
    loader.load(url).apply(configure).into(this)
}
