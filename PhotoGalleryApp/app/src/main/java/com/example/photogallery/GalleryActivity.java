package com.example.photogallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GalleryActivity displays all images found within a specific folder.
 * It uses a RecyclerView with a GridLayoutManager to show images in a 3-column grid.
 */
public class GalleryActivity extends AppCompatActivity {

    /** Intent extra key for the folder path to display. */
    public static final String EXTRA_FOLDER_PATH = "extra_folder_path";
    
    private static final int GRID_COLUMNS = 3;
    
    /** Supported image file extensions. */
    private static final String[] IMAGE_EXTENSIONS = {
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    };

    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private View emptyMessageLayout;
    private TextView tvFolderPath;

    private File folder;
    private final List<File> imageFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // Retrieve the folder path from the intent
        String folderPath = getIntent().getStringExtra(EXTRA_FOLDER_PATH);
        if (folderPath == null) {
            Toast.makeText(this, "No folder path provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Validate the folder path
        folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            Toast.makeText(this, "Invalid folder.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        tvFolderPath       = findViewById(R.id.tvGalleryFolderPath);
        emptyMessageLayout = findViewById(R.id.tvEmptyMessage);
        recyclerView       = findViewById(R.id.recyclerViewImages);

        tvFolderPath.setText(folder.getAbsolutePath());

        // Setup the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Photo Gallery");
        }

        // Configure the RecyclerView with a grid layout and adapter
        recyclerView.setLayoutManager(new GridLayoutManager(this, GRID_COLUMNS));
        imageAdapter = new ImageAdapter(this, imageFiles, imageFile -> {
            // Handle clicking an image to view details
            Intent intent = new Intent(GalleryActivity.this, ImageDetailActivity.class);
            intent.putExtra(ImageDetailActivity.EXTRA_IMAGE_PATH, imageFile.getAbsolutePath());
            startActivity(intent);
        });
        recyclerView.setAdapter(imageAdapter);

        // Load initial set of images
        loadImagesFromFolder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the image list when returning to this activity
        loadImagesFromFolder();
    }

    /**
     * Scans the selected folder for image files and updates the RecyclerView.
     * Sorts images by last modified date (newest first).
     */
    private void loadImagesFromFolder() {
        imageFiles.clear();
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isImageFile(file)) {
                    imageFiles.add(file);
                }
            }
            // Sort images so the most recent ones appear first
            Collections.sort(imageFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        }

        imageAdapter.notifyDataSetChanged();

        // Show an empty message if no images were found
        if (imageFiles.isEmpty()) {
            emptyMessageLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyMessageLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Checks if a file has a supported image extension.
     * @param file The file to check.
     * @return True if it is an image, false otherwise.
     */
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * Handles the up navigation (back button in action bar).
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
