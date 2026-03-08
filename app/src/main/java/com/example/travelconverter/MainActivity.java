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

    // categories shown in the top spinner
    private final String[] categories = {
            "Currency",
            "Fuel Efficiency",
            "Distance",
            "Liquid Volume",
            "Temperature"
    };

    // units per category / task conversion items
    private final String[] currencyUnits = {"USD", "AUD", "EUR", "JPY", "GBP"};
    private final String[] fuelUnits = {"mpg", "km/L"};
    private final String[] distanceUnits = {"Nautical Mile (NM)", "Kilometer (km)"};
    private final String[] volumeUnits = {"Gallon (US)", "Liter (L)"};
    private final String[] tempUnits = {"C", "F", "K"};

    // ui fields as class fields so listeners can reach them
    private Spinner spinnerCategory, spinnerFrom, spinnerTo;
    private EditText editValue;
    private TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // wire up all the views
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        editValue = findViewById(R.id.editValue);
        txtResult = findViewById(R.id.txtResult);
        Button btnConvert = findViewById(R.id.btnConvert);
        Button btnSwap = findViewById(R.id.btnSwap);

        // swap just flips the two spinner positions
        btnSwap.setOnClickListener(v -> {
            int fromPosition = spinnerFrom.getSelectedItemPosition();
            int toPosition = spinnerTo.getSelectedItemPosition();
            spinnerFrom.setSelection(toPosition);
            spinnerTo.setSelection(fromPosition);
            txtResult.setText("Result will appear here");
        });

        // populate category spinner
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );
        spinnerCategory.setAdapter(categoryAdapter);

        // when category changes, reload the from/to unit lists
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

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

                // post() waits until the adapter is done before resetting
                // otherwise setAdapter overwrites this!
                txtResult.post(() -> txtResult.setText("Result will appear here"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // convert button - validates input then runs the conversion
        btnConvert.setOnClickListener(v -> {

            String raw = editValue.getText().toString().trim();

            // catch empty input
            if (raw.isEmpty()) {
                Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show();
                return;
            }

            // catch anything that isn't  a number
            double value;
            try {
                value = Double.parseDouble(raw);
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = spinnerCategory.getSelectedItem().toString();
            String from = spinnerFrom.getSelectedItem().toString();
            String to = spinnerTo.getSelectedItem().toString();

            // negative values don't make sense unless its temperature
            if (value < 0 && !category.equals("Temperature")) {
                Toast.makeText(this, "Value cannot be negative for this category", Toast.LENGTH_SHORT).show();
                return;
            }

            // same unit selected - just show value back and toast
            if (from.equals(to)) {
                Toast.makeText(this, "Already in " + to + ", no conversion needed", Toast.LENGTH_SHORT).show();
                txtResult.setText(formatResult(value, to));
                return;
            }

            double result = convert(category, from, to, value);
            txtResult.setText(formatResult(result, to));
        });
    }

    // returns whichever unit list matches  selected category
    private String[] getUnitsForCategory(String category) {
        switch (category) {
            case "Currency": return currencyUnits;
            case "Fuel Efficiency": return fuelUnits;
            case "Distance": return distanceUnits;
            case "Liquid Volume": return volumeUnits;
            case "Temperature": return tempUnits;
            default: return tempUnits;
        }
    }

    // which converter to call from selected category
    private double convert(String category, String from, String to, double v) {
        switch (category) {
            case "Currency": return convertCurrency(from, to, v);
            case "Fuel Efficiency": return convertFuel(from, to, v);
            case "Distance": return convertDistance(from, to, v);
            case "Liquid Volume": return convertVolume(from, to, v);
            case "Temperature": return convertTemperature(from, to, v);
            default: return v;
        }
    }

    // currency - convert input to USD first then to the target currency
    private double convertCurrency(String from, String to, double v) {

        // first get everything into USD so i only need one set of rates
        double usd;
        switch (from) {
            case "USD": usd = v;          break;
            case "AUD": usd = v / 1.55;   break;
            case "EUR": usd = v / 0.92;   break;
            case "JPY": usd = v / 148.50; break;
            case "GBP": usd = v / 0.78;   break;
            default:    usd = v;
        }

        // then convert from USD out to whatever currency was selected
        switch (to) {
            case "USD": return usd;
            case "AUD": return usd * 1.55;
            case "EUR": return usd * 0.92;
            case "JPY": return usd * 148.50;
            case "GBP": return usd * 0.78;
            default:    return usd;
        }
    }

    // fuel efficiency - 1 mpg = 0.425 km/L
    private double convertFuel(String from, String to, double v) {
        if (from.equals("mpg")  && to.equals("km/L")) return v * 0.425;
        if (from.equals("km/L") && to.equals("mpg"))  return v / 0.425;
        return v;
    }

    // distance - 1 nautical mile = 1.852 km
    private double convertDistance(String from, String to, double v) {
        if (from.equals("Nautical Mile (NM)") && to.equals("Kilometer (km)")) return v * 1.852;
        if (from.equals("Kilometer (km)") && to.equals("Nautical Mile (NM)")) return v / 1.852;
        return v;
    }

    // volume - 1 US gallon = 3.785 liters
    private double convertVolume(String from, String to, double v) {
        if (from.equals("Gallon (US)") && to.equals("Liter (L)")) return v * 3.785;
        if (from.equals("Liter (L)")  && to.equals("Gallon (US)")) return v / 3.785;
        return v;
    }

    // temperature - convert to celsius first then out to target
    private double convertTemperature(String from, String to, double v) {

        // get to celsius first no matter what the input is
        double c;
        if      (from.equals("C")) c = v;
        else if (from.equals("F")) c = (v - 32) / 1.8;
        else if (from.equals("K")) c = v - 273.15;
        else                       c = v;

        // now go from celsius to whatever unit was picked
        if (to.equals("C")) return c;
        if (to.equals("F")) return (c * 1.8) + 32;
        if (to.equals("K")) return c + 273.15;

        return c;
    }

    // formats the number and sticks gets the right symbol on it
    private String formatResult(double value, String unit) {
        String symbol = getUnitSymbol(unit);
        String number;
        if (Math.abs(value) >= 1) number = String.format(Locale.US, "%.2f", value);
        else                      number = String.format(Locale.US, "%.4f", value);

        // currency symbols sit in front, units go after
        if (symbol.equals("$") || symbol.equals("A$") || symbol.equals("€") || symbol.equals("£") || symbol.equals("¥")) {
            return symbol + number;
        }
        return number + " " + symbol;
    }

    // looks up the symbol for whatever unit gets passed in
    private String getUnitSymbol(String unit) {
        switch (unit) {
            case "USD": return "$";
            case "AUD": return "A$";
            case "EUR": return "€";
            case "JPY": return "¥";
            case "GBP": return "£";
            case "C": return "°C";
            case "F": return "°F";
            case "K": return "K";
            case "mpg": return "mpg";
            case "km/L": return "km/L";
            case "Nautical Mile (NM)": return "NM";
            case "Kilometer (km)": return "km";
            case "Gallon (US)": return "gal";
            case "Liter (L)": return "L";
            default: return unit;
        }
    }
}