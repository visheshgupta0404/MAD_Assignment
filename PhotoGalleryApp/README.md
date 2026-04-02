# Photo Gallery App — Android

A complete Android application that lets users capture photos with the device camera,
save them to a chosen folder, and browse/manage those images in a grid gallery view.

---

## Features

### a) Camera & Save
- Tap **Take Photo** to open the device camera.
- The captured photo is saved to the currently selected folder (`Pictures/PhotoGalleryApp` by default).
- Permissions for `CAMERA`, `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES`, and `WRITE_EXTERNAL_STORAGE` (Android ≤ 9) are requested at runtime.
- A `FileProvider` is used to safely share file URIs with the camera app (required on Android 7+).

### b) Folder Picker & Image Grid
- Tap **Choose Folder** to open Android's built-in document-tree folder picker.
- Tap **View Gallery** to open `GalleryActivity` which scans the selected folder for images.
- Images are displayed in a **3-column grid** using `RecyclerView` + `GridLayoutManager`.
- Thumbnails are decoded on a background thread (`AsyncTask`) to keep scrolling smooth.
- Supported formats: JPG, JPEG, PNG, GIF, BMP, WEBP.

### c) Image Details & Delete
- Tap any image in the grid to open `ImageDetailActivity`.
- Details shown: **Name**, **Path**, **Size** (human-readable), **Date Taken**.
- Tap **Delete Image** → a confirmation `AlertDialog` appears.
- On confirmation, the file is deleted, `MediaStore` is notified, and the app returns to the gallery (which auto-refreshes).

---

## Project Structure

```
PhotoGalleryApp/
├── app/
│   ├── build.gradle                        # Module build config
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml             # Permissions + activity declarations
│       ├── java/com/example/photogallery/
│       │   ├── MainActivity.java           # Home screen: camera + folder picker
│       │   ├── GalleryActivity.java        # Image grid display
│       │   ├── ImageDetailActivity.java    # Image details + delete
│       │   ├── ImageAdapter.java           # RecyclerView adapter for the grid
│       │   └── UriToPathHelper.java        # Converts document URIs → file paths
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_gallery.xml
│           │   ├── activity_image_detail.xml
│           │   └── item_image.xml          # Grid cell layout
│           ├── drawable/
│           │   ├── ic_camera.xml
│           │   ├── ic_folder.xml
│           │   └── ic_image_placeholder.xml
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           └── xml/
│               └── file_paths.xml          # FileProvider paths config
├── build.gradle                            # Project-level build config
├── settings.gradle
└── gradle.properties
```

---

## How to Open in Android Studio

1. **Clone / unzip** this project folder.
2. Open **Android Studio** → *File → Open* → select the `PhotoGalleryApp` folder.
3. Wait for Gradle sync to complete (it will download all dependencies automatically).
4. Connect a physical Android device **or** start an AVD emulator with a camera configured.
5. Click **Run ▶** (Shift+F10).

> **Tip:** For best results, test camera capture on a real device, not an emulator.

---

## Permissions Explained

| Permission | Android Version | Purpose |
|---|---|---|
| `CAMERA` | All | Open the system camera to capture photos |
| `WRITE_EXTERNAL_STORAGE` | ≤ API 29 (Android 9) | Save photos to external storage folders |
| `READ_EXTERNAL_STORAGE` | ≤ API 32 (Android 12) | Read images from device storage |
| `READ_MEDIA_IMAGES` | ≥ API 33 (Android 13) | Granular media permission replacing READ_EXTERNAL_STORAGE |

All permissions are requested at runtime using the modern `ActivityResultLauncher` API.

---

## Key Design Decisions

- **FileProvider** — Required on Android 7+ to share `file://` URIs with other apps (the camera). Configured in `AndroidManifest.xml` and `res/xml/file_paths.xml`.
- **No third-party image libraries** — Thumbnails are loaded using `BitmapFactory` with `inSampleSize` sub-sampling to avoid `OutOfMemoryError`.
- **`onResume()` refresh** — `GalleryActivity` reloads images every time it resumes, so the grid automatically updates after a deletion.
- **`ActivityResultLauncher`** — Used instead of the deprecated `startActivityForResult` / `onActivityResult` pattern.

---

## Minimum Requirements

- Android 5.0 (API 21) or higher
- Android Studio Hedgehog (2023.1.1) or newer
- Gradle 8.2 / Android Gradle Plugin 8.2.2
