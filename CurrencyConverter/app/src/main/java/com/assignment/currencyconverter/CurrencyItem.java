package com.assignment.currencyconverter;

/**
 * Data model for a single currency entry.
 * It stores the currency details and the current converted amount.
 */
public class CurrencyItem {

    private final String code;       // e.g., "INR"
    private final String name;       // e.g., "Indian Rupee"
    private final String symbol;     // e.g., "₹"
    private final int flagResId;     // Drawable resource ID for the country flag
    private double amount;           // The current amount for this currency

    public CurrencyItem(String code, String name, String symbol, int flagResId) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.flagResId = flagResId;
        this.amount = 0.0;
    }

    // --- Getters ---
    public String getCode()      { return code; }
    public String getName()      { return name; }
    public String getSymbol()    { return symbol; }
    public int    getFlagResId() { return flagResId; }
    public double getAmount()    { return amount; }

    // --- Setter ---
    public void setAmount(double amount) { this.amount = amount; }
}
