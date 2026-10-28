package com.alona.imageloader.internal

/** Source of the current time in milliseconds. Injectable so cache expiry can be tested. */
internal fun interface TimeProvider {
    fun now(): Long

    companion object {
        val SYSTEM = TimeProvider { System.currentTimeMillis() }
    }
}
