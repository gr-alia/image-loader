package com.alona.imageloader.internal

import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal data class Size(val width: Int, val height: Int)

internal val ImageView.measuredSizeOrNull: Size?
    get() = if (width > 0 && height > 0) Size(width, height) else null

/**
 * Returns the view's size, suspending until the next layout pass if it hasn't been measured yet.
 * Dimensions that are still zero after layout (e.g. wrap_content) fall back to the screen size.
 */
internal suspend fun ImageView.awaitSize(): Size {
    measuredSizeOrNull?.let { return it }
    suspendCancellableCoroutine { continuation ->
        val listener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
            ) {
                view.removeOnLayoutChangeListener(this)
                continuation.resume(Unit)
            }
        }
        addOnLayoutChangeListener(listener)
        continuation.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
    }
    val display = resources.displayMetrics
    return Size(
        width = width.takeIf { it > 0 } ?: display.widthPixels,
        height = height.takeIf { it > 0 } ?: display.heightPixels,
    )
}
