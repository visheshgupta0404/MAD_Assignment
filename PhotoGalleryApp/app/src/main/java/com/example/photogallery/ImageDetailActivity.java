package com.example.photogallery;

import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * ImageDetailActivity displays a full-size preview of a selected image and its metadata.
 * It also provides functionality to delete the image, handling various Android versions
 * and Scoped Storage requirements.
 */
public class ImageDetailActivity extends AppCompatActivity {

    /** Intent extra key for the absolute path of the image to display. */
    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    
    private File imageFile;

    /**
     * Launcher for handling the system's "Allow delete?" permission dialog.
     * Required for Android 10+ when the app doesn't have direct permission to delete a file in MediaStore.
     */
    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            onDeleteSuccess(imageFile.getAbsolutePath());
                        } else {
                            Toast.makeText(this, R.string.delete_failure, Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        // Retrieve the image path from the intent
        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        if (imagePath == null) {
            Toast.makeText(this, R.string.image_path_not_provided, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Validate that the image file exists
        imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            Toast.makeText(this, R.string.image_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup action bar with a back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_image_details);
        }

        // Initialize UI components
        ImageView imageView   = findViewById(R.id.ivDetailImage);
        TextView  tvName      = findViewById(R.id.tvImageName);
        TextView  tvPath      = findViewById(R.id.tvImagePath);
        TextView  tvSize      = findViewById(R.id.tvImageSize);
        TextView  tvDate      = findViewById(R.id.tvImageDate);
        Button    btnDelete   = findViewById(R.id.btnDeleteImage);

        // Load the image into the view
        loadImageIntoView(imageView);

        // Populate metadata fields
        tvName.setText(getString(R.string.label_image_name, imageFile.getName()));
        tvPath.setText(getString(R.string.label_image_path, imageFile.getAbsolutePath()));
        tvSize.setText(getString(R.string.label_image_size, formatFileSize(imageFile.length())));
        tvDate.setText(getString(R.string.label_image_date, formatDate(imageFile.lastModified())));

        // Set delete button listener
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    /**
     * Decodes and loads the image into the ImageView. 
     * Uses scaling to prevent memory issues for large images.
     */
    private void loadImageIntoView(ImageView imageView) {
        try {
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), boundsOptions);

            // Scale image to a maximum of 1024x1024 to save memory
            boundsOptions.inSampleSize = calculateInSampleSize(boundsOptions, 1024, 1024);
            boundsOptions.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), boundsOptions);

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, R.string.could_not_load_image, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the sample size for downsizing images.
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width  = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth  = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Shows a confirmation dialog before deleting the image.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, imageFile.getName()))
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> deleteImage())
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Attempts to delete the image file.
     * Handles Scoped Storage on newer Android versions by using MediaStore APIs.
     */
    private void deleteImage() {
        String absolutePath = imageFile.getAbsolutePath();

        // Android 11+ with "All Files Access"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            if (imageFile.delete()) {
                onDeleteSuccess(absolutePath);
                return;
            }
        }

        // Fallback to MediaStore for deletion
        Uri uri = getImageUri(absolutePath);
        if (uri == null) {
            // Last attempt: Direct file deletion
            if (imageFile.delete()) {
                onDeleteSuccess(absolutePath);
            } else {
                Toast.makeText(this, R.string.delete_failure, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {
            ContentResolver resolver = getContentResolver();
            int rowsDeleted = resolver.delete(uri, null, null);
            if (rowsDeleted > 0 || !imageFile.exists()) {
                onDeleteSuccess(absolutePath);
            } else {
                Toast.makeText(this, R.string.delete_failure, Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException securityException) {
            handleSecurityException(securityException, uri);
        }
    }

    /**
     * Handles security exceptions related to file deletion in Scoped Storage.
     * Triggers the system's delete confirmation dialog.
     */
    private void handleSecurityException(SecurityException securityException, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), Collections.singletonList(uri));
                deleteLauncher.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.delete_failure, Toast.LENGTH_SHORT).show();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException instanceof RecoverableSecurityException) {
            RecoverableSecurityException rse = (RecoverableSecurityException) securityException;
            deleteLauncher.launch(new IntentSenderRequest.Builder(rse.getUserAction().getActionIntent().getIntentSender()).build());
        } else {
            Toast.makeText(this, R.string.delete_failure, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when an image is successfully deleted. 
     * Refreshes the media scanner and closes the activity.
     */
    private void onDeleteSuccess(String path) {
        MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
        Toast.makeText(this, R.string.delete_success, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Retrieves the MediaStore Uri for a physical file path.
     */
    private Uri getImageUri(String path) {
        String selection = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = {path};
        String[] projection = {MediaStore.MediaColumns._ID};

        Uri[] collections = {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Files.getContentUri("external")
        };

        for (Uri collection : collections) {
            try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                    return Uri.withAppendedPath(collection, String.valueOf(id));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Formats the file size into a human-readable string (e.g., KB, MB).
     */
    private String formatFileSize(long sizeBytes) {
        if (sizeBytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = (int) (Math.log10(sizeBytes) / Math.log10(1024));
        unitIndex = Math.min(unitIndex, units.length - 1);
        double value = sizeBytes / Math.pow(1024, unitIndex);
        return new DecimalFormat("#,##0.##").format(value) + " " + units[unitIndex];
    }

    /**
     * Formats a timestamp into a human-readable date string.
     */
    private String formatDate(long timeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timeMillis));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
