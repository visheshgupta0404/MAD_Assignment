package com.example.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * ImageAdapter is a RecyclerView Adapter that displays a grid of image thumbnails.
 * It uses a background thread (AsyncTask) to decode images into thumbnails for better performance.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    /**
     * Interface for handling click events on individual image items.
     */
    public interface OnImageClickListener {
        void onImageClick(File imageFile);
    }

    private final Context context;
    private final List<File> imageFiles;
    private final OnImageClickListener clickListener;

    /** Target size for thumbnails to ensure smooth scrolling and low memory usage. */
    private static final int THUMB_SIZE = 300;

    /**
     * Constructor for ImageAdapter.
     * @param context       The activity context.
     * @param imageFiles    The list of image files to be displayed.
     * @param clickListener The listener for item click events.
     */
    public ImageAdapter(Context context,
                        List<File> imageFiles,
                        OnImageClickListener clickListener) {
        this.context       = context;
        this.imageFiles    = imageFiles;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout for each gallery cell
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        File imageFile = imageFiles.get(position);

        // Set the file name as the caption
        holder.tvImageName.setText(imageFile.getName());

        // Set a placeholder while the thumbnail loads
        holder.imageView.setImageResource(R.drawable.ic_image_placeholder);

        // Load the thumbnail in the background to prevent UI lag
        new ThumbnailLoader(holder.imageView).execute(imageFile.getAbsolutePath());

        // Handle item clicks
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onImageClick(imageFile);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageFiles.size();
    }

    /**
     * ViewHolder holds references to the views for each data item to avoid repeated findViewById calls.
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView  tvImageName;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView   = itemView.findViewById(R.id.ivGridImage);
            tvImageName = itemView.findViewById(R.id.tvGridImageName);
        }
    }

    /**
     * AsyncTask to load image thumbnails in the background.
     * Uses a WeakReference to the ImageView to avoid memory leaks if the view is recycled.
     */
    private static class ThumbnailLoader extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewRef;

        ThumbnailLoader(ImageView imageView) {
            this.imageViewRef = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String filePath = params[0];

            // Decode image dimensions without loading full pixel data
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            // Calculate the scaling factor for the thumbnail
            options.inSampleSize = calculateInSampleSize(options, THUMB_SIZE, THUMB_SIZE);
            options.inJustDecodeBounds = false;

            // Decode the actual downscaled bitmap
            return BitmapFactory.decodeFile(filePath, options);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // Update the ImageView if it's still available
            ImageView imageView = imageViewRef.get();
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }

        /**
         * Calculates the power-of-two scaling factor for image decoding.
         */
        private int calculateInSampleSize(BitmapFactory.Options options,
                                          int reqWidth, int reqHeight) {
            int height = options.outHeight;
            int width  = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                int halfHeight = height / 2;
                int halfWidth  = width  / 2;
                while ((halfHeight / inSampleSize) >= reqHeight
                        && (halfWidth  / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }
    }
}
