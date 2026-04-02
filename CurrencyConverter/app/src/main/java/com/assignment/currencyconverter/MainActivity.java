package com.assignment.currencyconverter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Activity for the Currency Converter app.
 * Handles the logic for currency conversion and UI updates.
 */
public class MainActivity extends AppCompatActivity {

    // Static exchange rates relative to 1 USD (Base Currency)
    private static final double RATE_INR = 92.5;
    private static final double RATE_USD = 1.00;
    private static final double RATE_JPY = 160.0;
    private static final double RATE_EUR = 0.87;

    private CurrencyAdapter adapter;
    private List<CurrencyItem> currencyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme (Light/Dark) before setting the content view
        applyThemeFromPrefs();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Toolbar as the ActionBar
        setSupportActionBar(findViewById(R.id.toolbar));

        // Create the list of currencies to be displayed
        currencyList = buildCurrencyList();

        // Setup RecyclerView with a LinearLayoutManager and the CurrencyAdapter
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter with the currency list and a listener for input changes
        adapter = new CurrencyAdapter(currencyList, this::onCurrencyInputChanged);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Logic to handle conversion when an amount is changed in any currency field.
     * @param sourceIndex The index of the currency row that was edited.
     * @param rawText The new text entered by the user.
     */
    private void onCurrencyInputChanged(int sourceIndex, String rawText) {
        double inputValue = 0;
        try {
            if (rawText != null && !rawText.isEmpty()) {
                inputValue = Double.parseDouble(rawText);
            }
        } catch (NumberFormatException e) {
            inputValue = 0;
        }

        // 1. Convert the input value to USD (acting as a base reference)
        double valueInUSD = toUSD(sourceIndex, inputValue);

        // 2. Convert from USD to all other currencies and update the list
        for (int i = 0; i < currencyList.size(); i++) {
            if (i != sourceIndex) {
                double converted = fromUSD(i, valueInUSD);
                currencyList.get(i).setAmount(converted);
            }
        }

        // 3. Update the UI for all rows except the one currently being edited
        adapter.updateAmountsExcept(sourceIndex);
    }

    // Helper: Converts a specific currency amount to USD
    private double toUSD(int index, double amount) {
        switch (index) {
            case 0: return amount / RATE_INR; // INR -> USD
            case 1: return amount;             // USD -> USD
            case 2: return amount / RATE_JPY; // JPY -> USD
            case 3: return amount / RATE_EUR; // EUR -> USD
            default: return amount;
        }
    }

    // Helper: Converts USD to a specific currency amount
    private double fromUSD(int index, double usd) {
        switch (index) {
            case 0: return usd * RATE_INR; // USD -> INR
            case 1: return usd;             // USD -> USD
            case 2: return usd * RATE_JPY; // USD -> JPY
            case 3: return usd * RATE_EUR; // USD -> EUR
            default: return usd;
        }
    }

    // Creates the initial list of 4 currencies
    private List<CurrencyItem> buildCurrencyList() {
        List<CurrencyItem> list = new ArrayList<>();
        list.add(new CurrencyItem("INR", "Indian Rupee",  "₹", R.drawable.flag_in));
        list.add(new CurrencyItem("USD", "US Dollar",     "$", R.drawable.flag_us));
        list.add(new CurrencyItem("JPY", "Japanese Yen",  "¥", R.drawable.flag_jp));
        list.add(new CurrencyItem("EUR", "Euro",          "€", R.drawable.flag_eu));
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item clicks (e.g., Settings)
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Reads the saved theme preference and applies it to the app
    private void applyThemeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", true);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh theme on resume to catch any changes made in SettingsActivity
        applyThemeFromPrefs();
    }
}
