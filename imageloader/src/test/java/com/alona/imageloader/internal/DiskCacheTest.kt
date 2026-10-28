package com.alona.imageloader.internal

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

class DiskCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val ttl = TimeUnit.HOURS.toMillis(4)
    private val clock = FakeTimeProvider()
    private lateinit var directory: File
    private lateinit var cache: DiskCache

    @Before
    fun setUp() {
        directory = tempFolder.newFolder("image_cache")
        cache = DiskCache(directory, ttl, clock)
    }

    @Test
    fun `missing entry returns null`() {
        assertNull(cache.get(URL))
    }

    @Test
    fun `fresh entry is returned`() {
        cache.put(URL, "image")

        clock.now += ttl - 1

        assertEquals("image", cache.get(URL)?.readText())
    }

    @Test
    fun `expired entry returns null and is deleted`() {
        cache.put(URL, "image")

        clock.now += ttl

        assertNull(cache.get(URL))
        assertTrue(directory.listFiles().isNullOrEmpty())
    }

    @Test
    fun `rewriting an entry refreshes its timestamp`() {
        cache.put(URL, "old")
        clock.now += TimeUnit.HOURS.toMillis(3)
        cache.put(URL, "new")

        clock.now += TimeUnit.HOURS.toMillis(3)

        assertEquals("new", cache.get(URL)?.readText())
    }

    @Test
    fun `entries are stored per url`() {
        cache.put(URL, "a")
        cache.put(OTHER_URL, "b")

        assertEquals("a", cache.get(URL)?.readText())
        assertEquals("b", cache.get(OTHER_URL)?.readText())
    }

    @Test
    fun `remove deletes only that entry`() {
        cache.put(URL, "a")
        cache.put(OTHER_URL, "b")

        cache.remove(URL)

        assertNull(cache.get(URL))
        assertNotNull(cache.get(OTHER_URL))
    }

    @Test
    fun `clear deletes entries but keeps in-flight temp files`() {
        cache.put(URL, "a")
        val inFlight = cache.createTempFile()

        cache.clear()

        assertNull(cache.get(URL))
        assertTrue(inFlight.exists())
        assertFalse(directory.listFiles().orEmpty().any { it != inFlight })
    }

    private fun DiskCache.put(url: String, content: String) {
        val temp = createTempFile().apply { writeText(content) }
        assertTrue(commit(url, temp))
    }

    private companion object {
        const val URL = "https://example.com/a.jpg"
        const val OTHER_URL = "https://example.com/b.jpg"
    }
}
