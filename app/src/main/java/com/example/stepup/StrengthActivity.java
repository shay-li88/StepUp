package com.example.stepup;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import androidx.appcompat.app.AppCompatActivity;

public class StrengthActivity extends AppCompatActivity {

    private Button btnLight, btnModerate, btnHeavy, btnUpper, btnLower, btnFull, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedLevel = "Light";
    private String selectedType = "Full Body";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strength);

        // אתחול רכיבים
        btnLight = findViewById(R.id.btnLight);
        btnModerate = findViewById(R.id.btnModerate);
        btnHeavy = findViewById(R.id.btnHeavy);
        btnUpper = findViewById(R.id.btnUpper);
        btnLower = findViewById(R.id.btnLower);
        btnFull = findViewById(R.id.btnFull);
        btnGo = findViewById(R.id.btnGoStrength);
        timePicker = findViewById(R.id.strengthTimePicker);
        etNotes = findViewById(R.id.etStrengthNotes);

        timePicker.setMinValue(5);
        timePicker.setMaxValue(120);
        timePicker.setValue(45);

        // הגדרת כפתורי רמת קושי
        setupSelection(new Button[]{btnLight, btnModerate, btnHeavy}, btn -> selectedLevel = btn.getText().toString());

        // הגדרת כפתורי סוג אימון
        setupSelection(new Button[]{btnUpper, btnLower, btnFull}, btn -> selectedType = btn.getText().toString());


        btnGo.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutsActivity.class);
            intent.putExtra("type", "Strength - " + selectedType);
            intent.putExtra("difficulty", selectedLevel);
            intent.putExtra("time", timePicker.getValue());
            intent.putExtra("notes", etNotes.getText().toString());
            startActivity(intent);
        });
    }

    private void setupSelection(Button[] group, OnSelectionListener listener) {
        for (Button b : group) {
            b.setOnClickListener(v -> {
                updateButtonUI(b, group);
                listener.onSelected(b);
            });
        }
    }

    private void updateButtonUI(Button selected, Button[] group) {
        for (Button b : group) {
            b.setBackgroundResource(android.R.color.transparent);
            b.setTextColor(Color.parseColor("#4A148C"));
        }
        selected.setBackgroundResource(R.drawable.strength_selected); // צרי drawable סגול
        selected.setTextColor(Color.WHITE);
    }

    interface OnSelectionListener { void onSelected(Button b); }
}