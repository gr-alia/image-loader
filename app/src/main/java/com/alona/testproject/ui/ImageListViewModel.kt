package com.alona.testproject.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alona.testproject.ImageApp
import com.alona.testproject.data.ImageCacheCleaner
import com.alona.testproject.data.ImageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ImageListViewModel(
    private val repository: ImageRepository,
    private val cacheCleaner: ImageCacheCleaner,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImageListUiState>(ImageListUiState.Loading)
    val uiState: StateFlow<ImageListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadImages()
    }

    /** Loads the image list; also used to retry after an error. */
    fun loadImages() {
        loadJob?.cancel()
        _uiState.value = ImageListUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = try {
                ImageListUiState.Content(repository.getImages())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ImageListUiState.Error(e.message)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            cacheCleaner.clear()
            _uiState.update { state ->
                if (state is ImageListUiState.Content) {
                    state.copy(imagesVersion = state.imagesVersion + 1)
                } else {
                    state
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ImageApp).container
                ImageListViewModel(container.imageRepository, container.imageCacheCleaner)
            }
        }
    }
}
