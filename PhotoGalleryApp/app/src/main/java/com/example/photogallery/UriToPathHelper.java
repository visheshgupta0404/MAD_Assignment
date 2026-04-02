package com.example.photogallery;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;

import java.io.File;

/**
 * UriToPathHelper is a utility class for converting Android document-tree URIs
 * into physical file system paths.
 * 
 * This is necessary because the Storage Access Framework (SAF) returns content URIs
 * which cannot be directly used with standard java.io.File operations.
 */
public class UriToPathHelper {

    /**
     * Resolves a document-tree URI to an absolute file path.
     *
     * @param context The application context.
     * @param uri     The URI to resolve.
     * @return The absolute path as a String, or null if it cannot be resolved.
     */
    public static String getPathFromUri(Context context, Uri uri) {

        // Document tree URIs were introduced in Lollipop (API 21)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Extract the document ID from the tree URI (e.g., "primary:Pictures/MyFolder")
                String docId = DocumentsContract.getTreeDocumentId(uri);
                String[] split = docId.split(":");

                String type = split[0];
                String relativePath = split.length > 1 ? split[1] : "";

                // Handle primary internal storage
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory()
                            + File.separator + relativePath;
                }

                // Handle secondary storage (SD cards)
                // Attempting common mount points for external storage
                String[] sdcardPaths = {
                        "/storage/" + type,
                        "/mnt/sdcard/" + type,
                        "/mnt/extSdCard/" + type
                };

                for (String sdPath : sdcardPaths) {
                    File sdFile = new File(sdPath);
                    if (sdFile.exists()) {
                        return sdFile.getAbsolutePath()
                                + (relativePath.isEmpty() ? "" : File.separator + relativePath);
                    }
                }

            } catch (Exception e) {
                // Return null if parsing fails
                e.printStackTrace();
            }
        }

        return null;
    }
}
