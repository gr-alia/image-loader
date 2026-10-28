package com.alona.testproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageItem(
    val id: Int,
    @SerialName("imageUrl") val url: String,
)
