package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class RunningActivity extends AppCompatActivity {
    // הוסיפי את btnGo כאן למעלה
    private Button btnEasy, btnMedium, btnIntense, btnGo;
    private NumberPicker timePicker, distancePicker;

    private EditText etNotes;
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

        etNotes = findViewById(R.id.etRunningNotes);


        // הגדרת הגלילים
        timePicker.setMinValue(1); timePicker.setMaxValue(120); timePicker.setValue(30);
        distancePicker.setMinValue(1); distancePicker.setMaxValue(50); distancePicker.setValue(4);

        setupDifficulty(btnEasy);
        setupDifficulty(btnMedium);
        setupDifficulty(btnIntense);

        btnGo.setOnClickListener(v -> {
            // 1. איסוף הנתונים (וודאי שהמשתנים האלו מוגדרים אצלך)
            String type = "Running";
            String diff = selectedDifficulty;
            int time = timePicker.getValue();
            String notes = etNotes.getText().toString();

            // בדיקה בסיסית שנבחרה רמה
            if (diff == null || diff.isEmpty()) {
                Toast.makeText(RunningActivity.this, "Please select difficulty level", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. יצירת אובייקט האימון
            Workout newWorkout = new Workout(type, diff, time, notes);

            // 3. שימוש ב-Firestore לפי הקוד הנכון שלך
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("Workouts").add(newWorkout)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("RunningActivity", "DocumentSnapshot written with ID: " + documentReference.getId());
                        Toast.makeText(RunningActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();

                        // רק אם השמירה הצליחה - עוברים למסך הסיכום
                        Intent intent = new Intent(RunningActivity.this, WorkoutsActivity.class);
                        intent.putExtra("type", type);
                        intent.putExtra("difficulty", diff);
                        intent.putExtra("time", time);
                        intent.putExtra("notes", notes);
                        startActivity(intent);
                        finish(); // סגירת האקטיביטי וחזרה לפיד
                    })
                    .addOnFailureListener(e -> {
                        Log.w("RunningActivity", "Error adding document", e);
                        Toast.makeText(RunningActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

            Log.d("RunningActivity", "save workout: done");
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
