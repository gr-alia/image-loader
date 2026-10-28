package com.alona.imageloader.internal

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/** A placeholder or error image given either as a resource or as a [Drawable] instance. */
internal sealed interface DrawableSource {

    fun resolve(context: Context): Drawable?

    class Resource(@DrawableRes private val resId: Int) : DrawableSource {
        override fun resolve(context: Context): Drawable? = ContextCompat.getDrawable(context, resId)
    }

    class Instance(private val drawable: Drawable) : DrawableSource {
        // A Drawable keeps per-view state (bounds, callback), so each view gets its own copy.
        override fun resolve(context: Context): Drawable =
            drawable.constantState?.newDrawable(context.resources) ?: drawable
    }

    companion object {
        fun of(@DrawableRes resId: Int): DrawableSource? = if (resId != 0) Resource(resId) else null

        fun of(drawable: Drawable?): DrawableSource? = drawable?.let(::Instance)
    }
}
