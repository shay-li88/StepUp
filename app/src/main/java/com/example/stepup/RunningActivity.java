package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.NumberPicker;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class RunningActivity extends AppCompatActivity {
    // הוסיפי את btnGo כאן למעלה
    private Button btnEasy, btnMedium, btnIntense, btnGo;
    private NumberPicker timePicker, distancePicker;
    private String selectedDifficulty = "Easy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        // חיבור ה-ID מה-XML
        btnEasy = findViewById(R.id.btnEasy);
        btnMedium = findViewById(R.id.btnMedium);
        btnIntense = findViewById(R.id.btnIntense);
        btnGo = findViewById(R.id.btnGoActivity); // זה היה חסר!
        timePicker = findViewById(R.id.timePicker);
        distancePicker = findViewById(R.id.distancePicker);

        // הגדרת הגלילים
        timePicker.setMinValue(1); timePicker.setMaxValue(120); timePicker.setValue(30);
        distancePicker.setMinValue(1); distancePicker.setMaxValue(50); distancePicker.setValue(4);

        setupDifficulty(btnEasy);
        setupDifficulty(btnMedium);
        setupDifficulty(btnIntense);

        btnGo.setOnClickListener(v -> {
            int time = timePicker.getValue();
            int distance = distancePicker.getValue();

            Intent intent = new Intent(RunningActivity.this, WorkoutsActivity.class);
            intent.putExtra("type", "Running");
            intent.putExtra("difficulty", selectedDifficulty);
            intent.putExtra("time", time);
            intent.putExtra("distance", distance);

            startActivity(intent);
        });
    }

    private void setupDifficulty(Button clickedBtn) {
        clickedBtn.setOnClickListener(v -> {
            resetButtons(); // קודם כל מאפסים את כולם

            // צובעים את הנבחר בטורקיז (הקובץ שיצרת ב-drawable)
            clickedBtn.setBackgroundResource(R.drawable.selected_difficulty);
            clickedBtn.setTextColor(Color.WHITE); // טקסט לבן בבחירה

            selectedDifficulty = clickedBtn.getText().toString();
        });
    }

    private void resetButtons() {
        Button[] btns = {btnEasy, btnMedium, btnIntense};
        for (Button b : btns) {
            // מחזירים לרקע שקוף וטקסט ירוק כהה
            b.setBackgroundResource(android.R.color.transparent);
            b.setTextColor(Color.parseColor("#2D6A4F"));
        }
    }
}
