package com.alona.testproject.ui

import com.alona.testproject.data.ImageItem

sealed interface ImageListUiState {

    data object Loading : ImageListUiState

    /** [imagesVersion] is bumped after the image cache is cleared, so the UI knows to reload. */
    data class Content(
        val items: List<ImageItem>,
        val imagesVersion: Int = 0,
    ) : ImageListUiState

    data class Error(val message: String?) : ImageListUiState
}
