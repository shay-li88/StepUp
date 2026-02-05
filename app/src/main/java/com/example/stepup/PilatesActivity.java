package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import androidx.appcompat.app.AppCompatActivity;

public class PilatesActivity extends AppCompatActivity {

    private Button btnBeginner, btnIntermediate, btnAdvanced, btnCore, btnFlexibility, btnFullBody, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedLevel = "Intermediate";
    private String selectedFocus = "Core";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pilates);

        // קישור רכיבים
        btnBeginner = findViewById(R.id.btnBeginner);
        btnIntermediate = findViewById(R.id.btnIntermediate);
        btnAdvanced = findViewById(R.id.btnAdvanced);
        btnCore = findViewById(R.id.btnCore);
        btnFlexibility = findViewById(R.id.btnFlexibility);
        btnFullBody = findViewById(R.id.btnFullBody);
        btnGo = findViewById(R.id.btnGoPilates);
        timePicker = findViewById(R.id.pilatesTimePicker);
        etNotes = findViewById(R.id.etPilatesNotes);

        timePicker.setMinValue(5);
        timePicker.setMaxValue(120);
        timePicker.setValue(40); // ברירת מחדל לפי העיצוב

        // לוגיקה לבחירת רמה
        setupSelection(new Button[]{btnBeginner, btnIntermediate, btnAdvanced}, btn -> selectedLevel = btn.getText().toString());

        // לוגיקה לבחירת מיקוד
        setupSelection(new Button[]{btnCore, btnFlexibility, btnFullBody}, btn -> selectedFocus = btn.getText().toString());

        // צביעה ראשונית של ברירות המחדל
        updateButtonUI(btnIntermediate, new Button[]{btnBeginner, btnIntermediate, btnAdvanced});
        updateButtonUI(btnCore, new Button[]{btnCore, btnFlexibility, btnFullBody});

        btnGo.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutsActivity.class);
            intent.putExtra("type", "Pilates - " + selectedFocus);
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
            b.setTextColor(Color.parseColor("#F57F17"));
        }
        selected.setBackgroundResource(R.drawable.pilates_selected);
        selected.setTextColor(Color.WHITE);
    }

    interface OnSelectionListener { void onSelected(Button b); }
}