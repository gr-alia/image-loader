package com.alona.testproject.data

interface ImageRepository {
    suspend fun getImages(): List<ImageItem>
}

class DefaultImageRepository(private val api: ImageListApi) : ImageRepository {
    override suspend fun getImages(): List<ImageItem> = api.fetchImages()
}
