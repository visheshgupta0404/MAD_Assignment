package com.example.mediaplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

/**
 * MainActivity: The core controller for the Media Player application.
 * Handles both local audio files and remote video streams.
 */
public class MainActivity extends AppCompatActivity {

    // Request codes for permission and file picker intents
    private static final int REQUEST_PERMISSION = 100;
    private static final int REQUEST_FILE_PICK = 200;

    // UI Components
    private Button btnOpenFile, btnOpenUrl, btnPlay, btnPause, btnStop, btnRestart;
    private SeekBar seekBar;
    private TextView tvStatus, tvCurrentTime, tvTotalTime;
    private VideoView videoView;
    private View audioPanel, videoPanel;

    // Media handling variables
    private MediaPlayer mediaPlayer;
    private boolean isAudioMode = false;
    private boolean isVideoMode = false;
    private String currentUrl = "";
    private Uri currentFileUri = null;

    // Handler for updating the SeekBar progress
    private final Handler handler = new Handler();
    private Runnable seekBarUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();            // Initialize UI components
        setupListeners();       // Set up click and seek listeners
        setupSeekBarUpdater();  // Prepare the background task for progress updates
        updateButtonStates(false); // Initially disable playback buttons
    }

    /**
     * Finds and assigns all UI elements from the layout.
     */
    private void initViews() {
        btnOpenFile = findViewById(R.id.btnOpenFile);
        btnOpenUrl  = findViewById(R.id.btnOpenUrl);
        btnPlay     = findViewById(R.id.btnPlay);
        btnPause    = findViewById(R.id.btnPause);
        btnStop     = findViewById(R.id.btnStop);
        btnRestart  = findViewById(R.id.btnRestart);
        seekBar     = findViewById(R.id.seekBar);
        tvStatus    = findViewById(R.id.tvStatus);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime   = findViewById(R.id.tvTotalTime);
        videoView   = findViewById(R.id.videoView);
        audioPanel  = findViewById(R.id.audioPanel);
        videoPanel  = findViewById(R.id.videoPanel);

        // Standard Android MediaController for basic video overlay controls
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
    }

    /**
     * Defines behavior for button clicks and SeekBar interaction.
     */
    private void setupListeners() {
        btnOpenFile.setOnClickListener(v -> openFilePicker());
        btnOpenUrl.setOnClickListener(v -> showUrlDialog());

        btnPlay.setOnClickListener(v -> playMedia());
        btnPause.setOnClickListener(v -> pauseMedia());
        btnStop.setOnClickListener(v -> stopMedia());
        btnRestart.setOnClickListener(v -> restartMedia());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override 
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Seek to selected position if the user moves the slider
                if (fromUser) {
                    if (isAudioMode && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                    } else if (isVideoMode) {
                        videoView.seekTo(progress);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * Periodic task to sync the SeekBar position with the current playback time.
     */
    private void setupSeekBarUpdater() {
        seekBarUpdater = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isAudioMode && mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int current = mediaPlayer.getCurrentPosition();
                        int duration = mediaPlayer.getDuration();
                        if (duration > 0) {
                            seekBar.setProgress(current);
                            updateTimeLabels(current, duration);
                        }
                    } else if (isVideoMode && videoView.isPlaying()) {
                        int current = videoView.getCurrentPosition();
                        int duration = videoView.getDuration();
                        if (duration > 0) {
                            seekBar.setProgress(current);
                            updateTimeLabels(current, duration);
                        }
                    }
                } catch (Exception ignored) {}
                // Schedule next update in 500ms
                handler.postDelayed(this, 500);
            }
        };
    }

    /**
     * Clears existing media sessions before starting a new one.
     */
    private void resetMedia() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        videoView.stopPlayback();
        videoView.setVideoURI(null);
        handler.removeCallbacks(seekBarUpdater);
        updateButtonStates(false);
        seekBar.setProgress(0);
        updateTimeLabels(0, 0);
    }

    /**
     * Checks permissions and launches the system file picker for audio.
     */
    private void openFilePicker() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                ? Manifest.permission.READ_MEDIA_AUDIO 
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_PERMISSION);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Audio File"), REQUEST_FILE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_PICK && resultCode == RESULT_OK && data != null) {
            resetMedia();
            currentFileUri = data.getData();
            isAudioMode = true;
            isVideoMode = false;
            audioPanel.setVisibility(View.VISIBLE);
            videoPanel.setVisibility(View.GONE);
            prepareAudio(currentFileUri);
        }
    }

    /**
     * Displays an input dialog to enter a direct URL for video streaming.
     */
    private void showUrlDialog() {
        final EditText input = new EditText(this);
        input.setHint("http://example.com/video.mp4");
        if (!currentUrl.isEmpty()) input.setText(currentUrl);

        new AlertDialog.Builder(this)
                .setTitle("Media Link")
                .setMessage("Enter direct link:")
                .setView(input)
                .setPositiveButton("Load", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        resetMedia();
                        currentUrl = url;
                        isVideoMode = true;
                        isAudioMode = false;
                        audioPanel.setVisibility(View.GONE);
                        videoPanel.setVisibility(View.VISIBLE);
                        prepareVideo(url);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Prepares the MediaPlayer for local or remote audio.
     */
    private void prepareAudio(Uri uri) {
        setStatus("Loading...");
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.setOnPreparedListener(mp -> {
                setStatus("Ready");
                seekBar.setMax(mp.getDuration());
                updateTimeLabels(0, mp.getDuration());
                updateButtonStates(true);
                handler.post(seekBarUpdater);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                setStatus("Error loading audio");
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            setStatus("Failed");
        }
    }

    /**
     * Prepares the VideoView for network streaming.
     */
    private void prepareVideo(String url) {
        setStatus("Connecting...");
        videoView.setVideoPath(url);
        videoView.setOnPreparedListener(mp -> {
            int duration = videoView.getDuration();
            if (duration > 0) {
                seekBar.setMax(duration);
                updateTimeLabels(0, duration);
            }
            setStatus("Online");
            updateButtonStates(true);
            videoView.start();
            handler.post(seekBarUpdater);
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            setStatus("Streaming Error");
            new AlertDialog.Builder(this)
                   .setTitle("Diagnosis")
                   .setMessage("Could not play video. Check connection or URL validity.")
                   .setPositiveButton("OK", null)
                   .show();
            return true;
        });
    }

    private void playMedia() {
        if (isAudioMode && mediaPlayer != null) {
            mediaPlayer.start();
            setStatus("Playing");
        } else if (isVideoMode) {
            videoView.start();
            setStatus("Playing");
        }
    }

    private void pauseMedia() {
        if (isAudioMode && mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            setStatus("Paused");
        } else if (isVideoMode && videoView.isPlaying()) {
            videoView.pause();
            setStatus("Paused");
        }
    }

    private void stopMedia() {
        if (isAudioMode && mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            setStatus("Stopped");
        } else if (isVideoMode) {
            videoView.pause();
            videoView.seekTo(0);
            setStatus("Stopped");
        }
    }

    private void restartMedia() {
        if (isAudioMode && mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
            setStatus("Restarted");
        } else if (isVideoMode) {
            videoView.seekTo(0);
            videoView.start();
            setStatus("Restarted");
        }
    }

    private void setStatus(String msg) {
        tvStatus.setText(msg);
    }

    private void updateButtonStates(boolean mediaLoaded) {
        btnPlay.setEnabled(mediaLoaded);
        btnPause.setEnabled(mediaLoaded);
        btnStop.setEnabled(mediaLoaded);
        btnRestart.setEnabled(mediaLoaded);
        seekBar.setEnabled(mediaLoaded);
    }

    private void updateTimeLabels(int currentMs, int totalMs) {
        tvCurrentTime.setText(formatTime(currentMs));
        tvTotalTime.setText(formatTime(totalMs));
    }

    /**
     * Formats milliseconds into a mm:ss string.
     */
    private String formatTime(int ms) {
        if (ms <= 0) return "00:00";
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetMedia(); // Cleanup resources on exit
    }
}
