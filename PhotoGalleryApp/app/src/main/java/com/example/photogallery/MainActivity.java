package com.example.photogallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity is the entry point of the application.
 * It provides functionality to take photos, choose a save directory, and navigate to the gallery.
 * It also handles the complex permission requirements for different Android versions,
 * including the MANAGE_EXTERNAL_STORAGE permission for Android 11+.
 */
public class MainActivity extends AppCompatActivity {

    private static final String DEFAULT_FOLDER = "PhotoGalleryApp";
    private String currentPhotoPath;
    private File selectedSaveFolder;
    private TextView tvSelectedFolder;

    /**
     * Launcher for the system camera activity.
     * Handles the result of the photo capture process.
     */
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            handlePhotoCaptured();
                        } else {
                            Toast.makeText(MainActivity.this, "Photo capture cancelled.", Toast.LENGTH_SHORT).show();
                        }
                    });

    /**
     * Launcher for the directory picker.
     * Allows the user to select a folder where photos will be saved.
     */
    private final ActivityResultLauncher<Intent> folderPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            handleFolderSelected(result.getData());
                        }
                    });

    /**
     * Launcher for requesting multiple runtime permissions (Camera, Storage).
     */
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean allGranted = true;
                        for (Boolean granted : permissions.values()) {
                            if (!granted) {
                                allGranted = false;
                                break;
                            }
                        }
                        // Check for MANAGE_EXTERNAL_STORAGE after standard permissions are handled
                        checkManageStoragePermission();
                    });

    /**
     * Launcher for the "All Files Access" settings screen (Android 11+).
     */
    private final ActivityResultLauncher<Intent> manageStorageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (Environment.isExternalStorageManager()) {
                                Toast.makeText(this, "Full storage access granted!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Full storage access is needed to delete existing gallery photos.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize default save folder in the Pictures directory
        selectedSaveFolder = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                DEFAULT_FOLDER);

        if (!selectedSaveFolder.exists()) {
            selectedSaveFolder.mkdirs();
        }

        // Initialize UI components
        tvSelectedFolder = findViewById(R.id.tvSelectedFolder);
        Button btnTakePhoto     = findViewById(R.id.btnTakePhoto);
        Button btnChooseFolder  = findViewById(R.id.btnChooseFolder);
        Button btnViewGallery   = findViewById(R.id.btnViewGallery);

        updateFolderDisplay();

        // Set click listeners for buttons
        btnTakePhoto.setOnClickListener(v -> checkPermissionsAndTakePhoto());
        btnChooseFolder.setOnClickListener(v -> openFolderPicker());
        btnViewGallery.setOnClickListener(v -> openGallery());

        // Perform initial permission check on startup
        requestRequiredPermissions();
    }

    /**
     * Determines which permissions are missing and requests them from the user.
     * Handles differences between Android versions (Tiramisu+, R, and older).
     */
    private void requestRequiredPermissions() {
        List<String> needed = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12 and below use standard storage permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                // WRITE_EXTERNAL_STORAGE is only needed for API 28 and below
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        }

        if (!needed.isEmpty()) {
            permissionLauncher.launch(needed.toArray(new String[0]));
        } else {
            checkManageStoragePermission();
        }
    }

    /**
     * Prompts the user to grant "All Files Access" on Android 11+ if not already granted.
     * This is required for full file system operations like deleting images.
     */
    private void checkManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle("Full Storage Access Required")
                        .setMessage("To delete photos already in your gallery, this app needs \"All Files Access\".\n\nPlease enable it in the settings screen that follows.")
                        .setPositiveButton("Go to Settings", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.addCategory("android.intent.category.DEFAULT");
                                intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                                manageStorageLauncher.launch(intent);
                            } catch (Exception e) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                manageStorageLauncher.launch(intent);
                            }
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }
        }
    }

    /**
     * Verifies that all necessary permissions are granted before launching the camera.
     */
    private void checkPermissionsAndTakePhoto() {
        boolean cameraOk = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storageOk;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storageOk = Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storageOk = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            storageOk = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        if (cameraOk && storageOk) {
            dispatchTakePictureIntent();
        } else {
            requestRequiredPermissions();
            Toast.makeText(this, "Permissions required to take and save photos.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a temporary file and launches the camera intent to capture a photo.
     */
    private void dispatchTakePictureIntent() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "PHOTO_" + timeStamp;
        try {
            File storageDir = getCacheDir();
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            currentPhotoPath = imageFile.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraLauncher.launch(takePictureIntent);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating temp file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Processes the captured photo by moving it from the temporary cache to the selected save folder.
     */
    private void handlePhotoCaptured() {
        if (currentPhotoPath == null) return;
        File tempFile = new File(currentPhotoPath);
        if (!tempFile.exists()) return;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File destFile = new File(selectedSaveFolder, "PHOTO_" + timeStamp + ".jpg");

        try {
            if (moveFile(tempFile, destFile)) {
                // Notify the system that a new file has been created so it appears in other apps
                MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, null, null);
                Toast.makeText(this, "Photo saved to " + destFile.getName(), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save photo.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Moves a file from source to destination. If rename fails, it performs a manual copy.
     * @param source The source file to move.
     * @param dest   The destination file path.
     * @return True if successful, false otherwise.
     * @throws IOException If an I/O error occurs during copying.
     */
    private boolean moveFile(File source, File dest) throws IOException {
        if (source.renameTo(dest)) return true;
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
        return source.delete();
    }

    /**
     * Opens the system directory picker to allow the user to select a folder.
     */
    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.externalstorage.documents/tree/primary%3APictures"));
        folderPickerLauncher.launch(intent);
    }

    /**
     * Resolves the selected URI from the folder picker to a physical path and updates the save folder.
     */
    private void handleFolderSelected(@NonNull Intent data) {
        Uri treeUri = data.getData();
        if (treeUri == null) return;
        String path = UriToPathHelper.getPathFromUri(this, treeUri);
        if (path != null) {
            selectedSaveFolder = new File(path);
            if (!selectedSaveFolder.exists()) selectedSaveFolder.mkdirs();
            updateFolderDisplay();
        }
    }

    /**
     * Launches the GalleryActivity to view photos in the currently selected folder.
     */
    private void openGallery() {
        if (selectedSaveFolder == null || !selectedSaveFolder.exists()) {
            Toast.makeText(this, "Select a valid folder first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(GalleryActivity.EXTRA_FOLDER_PATH, selectedSaveFolder.getAbsolutePath());
        startActivity(intent);
    }

    /**
     * Updates the UI text to display the currently selected save folder path.
     */
    private void updateFolderDisplay() {
        if (selectedSaveFolder != null) {
            tvSelectedFolder.setText(String.format("Save folder: %s", selectedSaveFolder.getAbsolutePath()));
        }
    }
}
