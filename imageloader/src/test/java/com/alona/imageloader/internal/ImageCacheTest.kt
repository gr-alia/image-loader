package com.alona.imageloader.internal

import com.alona.imageloader.internal.ImageCache.Companion.memoryKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

class ImageCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var directory: File
    private lateinit var cache: ImageCache<String>

    @Before
    fun setUp() {
        directory = tempFolder.newFolder("image_cache")
        cache = ImageCache(
            memory = MemoryCache(maxSize = 100) { 1 },
            disk = DiskCache(directory, TimeUnit.HOURS.toMillis(4), FakeTimeProvider()),
        )
    }

    @Test
    fun `invalidateAll clears memory and disk`() {
        store(URL)
        store(OTHER_URL)

        cache.invalidateAll()

        assertNull(cache.getMemory(KEY))
        assertNull(cache.getMemory(OTHER_KEY))
        assertNull(cache.getDisk(URL))
        assertNull(cache.getDisk(OTHER_URL))
    }

    @Test
    fun `disk entries are deleted once pending operations are flushed`() {
        store(URL)

        cache.invalidateAll()
        cache.flushPendingDiskOps()

        assertTrue(directory.listFiles().isNullOrEmpty())
    }

    @Test
    fun `invalidate url removes only that url including all sizes`() {
        store(URL)
        cache.putMemory(memoryKey(URL, 50, 50), "small", cache.currentGeneration)
        store(OTHER_URL)

        cache.invalidate(URL)

        assertNull(cache.getMemory(KEY))
        assertNull(cache.getMemory(memoryKey(URL, 50, 50)))
        assertNull(cache.getDisk(URL))
        assertEquals("bitmap", cache.getMemory(OTHER_KEY))
        assertNotNull(cache.getDisk(OTHER_URL))
    }

    @Test
    fun `results of the current generation are stored`() {
        val generation = cache.currentGeneration

        assertTrue(cache.putMemory(KEY, "bitmap", generation))
        assertTrue(cache.commitDisk(URL, download("bytes"), generation))

        assertEquals("bitmap", cache.getMemory(KEY))
        assertEquals("bytes", cache.getDisk(URL)?.readText())
    }

    @Test
    fun `results started before invalidateAll are not stored`() {
        val staleGeneration = cache.currentGeneration
        val temp = download("bytes")

        cache.invalidateAll()

        assertFalse(cache.putMemory(KEY, "bitmap", staleGeneration))
        assertFalse(cache.commitDisk(URL, temp, staleGeneration))
        assertNull(cache.getMemory(KEY))
        assertNull(cache.getDisk(URL))
        // The caller still owns the download, so it can decode it for the view.
        assertEquals("bytes", temp.readText())
    }

    @Test
    fun `results started before invalidate url are not stored`() {
        val staleGeneration = cache.currentGeneration

        cache.invalidate(URL)

        assertFalse(cache.putMemory(KEY, "bitmap", staleGeneration))
        assertFalse(cache.commitDisk(URL, download("bytes"), staleGeneration))
        assertNull(cache.getDisk(URL))
    }

    private fun store(url: String) {
        val generation = cache.currentGeneration
        cache.putMemory(memoryKey(url, 100, 100), "bitmap", generation)
        cache.commitDisk(url, download("bytes"), generation)
    }

    private fun download(content: String) = cache.createTempFile().apply { writeText(content) }

    private companion object {
        const val URL = "https://example.com/a.jpg"
        const val OTHER_URL = "https://example.com/b.jpg"
        val KEY = memoryKey(URL, 100, 100)
        val OTHER_KEY = memoryKey(OTHER_URL, 100, 100)
    }
}
