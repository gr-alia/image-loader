package com.alona.imageloader

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import com.alona.imageloader.internal.DrawableSource

/**
 * A single image load, created by [ImageLoader.load] and started by [into].
 *
 * ```java
 * ImageLoader.getInstance(context)
 *     .load(url)
 *     .placeholder(R.drawable.placeholder)
 *     .error(R.drawable.error)
 *     .into(imageView);
 * ```
 */
class ImageRequest internal constructor(
    private val loader: ImageLoader,
    internal val url: String,
) {
    internal var placeholder: DrawableSource? = null
        private set
    internal var error: DrawableSource? = null
        private set

    /** Shown while the image loads. */
    fun placeholder(@DrawableRes resId: Int): ImageRequest = apply { placeholder = DrawableSource.of(resId) }

    /** Shown while the image loads. */
    fun placeholder(drawable: Drawable?): ImageRequest = apply { placeholder = DrawableSource.of(drawable) }

    /** Shown if the load fails. Without it, the placeholder stays. */
    fun error(@DrawableRes resId: Int): ImageRequest = apply { error = DrawableSource.of(resId) }

    /** Shown if the load fails. Without it, the placeholder stays. */
    fun error(drawable: Drawable?): ImageRequest = apply { error = DrawableSource.of(drawable) }

    /** Starts loading into [target], replacing any request previously bound to it. */
    @MainThread
    fun into(target: ImageView) {
        loader.enqueue(this, target)
    }
}
