package com.alona.testproject.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alona.testproject.ImageApp
import com.alona.testproject.R
import com.alona.testproject.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: ImageListViewModel by viewModels { ImageListViewModel.Factory }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ImageListAdapter

    /** Last [ImageListUiState.Content.imagesVersion] rendered; null until content is shown. */
    private var renderedImagesVersion: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()

        adapter = ImageListAdapter((application as ImageApp).container.imageLoader)
        binding.imageList.adapter = adapter
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_clear_cache -> {
                    viewModel.clearCache()
                    true
                }
                else -> false
            }
        }
        binding.retryButton.setOnClickListener { viewModel.loadImages() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ImageListUiState) {
        binding.progress.isVisible = state is ImageListUiState.Loading
        binding.errorGroup.isVisible = state is ImageListUiState.Error
        binding.imageList.isVisible = state is ImageListUiState.Content

        when (state) {
            is ImageListUiState.Content -> renderContent(state)
            is ImageListUiState.Error -> {
                binding.errorText.text = listOfNotNull(getString(R.string.error_loading), state.message)
                    .joinToString("\n")
            }
            ImageListUiState.Loading -> Unit
        }
    }

    private fun renderContent(state: ImageListUiState.Content) {
        adapter.submitList(state.items)
        val previousVersion = renderedImagesVersion
        if (previousVersion != null && previousVersion != state.imagesVersion) {
            adapter.reloadImages()
            Snackbar.make(binding.root, R.string.cache_cleared, Snackbar.LENGTH_SHORT).show()
        }
        renderedImagesVersion = state.imagesVersion
    }

    private fun applyWindowInsets() {
        val listBottomPadding = binding.imageList.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.updatePadding(left = bars.left, right = bars.right)
            binding.toolbar.updatePadding(top = bars.top)
            binding.imageList.updatePadding(bottom = listBottomPadding + bars.bottom)
            insets
        }
    }
}
