package com.alona.testproject.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alona.imageloader.ImageLoader;
import com.alona.testproject.R;
import com.alona.testproject.data.ImageItem;
import com.alona.testproject.databinding.ItemImageBinding;

/**
 * Written in Java on purpose, to show that the image loader API works from Java callers.
 */
public final class ImageViewHolder extends RecyclerView.ViewHolder {

    private final ItemImageBinding binding;
    private final ImageLoader imageLoader;

    public ImageViewHolder(@NonNull ItemImageBinding binding, @NonNull ImageLoader imageLoader) {
        super(binding.getRoot());
        this.binding = binding;
        this.imageLoader = imageLoader;
    }

    public void bind(@NonNull ImageItem item) {
        Context context = binding.getRoot().getContext();
        binding.imageId.setText(context.getString(R.string.image_id, item.getId()));
        binding.image.setContentDescription(context.getString(R.string.image_description, item.getId()));

        imageLoader.load(item.getUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(binding.image);
    }

    public void unbind() {
        imageLoader.cancel(binding.image);
    }
}
