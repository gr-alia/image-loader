package com.alona.testproject.ui

import com.alona.testproject.data.ImageCacheCleaner
import com.alona.testproject.data.ImageItem
import com.alona.testproject.data.ImageRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ImageListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeImageRepository()
    private val cacheCleaner = FakeImageCacheCleaner()

    private fun createViewModel() = ImageListViewModel(repository, cacheCleaner)

    @Test
    fun `shows loading then content`() = runTest {
        val response = CompletableDeferred<List<ImageItem>>()
        repository.response = { response.await() }

        val viewModel = createViewModel()
        assertEquals(ImageListUiState.Loading, viewModel.uiState.value)

        response.complete(ITEMS)
        assertEquals(ImageListUiState.Content(ITEMS), viewModel.uiState.value)
    }

    @Test
    fun `shows error when loading fails`() = runTest {
        repository.response = { throw IOException("No network") }

        val viewModel = createViewModel()

        assertEquals(ImageListUiState.Error("No network"), viewModel.uiState.value)
    }

    @Test
    fun `loading again after an error shows content`() = runTest {
        repository.response = { throw IOException("No network") }
        val viewModel = createViewModel()

        repository.response = { ITEMS }
        viewModel.loadImages()

        assertEquals(ImageListUiState.Content(ITEMS), viewModel.uiState.value)
        assertEquals(2, repository.calls)
    }

    @Test
    fun `clearing the cache bumps the images version once the cache is cleared`() = runTest {
        repository.response = { ITEMS }
        val viewModel = createViewModel()
        val cleared = CompletableDeferred<Unit>()
        cacheCleaner.onClear = { cleared.await() }

        viewModel.clearCache()
        assertEquals(1, cacheCleaner.calls)
        assertEquals(ImageListUiState.Content(ITEMS, imagesVersion = 0), viewModel.uiState.value)

        cleared.complete(Unit)
        assertEquals(ImageListUiState.Content(ITEMS, imagesVersion = 1), viewModel.uiState.value)
    }

    @Test
    fun `clearing the cache without content keeps the state`() = runTest {
        repository.response = { throw IOException("No network") }
        val viewModel = createViewModel()

        viewModel.clearCache()

        assertEquals(1, cacheCleaner.calls)
        assertEquals(ImageListUiState.Error("No network"), viewModel.uiState.value)
    }

    private class FakeImageRepository : ImageRepository {
        var response: suspend () -> List<ImageItem> = { emptyList() }
        var calls = 0

        override suspend fun getImages(): List<ImageItem> {
            calls++
            return response()
        }
    }

    private class FakeImageCacheCleaner : ImageCacheCleaner {
        var onClear: suspend () -> Unit = {}
        var calls = 0

        override suspend fun clear() {
            calls++
            onClear()
        }
    }

    private companion object {
        val ITEMS = listOf(
            ImageItem(0, "https://example.com/0.jpg"),
            ImageItem(1, "https://example.com/1.jpg"),
        )
    }
}
