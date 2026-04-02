# Media Player Android App

A fully-functional Android media player in Java supporting:
- 🎵 **Local audio file playback** (MP3, WAV, AAC, FLAC, OGG)
- 🎬 **Video streaming from URL** (MP4, HLS, DASH streams)

## Controls
| Button   | Description                          |
|----------|--------------------------------------|
| Open File | Pick an audio file from device storage |
| Open URL  | Enter a video/audio stream URL       |
| ▶ Play   | Start or resume playback             |
| ⏸ Pause  | Pause at current position            |
| ⏹ Stop   | Stop and reset to beginning          |
| ⏮ Restart| Jump to beginning and play           |

A **SeekBar** with time labels lets you scrub to any position.

## How to Open in Android Studio

1. Open **Android Studio**
2. Choose **Open an Existing Project**
3. Select the `MediaPlayerApp` folder
4. Wait for Gradle sync to finish
5. Connect a device or start an emulator
6. Click ▶ **Run**

## Permissions
- `INTERNET` – video streaming
- `READ_EXTERNAL_STORAGE` – audio files (Android ≤ 12)
- `READ_MEDIA_AUDIO` – audio files (Android 13+)

## Test URLs (video streaming)
```
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4
```

## Requirements
- Android Studio Hedgehog or newer
- Min SDK: 21 (Android 5.0)
- Target SDK: 34 (Android 14)
- Java 8
