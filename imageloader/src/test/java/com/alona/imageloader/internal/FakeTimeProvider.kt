package com.alona.imageloader.internal

// Start on a whole second so tests don't depend on file system timestamp precision.
internal class FakeTimeProvider(var now: Long = 1_700_000_000_000L) : TimeProvider {
    override fun now(): Long = now
}
