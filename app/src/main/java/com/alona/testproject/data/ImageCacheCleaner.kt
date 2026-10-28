package com.alona.testproject.data

/** Clears the image cache. Abstracts the image loader so the ViewModel stays testable. */
interface ImageCacheCleaner {
    /** Suspends until both memory and disk caches are cleared. */
    suspend fun clear()
}
