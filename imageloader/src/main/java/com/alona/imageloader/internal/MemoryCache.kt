package com.alona.imageloader.internal

import androidx.collection.LruCache

/**
 * Size-bounded LRU memory cache. Generic over the value type so it can be unit-tested
 * without Android bitmaps; in production `V` is `Bitmap`, sized in kilobytes.
 */
internal class MemoryCache<V : Any>(
    maxSize: Int,
    private val valueSize: (V) -> Int,
) {
    private val cache = object : LruCache<String, V>(maxSize) {
        override fun sizeOf(key: String, value: V): Int = valueSize(value)
    }

    operator fun get(key: String): V? = cache[key]

    fun put(key: String, value: V) {
        cache.put(key, value)
    }

    fun removeIf(predicate: (key: String) -> Boolean) {
        cache.snapshot().keys.filter(predicate).forEach { cache.remove(it) }
    }

    fun clear() {
        cache.evictAll()
    }
}
