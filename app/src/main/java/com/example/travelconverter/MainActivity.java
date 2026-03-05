package com.example.travelconverter;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // categories
    private final String[] categories = {
            "Currency",
            "Fuel Efficiency",
            "Distance",
            "Liquid Volume",
            "Temperature"
    };

    // units per category (explicit fixed factors for)
    private final String[] currencyUnits = {"USD", "AUD", "EUR", "JPY", "GBP"};
    private final String[] fuelUnits = {"mpg", "km/L"};
    private final String[] distanceUnits = {"Nautical Mile (NM)", "Kilometer (km)"};
    private final String[] volumeUnits = {"Gallon (US)", "Liter (L)"};
    private final String[] tempUnits = {"C", "F", "K"};

    // ui references (kept as fields so listeners can use them)
    private Spinner spinnerCategory, spinnerFrom, spinnerTo;
    private EditText editValue;
    private TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // connect java to xml views
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        editValue = findViewById(R.id.editValue);
        txtResult = findViewById(R.id.txtResult);
        Button btnConvert = findViewById(R.id.btnConvert);
        Button btnSwap = findViewById(R.id.btnSwap);

        // swap button reverses selected units in the two spinners
        btnSwap.setOnClickListener(v -> {

            int fromPosition = spinnerFrom.getSelectedItemPosition();
            int toPosition = spinnerTo.getSelectedItemPosition();

            spinnerFrom.setSelection(toPosition);
            spinnerTo.setSelection(fromPosition);

        });

        // populate category spinner
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );
        spinnerCategory.setAdapter(categoryAdapter);

        // when category changes update from/to spinners
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            // switches available units based on selected category
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                String[] units = getUnitsForCategory(selectedCategory);

                ArrayAdapter<String> unitsAdapter = new ArrayAdapter<>(
                        MainActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        units
                );

                spinnerFrom.setAdapter(unitsAdapter);
                spinnerTo.setAdapter(unitsAdapter);

                txtResult.setText("Result will appear here");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // convert button click
        btnConvert.setOnClickListener(v -> {

            // validates input runs conversion and displays formatted result

            String raw = editValue.getText().toString().trim();

            // protection for empty input
            if (raw.isEmpty()) {
                Toast.makeText(this, "Enter a value", Toast.LENGTH_SHORT).show();
                return;
            }

            // protection for numeric parsing
            final double value;
            try {
                value = Double.parseDouble(raw);
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = spinnerCategory.getSelectedItem().toString();
            String from = spinnerFrom.getSelectedItem().toString();
            String to = spinnerTo.getSelectedItem().toString();

            // protection block negative values except for temperature
            if (value < 0 && !category.equals("Temperature")) {
                Toast.makeText(this, "Value cannot be negative for this category", Toast.LENGTH_SHORT).show();
                return;
            }

            // identity conversion (same units)
            if (from.equals(to)) {
                txtResult.setText(formatResult(value));
                return;
            }

            double result = convert(category, from, to, value);
            txtResult.setText(formatResult(result));
        });
    }

    // returns unit list for the chosen category
    private String[] getUnitsForCategory(String category) {
        switch (category) {
            case "Currency":
                return currencyUnits;
            case "Fuel Efficiency":
                return fuelUnits;
            case "Distance":
                return distanceUnits;
            case "Liquid Volume":
                return volumeUnits;
            case "Temperature":
            default:
                return tempUnits;
        }
    }

    // routes conversion to the correct conversion block
    private double convert(String category, String from, String to, double v) {
        switch (category) {
            case "Currency":
                return convertCurrency(from, to, v);
            case "Fuel Efficiency":
                return convertFuel(from, to, v);
            case "Distance":
                return convertDistance(from, to, v);
            case "Liquid Volume":
                return convertVolume(from, to, v);
            case "Temperature":
                return convertTemperature(from, to, v);
            default:
                return v;
        }
    }

    // currency conversion via usd using fixed task rates
    private double convertCurrency(String from, String to, double v) {

        double usd;
        switch (from) {
            case "USD": usd = v; break;
            case "AUD": usd = v / 1.55; break;
            case "EUR": usd = v / 0.92; break;
            case "JPY": usd = v / 148.50; break;
            case "GBP": usd = v / 0.78; break;
            default: usd = v;
        }

        switch (to) {
            case "USD": return usd;
            case "AUD": return usd * 1.55;
            case "EUR": return usd * 0.92;
            case "JPY": return usd * 148.50;
            case "GBP": return usd * 0.78;
            default: return usd;
        }
    }

    // fuel efficiency conversion using fixed factor
    private double convertFuel(String from, String to, double v) {

        // 1 mpg = 0.425 km/L
        if (from.equals("mpg") && to.equals("km/L")) return v * 0.425;
        if (from.equals("km/L") && to.equals("mpg")) return v / 0.425;
        return v;
    }

    // distance conversion using fixed factor
    private double convertDistance(String from, String to, double v) {

        // 1 nautical mile = 1.852 kilometers
        if (from.equals("Nautical Mile (NM)") && to.equals("Kilometer (km)")) return v * 1.852;
        if (from.equals("Kilometer (km)") && to.equals("Nautical Mile (NM)")) return v / 1.852;
        return v;
    }

    // volume conversion using fixed factor
    private double convertVolume(String from, String to, double v) {

        // 1 gallon (us) = 3.785 liters
        if (from.equals("Gallon (US)") && to.equals("Liter (L)")) return v * 3.785;
        if (from.equals("Liter (L)") && to.equals("Gallon (US)")) return v / 3.785;
        return v;
    }

    // temperature conversions using task formulas
    private double convertTemperature(String from, String to, double v) {

        double c;

        if (from.equals("C")) c = v;
        else if (from.equals("F")) c = (v - 32) / 1.8;
        else if (from.equals("K")) c = v - 273.15;
        else c = v;

        if (to.equals("C")) return c;
        if (to.equals("F")) return (c * 1.8) + 32;
        if (to.equals("K")) return c + 273.15;

        return c;
    }

    // format output for a cleaner result
    private String formatResult(double value) {

        if (Math.abs(value) >= 100) return String.format(Locale.US, "%.2f", value);
        if (Math.abs(value) >= 1) return String.format(Locale.US, "%.2f", value);
        return String.format(Locale.US, "%.4f", value);
    }
}