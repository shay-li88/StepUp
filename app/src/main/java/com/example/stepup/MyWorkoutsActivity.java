package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.utils.WorkoutAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך אימוני המשתמש (MyWorkoutsActivity):
 * מציג את רשימת כל האימונים שהמשתמש שמר ב-Firestore.
 * כולל מנגנון סינון (Filter) מקומי מהיר המאפשר להציג אימונים לפי סוג או לפי האימונים האחרונים.
 */
public class MyWorkoutsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WorkoutAdapter adapter;

    // אופטימיזציה: מחזיקים שתי רשימות.
    // workoutList - הרשימה הדינמית שמוצגת כרגע על המסך (משתנה לפי הסינון).
    // fullWorkoutList - הרשימה המקורית המלאה שנשמרת בצד כדי שלא נצטרך לפנות ל-Firestore מחדש בכל סינון.
    private List<Workout> workoutList;
    private List<Workout> fullWorkoutList;

    private CardView emptyCard; // כרטיס הודעה שמוצג רק אם אין אימונים להצגה
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_workouts);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // הגדרת סרגל הניווט התחתון ומעבר בין המסכים השונים
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_workouts);
        bottomNav.setItemIconTintList(null);
        bottomNav.setSelectedItemId(R.id.nav_workouts);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_workouts) return true;
            if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));
            else if (id == R.id.nav_home) startActivity(new Intent(this, HomeActivity.class));
            else if (id == R.id.nav_posts) startActivity(new Intent(this, PostsActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            return true;
        });

        // אתחול רכיבי ה-UI של ה-RecyclerView
        recyclerView = findViewById(R.id.recyclerViewWorkouts);
        emptyCard = findViewById(R.id.workoutCardEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // תצוגה אנכית כרשימה טורית

        // אתחול הרשימות והאדפטר
        workoutList = new ArrayList<>();
        fullWorkoutList = new ArrayList<>();
        adapter = new WorkoutAdapter(this, workoutList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // 1. שלב ראשון: משיכת הנתונים מהענן
        loadWorkoutsFromFirestore();

        // 2. שלב שני: האזנה לכפתורי הסינון
        setupFilterButtons();
    }

    /**
     * מקשרת בין כפתורי הסינון ב-XML ללוגיקת הסינון בקוד.
     * כל כפתור קורא לפונקציה filterWorkouts ומעביר לה את שם הקריטריון.
     */
    private void setupFilterButtons() {
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> filterWorkouts("All"));
        findViewById(R.id.btnFilterRunning).setOnClickListener(v -> filterWorkouts("Running"));
        findViewById(R.id.btnFilterStrength).setOnClickListener(v -> filterWorkouts("Strength"));
        findViewById(R.id.btnFilterCardio).setOnClickListener(v -> filterWorkouts("Cardio"));
        findViewById(R.id.btnFilterPilates).setOnClickListener(v -> filterWorkouts("Pilates"));
        findViewById(R.id.btnFilterRecent).setOnClickListener(v -> filterWorkouts("Recent"));
    }

    /**
     * שליפת כל האימונים המשוייכים למשתמש המחובר מתוך אוסף "Workouts".
     * השאילתה מסודרת בסדר יורד לפי זמן (orderBy DESCENDING) - מהחדש ביותר לישן ביותר.
     */
    private void loadWorkoutsFromFirestore() {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        db.collection("Workouts")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullWorkoutList.clear();

                    // המרת כל מסמך מה-Firestore לאובייקט Java מסוג Workout והוספתו לרשימה המלאה
                    for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                        Workout workout = document.toObject(Workout.class);
                        if (workout != null) fullWorkoutList.add(workout);
                    }

                    // כברירת מחדל, עם עליית המסך מציגים את כל האימונים ("All")
                    filterWorkouts("All");
                })
                .addOnFailureListener(e -> {
                    Log.e("MyWorkouts", "Error: " + e.getMessage());
                });
    }

    /**
     * לוגיקת הסינון המקומית - מפלטרת את האימונים בזיכרון המכשיר ללא צורך בפנייה נוספת לאינטרנט.
     */
    private void filterWorkouts(String criteria) {
        workoutList.clear(); // מרוקנים את הרשימה המוצגת כדי למלא אותה מחדש בתוצאות הרלוונטיות
        String searchCriteria = criteria.toLowerCase().trim();

        if (criteria.equals("All")) {
            // מציג את הכל - מעתיק את כל האימונים מרשימת הגיבוי המלאה
            workoutList.addAll(fullWorkoutList);
        } else if (criteria.equals("Recent")) {
            // מציג רק את 5 האימונים האחרונים (המערך כבר ממוין מהשרת מהחדש לישן)
            int limit = Math.min(fullWorkoutList.size(), 5);
            for (int i = 0; i < limit; i++) {
                workoutList.add(fullWorkoutList.get(i));
            }
        } else {
            // סינון חכם לפי סוג אימון: רצים על כל הרשימה ובודקים התאמה של תתי-מחרוזות באותיות קטנות
            for (Workout w : fullWorkoutList) {
                if (w.getType() != null && w.getType().toLowerCase().contains(searchCriteria)) {
                    workoutList.add(w);
                }
            }
        }

        // פקודה קריטית שמורה ל-RecyclerView לרענן ולצייר מחדש את האימונים שסיננו
        adapter.notifyDataSetChanged();

        // בדיקה קוסמטית: אם הרשימה המסוננת ריקה, נציג כרטיס ריק ("אין אימונים מסוג זה") ונחביא את הרשימה
        if (workoutList.isEmpty()) {
            emptyCard.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyCard.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}