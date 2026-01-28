package com.example.stepup;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.NumberPicker;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class RunningActivity extends AppCompatActivity {
    private Button btnEasy, btnMedium, btnIntense;
    private NumberPicker timePicker, distancePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        btnEasy = findViewById(R.id.btnEasy);
        btnMedium = findViewById(R.id.btnMedium);
        btnIntense = findViewById(R.id.btnIntense);
        timePicker = findViewById(R.id.timePicker);
        distancePicker = findViewById(R.id.distancePicker);

        // הגדרת הגלילים
        timePicker.setMinValue(1); timePicker.setMaxValue(120); timePicker.setValue(30);
        distancePicker.setMinValue(1); distancePicker.setMaxValue(50); distancePicker.setValue(4);

        setupDifficulty(btnEasy);
        setupDifficulty(btnMedium);
        setupDifficulty(btnIntense);
    }

    private void setupDifficulty(Button clickedBtn) {
        clickedBtn.setOnClickListener(v -> {
            // איפוס כולם
            resetButtons();
            // שינוי הנבחר לטורקיז וטקסט לבן
            clickedBtn.setBackgroundResource(R.drawable.selected_difficulty);
            clickedBtn.setTextColor(Color.WHITE);
        });
    }

    private void resetButtons() {
        Button[] btns = {btnEasy, btnMedium, btnIntense};
        for (Button b : btns) {
            b.setBackgroundColor(Color.TRANSPARENT);
            b.setTextColor(Color.parseColor("#2D6A4F"));
        }
    }
}
