package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvAge, tvHeight, tvWeight, tvBMI;
    private TextView tvStreak, tvStars, tvLogs, tvWorkouts;
    private Button btnEditProfile, btnMyPosts;
    private BarChart barChart;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            initViews();
            loadUserData();
            loadWorkoutStats(); // הפונקציה המעודכנת שמסנכרנת כוכבים
            setupChart();
        }

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        btnMyPosts.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MyPostsActivity.class));
        });
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserNameProfile);
        tvAge = findViewById(R.id.tvAge);
        tvHeight = findViewById(R.id.tvHeight);
        tvWeight = findViewById(R.id.tvWeight);
        tvBMI = findViewById(R.id.tvBMI);
        tvStreak = findViewById(R.id.tvStreakCount);
        tvStars = findViewById(R.id.tvTotalStars);
        tvLogs = findViewById(R.id.tvDaysLogs);
        tvWorkouts = findViewById(R.id.tvTotalWorkouts);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnMyPosts = findViewById(R.id.btnMyPosts);
        barChart = findViewById(R.id.barChart);
    }

    private void loadUserData() {
        db.collection("users").document(userId).addSnapshotListener((doc, e) -> {
            if (e != null) return;
            if (doc != null && doc.exists()) {
                tvUserName.setText(doc.getString("name") != null ? doc.getString("name") : "User");

                // סטטיסטיקות מהמסמך
                tvStreak.setText(String.valueOf(doc.getLong("streak") != null ? doc.getLong("streak") : 0));

                // כאן אנחנו רק מציגים את ה-totalStars מהמסמך, הסנכרון קורה ב-loadWorkoutStats
                tvStars.setText(String.valueOf(doc.getLong("totalStars") != null ? doc.getLong("totalStars") : 0));

                Long age = doc.getLong("age");
                Double h = doc.getDouble("height");
                Double w = doc.getDouble("weight");
                Double bmi = doc.getDouble("bmi");

                tvAge.setText("Age " + (age != null ? age : 0));
                tvHeight.setText((h != null ? h : 0) + " cm");
                tvWeight.setText((w != null ? w : 0) + " kg");
                tvBMI.setText("BMI " + (bmi != null ? String.format("%.1f", bmi) : "0.0"));

                btnEditProfile.setText(age != null && age > 0 ? "Edit Details" : "Add Details");
            }
        });
    }

    private void loadWorkoutStats() {
        db.collection("Workouts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((querySnap, e) -> {
                    if (e != null) return;
                    if (querySnap != null) {
                        int workoutCount = querySnap.size();
                        int calculatedStars = workoutCount * 3;

                        tvWorkouts.setText(String.valueOf(workoutCount));
                        tvLogs.setText(String.valueOf(workoutCount));
                        tvStars.setText(String.valueOf(calculatedStars));

                        db.collection("users").document(userId)
                                .update("totalStars", calculatedStars);

                        // כאן אנחנו קוראים לעדכון הגרף עם הנתונים האמיתיים
                        updateChartWithRealData(querySnap.getDocuments());
                    }
                });
    }

    private void updateChartWithRealData(List<DocumentSnapshot> workouts) {
        float[] daysTimeSum = new float[7];
        // רשימה שתשמור את הסוג הנפוץ ביותר לכל יום
        String[] topTypePerDay = new String[7];
        // מפה לספירת סוגים (סוג אימון -> כמות) לכל יום בנפרד
        ArrayList<java.util.HashMap<String, Integer>> typesCounter = new ArrayList<>();

        for (int i = 0; i < 7; i++) typesCounter.add(new java.util.HashMap<>());

        Calendar cal = Calendar.getInstance();

        for (DocumentSnapshot doc : workouts) {
            Object timestampObj = doc.get("timestamp");
            Date date = null;

            if (timestampObj instanceof com.google.firebase.Timestamp) {
                date = ((com.google.firebase.Timestamp) timestampObj).toDate();
            } else if (timestampObj instanceof Long) {
                date = new Date((Long) timestampObj);
            }

            if (date != null) {
                cal.setTime(date);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

                // 1. צבירת זמן (כמו שביקשת קודם)
                Long workoutMinutes = doc.getLong("time");
                if (workoutMinutes != null) {
                    daysTimeSum[dayOfWeek] += workoutMinutes;
                }

                // 2. ספירת סוג האימון
                String type = doc.getString("type");
                if (type != null) {
                    java.util.HashMap<String, Integer> dayMap = typesCounter.get(dayOfWeek);
                    dayMap.put(type, dayMap.getOrDefault(type, 0) + 1);
                }
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, daysTimeSum[i]));

            // מציאת סוג האימון השולט באותו יום
            String dominantType = "";
            int maxCount = -1;
            for (java.util.Map.Entry<String, Integer> entry : typesCounter.get(i).entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    dominantType = entry.getKey();
                }
            }

            // קביעת צבע לפי הסוג השולט (Default אפור אם אין אימונים)
            colors.add(getColorForType(dominantType));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Workout Duration (Minutes)");
        dataSet.setColors(colors); // שימוש ברשימת הצבעים הדינמית

        dataSet.setDrawValues(true);
        BarData data = new BarData(dataSet);
        barChart.setData(data);

        String[] daysNames = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(daysNames));
        barChart.invalidate();
    }

    // פונקציית עזר להמרת שם סוג האימון לצבע
    private int getColorForType(String type) {
        if (type == null) return Color.LTGRAY;

        switch (type) {
            case "Running":
                return Color.parseColor("#75E285"); // ירוק
            case "Pilates":
                return Color.parseColor("#80DEEA"); // תכלת
            case "Strength":
                return Color.parseColor("#EF9BFDFF"); // סגול
            case "Cardio":
                return Color.parseColor("#F48FB1"); // ורוד
            default:
                return Color.parseColor("#90CAF9"); // כחול בהיר ברירת מחדל
        }
    }

    private void setupChart() {
        // הגדרות עיצוב כלליות לגרף (ללא נתונים עדיין)
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setGranularity(1f); // מונע מספרים עשרוניים בציר ה-Y
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
    }
}