package com.example.stepup;
import android.content.Intent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class PilatesActivity extends AppCompatActivity {

    private Button btnBeginner, btnIntermediate, btnAdvanced, btnCore, btnFlexibility, btnFullBody, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedLevel = "Beginner";
    private String selectedFocus = "Core";

    private static final String TAG = "PilatesActivity";

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

        btnGo.setOnClickListener(v -> {
            // 1. איסוף הנתונים מהשדות
            String type = "Pilates " + selectedFocus;
            String diff = selectedLevel;
            int time = timePicker.getValue();
            String notes = etNotes.getText().toString();

            // 2. בדיקה שהמשתמש בחר הכל (כדי שלא יישמר אימון ריק)
            if (diff.isEmpty() || selectedFocus.isEmpty()) {
                android.widget.Toast.makeText(this, "Please select level and focus", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. יצירת אובייקט האימון החדש מהמחלקה שיצרנו קודם
            Workout newWorkout = new Workout(type, diff, time, notes);

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("Workouts").add(newWorkout)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                        Toast.makeText(PilatesActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();
                        // רק אם השמירה הצליחה - עוברים למסך הסיכום
                        Intent intent = new Intent(this, WorkoutsActivity.class);
                        intent.putExtra("type", type);
                        intent.putExtra("difficulty", diff);
                        intent.putExtra("time", time);
                        intent.putExtra("notes", notes);
                        startActivity(intent);
                        finish(); // Close this activity and return to FeedActivity
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error adding document", e);
                        Toast.makeText(PilatesActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            Log.d(TAG, "save workout: done");

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
            // ביטול ה-Tint של אנדרואיד כדי שנוכל לראות את ה-Drawable שלנו
            b.setBackgroundTintList(null);

            // מצב לא נבחר: רקע שקוף וטקסט כחול כהה
            b.setBackgroundResource(android.R.color.transparent);
            b.setTextColor(Color.parseColor("#1A4375"));
        }
        // מצב נבחר: הרקע הכחול שבנינו ב-pilates_selected
        selected.setBackgroundResource(R.drawable.pilates_selected);
        selected.setTextColor(Color.WHITE);
    }

    interface OnSelectionListener { void onSelected(Button b); }
}