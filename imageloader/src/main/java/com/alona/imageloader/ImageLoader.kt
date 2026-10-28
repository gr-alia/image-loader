package com.alona.imageloader

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import androidx.annotation.MainThread
import com.alona.imageloader.internal.BitmapDecoder
import com.alona.imageloader.internal.DiskCache
import com.alona.imageloader.internal.ImageCache
import com.alona.imageloader.internal.ImageDownloader
import com.alona.imageloader.internal.MemoryCache
import com.alona.imageloader.internal.Size
import com.alona.imageloader.internal.awaitSize
import com.alona.imageloader.internal.measuredSizeOrNull
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException

/**
 * Downloads images into [ImageView]s with a memory + disk cache.
 *
 * - Memory: LRU of decoded bitmaps, keyed by URL and target size.
 * - Disk: original bytes per URL, valid for the configured TTL (4 hours by default).
 * - Concurrent loads of the same URL share one download.
 * - Safe in RecyclerView: rebinding a view cancels its previous request, and a result is only
 *   applied if the view is still bound to the request that produced it.
 *
 * Use one instance per disk cache directory, typically [getInstance].
 */
class ImageLoader private constructor(
    private val cache: ImageCache<Bitmap>,
    private val downloader: ImageDownloader,
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, e -> Log.e(TAG, "Unexpected error", e) },
    )
    private val networkDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_DOWNLOADS)
    private val inFlight = HashMap<InFlightKey, Deferred<ByteArray>>()

    /** Creates a request for [url]; call [ImageRequest.into] to start it. */
    fun load(url: String): ImageRequest = ImageRequest(this, url)

    /** Cancels the request currently bound to [target], if any. */
    @MainThread
    fun cancel(target: ImageView) {
        (target.getTag(R.id.imageloader_request) as? Job)?.cancel()
        target.setTag(R.id.imageloader_request, null)
    }

    /**
     * Clears the memory and disk caches. Memory is cleared immediately; disk entries are deleted
     * in the background, after which [onComplete] runs on the main thread. Loads that were already
     * running still deliver their result but don't write it to the cache.
     */
    @JvmOverloads
    fun invalidate(onComplete: Runnable? = null) {
        cache.invalidateAll()
        flushDiskCache(onComplete)
    }

    /** Removes [url] from the memory and disk caches. See [invalidate]. */
    @JvmOverloads
    fun invalidate(url: String, onComplete: Runnable? = null) {
        cache.invalidate(url)
        flushDiskCache(onComplete)
    }

    @MainThread
    internal fun enqueue(request: ImageRequest, target: ImageView) {
        cancel(target)

        val cached = target.measuredSizeOrNull?.let { cache.getMemory(memoryKey(request.url, it)) }
        if (cached != null) {
            Log.d(TAG, "MEMORY ${request.url}")
            target.setImageBitmap(cached)
            return
        }

        // Always reset the view, so a recycled cell never shows the previous image.
        target.setImageDrawable(request.placeholder?.resolve(target.context))
        // Started lazily so the tag is set before the request can check it.
        val job = scope.launch(start = CoroutineStart.LAZY) { execute(request, target) }
        target.setTag(R.id.imageloader_request, job)
        job.start()
    }

    private suspend fun execute(request: ImageRequest, target: ImageView) {
        val job = currentCoroutineContext().job
        fun isCurrent() = target.getTag(R.id.imageloader_request) === job
        try {
            val size = target.awaitSize()
            val key = memoryKey(request.url, size)
            val bitmap = cache.getMemory(key)
                ?: withContext(Dispatchers.IO) { fetch(request.url, key, size) }
            if (isCurrent()) target.setImageBitmap(bitmap)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load ${request.url}", e)
            if (isCurrent()) request.error?.let { target.setImageDrawable(it.resolve(target.context)) }
        } finally {
            if (isCurrent()) target.setTag(R.id.imageloader_request, null)
        }
    }

    /** Disk → network. Runs on IO. */
    private suspend fun fetch(url: String, key: String, size: Size): Bitmap {
        val generation = cache.currentGeneration
        val bitmap = cache.getDisk(url)
            ?.let { BitmapDecoder.decode(it, size.width, size.height) }
            ?.also { Log.d(TAG, "DISK $url") }
            ?: download(url, generation)
                .let { BitmapDecoder.decode(it, size.width, size.height) }
                ?.also { Log.d(TAG, "NETWORK $url") }
            ?: throw IOException("Failed to decode $url")
        cache.putMemory(key, bitmap, generation)
        return bitmap
    }

    /**
     * Downloads [url], sharing the download with concurrent requests of the same generation.
     * The download runs in the loader's scope, so a single cancelled view doesn't abort it for
     * other waiters.
     */
    private suspend fun download(url: String, generation: Int): ByteArray {
        val key = InFlightKey(url, generation)
        val deferred = synchronized(inFlight) {
            inFlight[key]?.also { Log.d(TAG, "JOIN $url") }
                ?: scope.async(networkDispatcher) {
                    try {
                        downloader.download(url).also { storeOnDisk(url, it, generation) }
                    } finally {
                        synchronized(inFlight) { inFlight.remove(key) }
                    }
                }.also { inFlight[key] = it }
        }
        return deferred.await()
    }

    private fun storeOnDisk(url: String, bytes: ByteArray, generation: Int) {
        try {
            val temp = cache.createTempFile()
            temp.writeBytes(bytes)
            if (!cache.commitDisk(url, temp, generation)) temp.delete()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to write disk cache for $url", e)
        }
    }

    private fun flushDiskCache(onComplete: Runnable?) {
        scope.launch {
            withContext(Dispatchers.IO) { cache.flushPendingDiskOps() }
            onComplete?.run()
        }
    }

    private fun memoryKey(url: String, size: Size) = ImageCache.memoryKey(url, size.width, size.height)

    private data class InFlightKey(val url: String, val generation: Int)

    /** Configures a custom [ImageLoader]. */
    class Builder(context: Context) {
        private val appContext = context.applicationContext
        private var memoryCacheMaxBytes = (Runtime.getRuntime().maxMemory() / 8).toInt()
        private var diskCacheDirectory = File(appContext.cacheDir, DEFAULT_DISK_CACHE_DIR)
        private var cacheTtl = DEFAULT_CACHE_TTL

        /** Default: 1/8 of the app's max heap. */
        fun memoryCacheMaxBytes(bytes: Int): Builder = apply {
            require(bytes > 0) { "Memory cache size must be positive" }
            memoryCacheMaxBytes = bytes
        }

        /** Default: `cacheDir/image_cache`. */
        fun diskCacheDirectory(directory: File): Builder = apply { diskCacheDirectory = directory }

        /** How long downloaded images stay valid. Default: 4 hours. */
        fun cacheTtl(ttl: Duration): Builder = apply {
            require(!ttl.isNegative && !ttl.isZero) { "TTL must be positive" }
            cacheTtl = ttl
        }

        fun build(): ImageLoader {
            val memory = MemoryCache<Bitmap>(maxSize = memoryCacheMaxBytes / 1024) {
                maxOf(1, it.allocationByteCount / 1024)
            }
            val disk = DiskCache(diskCacheDirectory, cacheTtl.toMillis())
            return ImageLoader(ImageCache(memory, disk), ImageDownloader())
        }
    }

    companion object {
        private const val TAG = "ImageLoader"
        private const val MAX_PARALLEL_DOWNLOADS = 4
        private const val DEFAULT_DISK_CACHE_DIR = "image_cache"
        private val DEFAULT_CACHE_TTL: Duration = Duration.ofHours(4)

        @Volatile
        private var instance: ImageLoader? = null

        /** Shared instance with the default configuration. */
        @JvmStatic
        fun getInstance(context: Context): ImageLoader =
            instance ?: synchronized(this) {
                instance ?: Builder(context).build().also { instance = it }
            }
    }
}
