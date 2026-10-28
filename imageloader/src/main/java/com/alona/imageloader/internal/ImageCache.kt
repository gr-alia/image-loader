package com.alona.imageloader.internal

import java.io.File

/**
 * Two-level (memory + disk) image cache.
 *
 * Invalidation race: every invalidation bumps a generation counter. A request captures
 * [currentGeneration] when it starts and passes it back when storing results; writes from an
 * older generation are rejected, so a download that started before an invalidation can still be
 * shown but never repopulates the cache.
 *
 * Invalidation is cheap and main-safe: memory is updated immediately, while disk work is queued
 * and flushed under the same lock before the next disk access (or via [flushPendingDiskOps]).
 * So a load started right after an invalidation can never read a stale file.
 */
internal class ImageCache<V : Any>(
    private val memory: MemoryCache<V>,
    private val disk: DiskCache,
) {
    private val lock = Any()
    private var generation = 0
    private var pendingClearAll = false
    private val pendingRemovals = mutableSetOf<String>()

    val currentGeneration: Int
        get() = synchronized(lock) { generation }

    fun getMemory(key: String): V? = memory[key]

    fun putMemory(key: String, value: V, generation: Int): Boolean = synchronized(lock) {
        if (generation != this.generation) return false
        memory.put(key, value)
        true
    }

    /** Blocking disk lookup; call from a background thread. */
    fun getDisk(url: String): File? = synchronized(lock) {
        flushLocked()
        disk.get(url)
    }

    fun createTempFile(): File = disk.createTempFile()

    /**
     * Moves a downloaded [tempFile] into the disk cache. Returns false if [generation] is stale
     * or the move failed; the caller still owns [tempFile] in that case.
     */
    fun commitDisk(url: String, tempFile: File, generation: Int): Boolean = synchronized(lock) {
        if (generation != this.generation) return false
        flushLocked()
        disk.commit(url, tempFile)
    }

    fun invalidateAll() = synchronized(lock) {
        generation++
        memory.clear()
        pendingClearAll = true
        pendingRemovals.clear()
    }

    /**
     * Invalidates a single URL. The generation is global, so results of other in-flight requests
     * are also not cached; this keeps the logic simple at the cost of a few extra downloads.
     */
    fun invalidate(url: String) = synchronized(lock) {
        generation++
        memory.removeIf { it.substringBeforeLast(KEY_SEPARATOR) == url }
        pendingRemovals += url
    }

    /** Applies queued disk invalidations; call from a background thread. */
    fun flushPendingDiskOps() = synchronized(lock) { flushLocked() }

    private fun flushLocked() {
        if (pendingClearAll) {
            disk.clear()
            pendingClearAll = false
        }
        pendingRemovals.forEach(disk::remove)
        pendingRemovals.clear()
    }

    companion object {
        private const val KEY_SEPARATOR = '#'

        fun memoryKey(url: String, width: Int, height: Int) = "$url$KEY_SEPARATOR${width}x$height"
    }
}
