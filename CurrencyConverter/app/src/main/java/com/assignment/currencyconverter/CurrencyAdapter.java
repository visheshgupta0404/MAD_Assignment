package com.assignment.currencyconverter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Adapter class to manage the list of currency rows in the RecyclerView.
 */
public class CurrencyAdapter extends RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder> {

    // Callback interface: informs the listener which row changed and the new text
    public interface OnAmountChangedListener {
        void onAmountChanged(int position, String text);
    }

    private final List<CurrencyItem> items;
    private final OnAmountChangedListener listener;

    // Track which row is actively being edited to prevent recursive update loops
    private int activePosition = -1;

    public CurrencyAdapter(List<CurrencyItem> items, OnAmountChangedListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CurrencyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single currency row
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_currency, parent, false);
        return new CurrencyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CurrencyViewHolder holder, int position) {
        CurrencyItem item = items.get(position);

        // Bind basic data: flag icon, currency code, and full name
        holder.flagImage.setImageResource(item.getFlagResId());
        holder.codeText.setText(item.getCode());
        holder.nameText.setText(item.getName());

        // Remove previous watcher to avoid triggering it when we manually set text below
        if (holder.textWatcher != null) {
            holder.amountEdit.removeTextChangedListener(holder.textWatcher);
        }

        // Update the amount field only if this row is not the one being edited
        if (position != activePosition) {
            if (item.getAmount() == 0.0) {
                holder.amountEdit.setText("");
                holder.amountEdit.setHint("0");
            } else {
                // Format to 2 decimal places using US Locale for consistency (.)
                String formatted = String.format(Locale.US, "%.2f", item.getAmount());
                holder.amountEdit.setText(formatted);
            }
        }

        // Add a new TextWatcher to listen for user input in this specific field
        holder.textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_ID) return;
                
                // Mark this position as active to skip it during the broad UI update
                activePosition = adapterPos;
                listener.onAmountChanged(adapterPos, s.toString());
            }
        };
        holder.amountEdit.addTextChangedListener(holder.textWatcher);
    }

    @Override
    public int getItemCount() { return items.size(); }

    /**
     * Refreshes all currency rows except the one currently being typed in.
     * This avoids jumping or double-editing issues.
     */
    public void updateAmountsExcept(int skipPosition) {
        activePosition = skipPosition;
        for (int i = 0; i < items.size(); i++) {
            if (i != skipPosition) {
                notifyItemChanged(i);
            }
        }
    }

    // ViewHolder class to hold references to each row's UI components
    static class CurrencyViewHolder extends RecyclerView.ViewHolder {
        ImageView flagImage;
        TextView  codeText;
        TextView  nameText;
        EditText  amountEdit;
        TextWatcher textWatcher;

        CurrencyViewHolder(@NonNull View itemView) {
            super(itemView);
            flagImage  = itemView.findViewById(R.id.flagImage);
            codeText   = itemView.findViewById(R.id.codeText);
            nameText   = itemView.findViewById(R.id.nameText);
            amountEdit = itemView.findViewById(R.id.amountEdit);
        }
    }
}
