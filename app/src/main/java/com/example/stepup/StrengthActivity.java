package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

/**
 * מסך רישום אימון כוח (StrengthActivity):
 * מאפשר למשתמש לבחור את רמת העומס (Difficulty) ואת סוג קבוצת השרירים (Upper/Lower/Full Body).
 * הנתונים נשמרים ב-Firestore, ובדומה למסך הריצה, מעדכנים את מדדי הכוכבים והרצף (Streak) של המשתמש.
 */
public class StrengthActivity extends AppCompatActivity {

    private Button btnLight, btnModerate, btnHeavy, btnUpper, btnLower, btnFull, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = "Light"; // ברירת מחדל לרמת הקושי
    private String selectedType = "Full Body";   // ברירת מחדל לסוג אימון הכוח
    private static final String TAG = "StrengthActivity";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_strength);

        db = FirebaseFirestore.getInstance();

        // הגדרת מערכת ה-EdgeToEdge להתאמת שולי המסך (System Bars Insets)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews(); // קישור רכיבי ה-XML

        // אתחול בורר הזמן (דקות)
        if (timePicker != null) {
            timePicker.setMinValue(5);
            timePicker.setMaxValue(120);
            timePicker.setValue(5);
        }

        // הגדרת לוגיקת בחירה חכמה מבוססת ממשק (Interface) לקבוצות הכפתורים השונות
        setupSelection(new Button[]{btnLight, btnModerate, btnHeavy}, btn -> selectedDifficulty = btn.getText().toString());
        setupSelection(new Button[]{btnUpper, btnLower, btnFull}, btn -> selectedType = btn.getText().toString());

        // כפתור שמירה ושליחה
        btnGo.setOnClickListener(v -> saveStrengthWorkout());
    }

    private void initViews() {
        btnLight = findViewById(R.id.btnLight);
        btnModerate = findViewById(R.id.btnModerate);
        btnHeavy = findViewById(R.id.btnHeavy);
        btnUpper = findViewById(R.id.btnUpper);
        btnLower = findViewById(R.id.btnLower);
        btnFull = findViewById(R.id.btnFull);
        btnGo = findViewById(R.id.btnGoStrength);
        timePicker = findViewById(R.id.strengthTimePicker);
        etNotes = findViewById(R.id.etStrengthNotes);
    }

    /**
     * פונקציה האוספת את נתוני המשתמש ומפרסמת אובייקט Workout מותאם לאוסף "Workouts" ב-Firestore
     */
    private void saveStrengthWorkout() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        // בניית שם דינמי לאימון המשלב את סוג השריר שנבחר (לדוגמה: "Strength - Upper Body")
        Workout newWorkout = new Workout("Strength - " + selectedType, selectedDifficulty, timePicker.getValue(), etNotes.getText().toString(), 0.0);
        newWorkout.setUserId(currentUserId);
        newWorkout.setTimestamp(Timestamp.now());

        db.collection("Workouts").add(newWorkout)
                .addOnSuccessListener(documentReference -> {
                    // עדכון נקודות וסטריק
                    updateUserStats(currentUserId);

                    Toast.makeText(this, "Workout saved! +3 Stars", Toast.LENGTH_SHORT).show();
                    finish(); // סגירת המסך וחזרה למסך הקודם
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving workout", e));
    }

    /**
     * פונקציה זהה לחלוטין לזו שבמסך הריצה – מבטיחה סנכרון ואינטגרציה מלאה מול ה-Streak והכוכבים בפרופיל
     */
    private void updateUserStats(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        //שאילתה שליפת נתוני המשתמש כדי לבדוק מתי עודכן הסטרייק לאחרונה
        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // תמיד מעדכנים כוכבים (כל אימון נותן כוכבים)
                //שאילתה עדכון מספר הכוכבים
                userRef.update("totalStars", FieldValue.increment(3));

                // לוגיקה חכמה לסטריק: מעלים רק אם זה האימון הראשון היום
                Timestamp lastUpdateTS = doc.getTimestamp("lastStreakUpdate");
                Date today = new Date();

                if (lastUpdateTS == null || !isSameDay(lastUpdateTS.toDate(), today)) {
                    //שאילתה עדכון הסטרייק ותיעוד זמן העדכון האחרון
                    userRef.update(
                            "streak", FieldValue.increment(1),
                            "lastStreakUpdate", new Timestamp(today)
                    );
                    Log.d(TAG, "Streak incremented!");
                } else {
                    Log.d(TAG, "Already updated streak today.");
                }
            }
        });
    }

    /**
     * פונקציית עזר הבודקת התאמה קלנדרית מלאה בין שני תאריכים (שנה ויום בשנה)
     */
    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * פונקציה גנרית המאזינה ללחיצות של קבוצת כפתורים (Radio Group מותאם אישית בכפתורים רגילים).
     * משתמשת בביטוי למדא (Lambda) ובאינטרפייס הפנימי כדי לעדכן בצורה דינמית את משתני המחלקה.
     */
    private void setupSelection(Button[] group, OnSelectionListener listener) {
        for (Button b : group) {
            if (b == null) continue;
            b.setOnClickListener(v -> {
                updateButtonUI(b, group); // שינוי ויזואלי של הכפתורים
                listener.onSelected(b);   // הפעלת פונקציית הקולבק לעדכון המשתנה המתאים (סוג או קושי)
            });
        }
    }

    /**
     * מנקה את העיצוב מכל חברי הקבוצה וצובעת בצבע מלא וטקסט לבן רק את הכפתור שנלחץ כעת
     */
    private void updateButtonUI(Button selected, Button[] group) {
        for (Button b : group) {
            if (b != null) {
                b.setBackgroundResource(android.R.color.transparent);
                b.setTextColor(Color.parseColor("#4A148C")); // צבע סגול כהה לאימוני כוח
            }
        }
        selected.setBackgroundResource(R.drawable.strength_selected); // רקע מעוצב לכפתור הנבחר
        selected.setTextColor(Color.WHITE);
    }

    /**
     * ממשק (Interface) מקומי המאפשר לבצע פולימורפיזם ולהשתמש באותה פונקציית הבחירה (setupSelection)
     * גם עבור קבוצת כפתורי הקושי וגם עבור קבוצת כפתורי סוג השריר.
     */
    interface OnSelectionListener { void onSelected(Button b); }
}