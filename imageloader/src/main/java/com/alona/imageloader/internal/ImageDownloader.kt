package com.alona.imageloader.internal

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal class ImageDownloader(
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 15_000,
) {

    /** Blocking download of [url]. Throws [IOException] on network errors and non-2xx responses. */
    fun download(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            connection.instanceFollowRedirects = true
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for $url")
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
