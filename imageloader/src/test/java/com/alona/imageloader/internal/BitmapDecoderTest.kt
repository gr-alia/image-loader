package com.alona.imageloader.internal

import com.alona.imageloader.internal.BitmapDecoder.calculateInSampleSize
import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapDecoderTest {

    @Test
    fun `image not larger than twice the target is not downsampled`() {
        assertEquals(1, calculateInSampleSize(1920, 1080, 1080, 600))
        assertEquals(1, calculateInSampleSize(500, 500, 500, 500))
    }

    @Test
    fun `image smaller than the target is not downsampled`() {
        assertEquals(1, calculateInSampleSize(200, 100, 400, 200))
    }

    @Test
    fun `sample size is the largest power of two keeping both sides above target`() {
        assertEquals(4, calculateInSampleSize(1920, 1080, 400, 200))
        assertEquals(4, calculateInSampleSize(4000, 3000, 1000, 750))
    }

    @Test
    fun `the tighter dimension limits the sample size`() {
        // Width alone would allow 8, height only 2.
        assertEquals(2, calculateInSampleSize(4000, 1000, 400, 400))
    }

    @Test
    fun `unknown target size decodes at full size`() {
        assertEquals(1, calculateInSampleSize(1920, 1080, 0, 0))
    }
}
