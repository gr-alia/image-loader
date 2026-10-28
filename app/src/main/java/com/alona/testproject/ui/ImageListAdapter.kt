package com.alona.testproject.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.alona.imageloader.ImageLoader
import com.alona.testproject.data.ImageItem
import com.alona.testproject.databinding.ItemImageBinding

class ImageListAdapter(
    private val imageLoader: ImageLoader,
) : ListAdapter<ImageItem, ImageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding, imageLoader)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        holder.unbind()
    }

    /** Rebinds all items so their images are loaded again, e.g. after the cache was cleared. */
    fun reloadImages() {
        notifyItemRangeChanged(0, itemCount)
    }

    private object DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) = oldItem == newItem
    }
}
