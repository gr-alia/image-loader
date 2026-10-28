package com.alona.testproject.data

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageListApiParsingTest {

    private val api = ImageListApi(url = "unused")

    @Test
    fun `parses id and imageUrl`() {
        val json = """
            [
               { "id": 0, "imageUrl": "https://example.com/0.jpg" },
               { "id": 1, "imageUrl": "https://example.com/1.jpg" }
            ]
        """.trimIndent()

        assertEquals(
            listOf(
                ImageItem(id = 0, url = "https://example.com/0.jpg"),
                ImageItem(id = 1, url = "https://example.com/1.jpg"),
            ),
            api.parse(json),
        )
    }

    @Test
    fun `ignores unknown fields`() {
        val json = """[ { "id": 7, "imageUrl": "https://example.com/7.jpg", "title": "x" } ]"""

        assertEquals(listOf(ImageItem(7, "https://example.com/7.jpg")), api.parse(json))
    }

    @Test(expected = SerializationException::class)
    fun `missing required field throws`() {
        api.parse("""[ { "id": 7 } ]""")
    }
}
