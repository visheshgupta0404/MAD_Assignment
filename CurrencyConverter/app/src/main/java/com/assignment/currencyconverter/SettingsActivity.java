package com.assignment.currencyconverter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Activity for managing app settings, specifically the Dark Mode toggle.
 */
public class SettingsActivity extends AppCompatActivity {

    private Switch themeSwitch;
    private TextView themeLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Configure the Toolbar with a back arrow and title
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        themeSwitch = findViewById(R.id.themeSwitch);
        themeLabel  = findViewById(R.id.themeLabel);

        // Retrieve current theme preference (Default is Dark Mode)
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", true);

        // Initial UI state setup
        themeSwitch.setChecked(isDark);
        updateLabel(isDark);

        // Handle Dark Mode toggle changes
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Persist the user's choice
            prefs.edit().putBoolean("dark_mode", isChecked).apply();

            // Apply the theme change immediately to the entire app
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            updateLabel(isChecked);
        });
    }

    // Updates the descriptive text next to the switch
    private void updateLabel(boolean isDark) {
        themeLabel.setText(isDark ? "Dark Mode" : "Light Mode");
    }

    // Handles the Up button (back arrow) click
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
