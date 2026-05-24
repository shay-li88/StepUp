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

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

/**
 * מסך רישום אימון פילאטיס (PilatesActivity):
 * מאפשר למשתמש לבחור את רמת הקושי (Beginner/Intermediate/Advanced) ואת מיקוד האימון (Core/Flexibility/Full Body).
 * הנתונים נשמרים ב-Firestore, ומעדכנים את מדדי הכוכבים והרצף (Streak) של המשתמש בהתאם ללוגיקה המשותפת.
 */
public class PilatesActivity extends AppCompatActivity {

    private Button btnBeginner, btnIntermediate, btnAdvanced, btnCore, btnFlexibility, btnFullBody, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = "Beginner"; // ברירת מחדל לרמת הקושי
    private String selectedFocus = "Core";          // ברירת מחדל למיקוד האימון
    private FirebaseFirestore db;

    private static final String TAG = "PilatesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pilates);

        db = FirebaseFirestore.getInstance();

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

        // הגדרת טווח ערכים לבורר הזמן (בין 5 דקות לשעתיים, ברירת מחדל 40 דקות)
        timePicker.setMinValue(5);
        timePicker.setMaxValue(120);
        timePicker.setValue(40);

        // לוגיקה לבחירת רמה
        setupSelection(new Button[]{btnBeginner, btnIntermediate, btnAdvanced}, btn -> selectedDifficulty = btn.getText().toString());

        // לוגיקה לבחירת מיקוד
        setupSelection(new Button[]{btnCore, btnFlexibility, btnFullBody}, btn -> selectedFocus = btn.getText().toString());

        btnGo.setOnClickListener(v -> savePilatesWorkout());
    }

    /**
     * פונקציה האוספת את הנתונים מהמסך, בונה אובייקט Workout מסוג פילאטיס, ושומרת אותו ב-Firestore
     */
    private void savePilatesWorkout() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        // בניית שם דינמי לאימון המשלב את מיקוד האימון (לדוגמה: "Pilates Core")
        Workout newWorkout = new Workout("Pilates " + selectedFocus, selectedDifficulty, timePicker.getValue(), etNotes.getText().toString(), 0.0);
        newWorkout.setUserId(currentUserId);
        newWorkout.setTimestamp(Timestamp.now());
        //שאילתה לשמירת אימון חדש
        db.collection("Workouts").add(newWorkout)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Workout saved with ID: " + documentReference.getId());

                    // --- עדכון סטטיסטיקות משתמש (כוכבים וסטריק חכם) ---
                    updateUserStats(currentUserId);

                    Toast.makeText(PilatesActivity.this, "Workout saved! +3 Stars", Toast.LENGTH_SHORT).show();

                    // מעבר למסך רשימת האימונים וסגירת המסך הנוכחי
                    Intent intent = new Intent(this, MyWorkoutsActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(PilatesActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // הפונקציה המעודכנת לעדכון כוכבים וסטריק בצורה חכמה
    // פונקציה המבטיחה סנכרון ואינטגרציה מלאה מול ה-Streak והכוכבים בפרופיל
    private void updateUserStats(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        //שאילתה לעדכון הכוכבים והסטרייק היומי
        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // 1. תמיד מוסיפים 3 כוכבים
                userRef.update("totalStars", FieldValue.increment(3));

                // 2. בדיקה אם הסטריק כבר עודכן היום (מניעת Spam של אימונים באותו יום)
                Timestamp lastUpdateTS = doc.getTimestamp("lastStreakUpdate");
                Date today = new Date();

                if (lastUpdateTS == null || !isSameDay(lastUpdateTS.toDate(), today)) {
                    userRef.update(
                            "streak", FieldValue.increment(1),
                            "lastStreakUpdate", new Timestamp(today)
                    );
                    Log.d("Points", "Pilates: Streak incremented!");
                } else {
                    Log.d("Points", "Pilates: Streak already updated today.");
                }
            }
        }).addOnFailureListener(e -> Log.e("Points", "Error fetching user", e));
    }

    // בדיקה אם שני תאריכים הם באותו יום קלנדרי (השוואת שנה ויום בשנה)
    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * פונקציה גנרית המאזינה לקבוצת כפתורים ומנהלת בחירה בלעדית (Radio Group מותאם אישית) באמצעות הממשק המקומי
     */
    private void setupSelection(Button[] group, OnSelectionListener listener) {
        for (Button b : group) {
            if (b == null) continue;
            b.setOnClickListener(v -> {
                updateButtonUI(b, group); // שינוי עיצוב הכפתורים
                listener.onSelected(b);   // קולבק לעדכון המשתנה המתאים (סוג או קושי)
            });
        }
    }

    /**
     * מנקה את העיצוב מכל חברי הקבוצה וצובעת בצבע מלא וטקסט לבן רק את הכפתור שנבחר כעת
     */
    private void updateButtonUI(Button selected, Button[] group) {
        for (Button b : group) {
            if (b != null) {
                // ביטול ה-Tint המובנה של Material Component כדי לאפשר הצגת רקע שקוף לחלוטין
                b.setBackgroundTintList(null);
                b.setBackgroundResource(android.R.color.transparent);
                b.setTextColor(Color.parseColor("#1A4375")); // צבע כחול כהה ייחודי לפילאטיס
            }
        }
        selected.setBackgroundResource(R.drawable.pilates_selected); // החלת רקע מעוצב לכפתור הנבחר
        selected.setTextColor(Color.WHITE);
    }

    /**
     * ממשק (Interface) מקומי המאפשר פולימורפיזם ושימוש חוזר בפונקציית setupSelection
     */
    interface OnSelectionListener { void onSelected(Button b); }
}