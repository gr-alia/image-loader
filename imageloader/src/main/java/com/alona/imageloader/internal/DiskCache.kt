package com.alona.imageloader.internal

import java.io.File
import java.security.MessageDigest

/**
 * File-per-URL disk cache. Entries are named by the SHA-256 of the URL and expire [ttlMillis]
 * after they were written (tracked via the file's last-modified time).
 *
 * Not thread-safe on its own; [ImageCache] serializes access.
 */
internal class DiskCache(
    private val directory: File,
    private val ttlMillis: Long,
    private val timeProvider: TimeProvider = TimeProvider.SYSTEM,
) {

    /** Returns the entry for [url] if it exists and hasn't expired. Expired entries are deleted. */
    fun get(url: String): File? {
        val file = fileFor(url)
        if (!file.exists()) return null
        if (timeProvider.now() - file.lastModified() >= ttlMillis) {
            file.delete()
            return null
        }
        return file
    }

    /** Creates an empty file inside the cache directory for a download to write into. */
    fun createTempFile(): File {
        directory.mkdirs()
        return File.createTempFile("download", TEMP_SUFFIX, directory)
    }

    /** Moves a fully written [tempFile] into place as the entry for [url]. */
    fun commit(url: String, tempFile: File): Boolean {
        val target = fileFor(url)
        if (!tempFile.renameTo(target)) return false
        target.setLastModified(timeProvider.now())
        return true
    }

    fun remove(url: String) {
        fileFor(url).delete()
    }

    /** Deletes all entries. Temp files are kept so in-flight downloads aren't broken. */
    fun clear() {
        directory.listFiles { file -> !file.name.endsWith(TEMP_SUFFIX) }?.forEach { it.delete() }
    }

    private fun fileFor(url: String) = File(directory, sha256(url))

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TEMP_SUFFIX = ".tmp"
    }
}
