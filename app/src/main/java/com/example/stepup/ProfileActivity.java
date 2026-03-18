package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

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

        // 1. אתחול רכיבים
        initViews();

        // 2. אתחול Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // 3. טעינת נתונים בזמן אמת מה-Firestore
            loadUserData();
        }

        // 4. הגדרת הגרף
        setupChart();

        // 5. כפתור מעבר למסך עריכה
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        // כפתור פוסטים (כרגע רק Toast או הכנה לעתיד)
        btnMyPosts.setOnClickListener(v -> {
            // כאן יבוא הקוד למעבר למסך הפוסטים שלך
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
        // המאזין הזה מעדכן את המסך אוטומטית בכל פעם שיש שינוי ב-DB
        db.collection("users").document(userId).addSnapshotListener((documentSnapshot, e) -> {
            if (documentSnapshot != null && documentSnapshot.exists()) {

                String name = documentSnapshot.getString("name");
                Long age = documentSnapshot.getLong("age");
                Double height = documentSnapshot.getDouble("height");
                Double weight = documentSnapshot.getDouble("weight");
                Double bmi = documentSnapshot.getDouble("bmi");

                // עדכון התצוגה
                tvUserName.setText(name != null ? name : "User");
                tvAge.setText("Age " + (age != null ? age : 0));
                tvHeight.setText((height != null ? height : 0) + " cm");
                tvWeight.setText((weight != null ? weight : 0) + " kg");
                tvBMI.setText("BMI " + (bmi != null ? String.format("%.1f", bmi) : "0.0"));

                // שינוי טקסט הכפתור לפי מצב הנתונים
                if (age != null && age > 0) {
                    btnEditProfile.setText("Edit Details");
                } else {
                    btnEditProfile.setText("Add Details");
                }
            }
        });
    }

    private void setupChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        // נתונים סטטיים כרגע - בהמשך נמשוך מה-DB לפי אימונים
        entries.add(new BarEntry(0, 1.5f));
        entries.add(new BarEntry(1, 2.2f));
        entries.add(new BarEntry(2, 2.5f));
        entries.add(new BarEntry(3, 3.8f));
        entries.add(new BarEntry(4, 4.2f));
        entries.add(new BarEntry(5, 5.0f));
        entries.add(new BarEntry(6, 3.5f));

        BarDataSet dataSet = new BarDataSet(entries, "Weekly Activity");

        int[] colors = {
                Color.parseColor("#A5D6A7"), // Green
                Color.parseColor("#F48FB1"), // Pink
                Color.parseColor("#90CAF9"), // Blue
                Color.parseColor("#B39DDB"), // Purple
                Color.parseColor("#FFCC80"), // Orange
                Color.parseColor("#CE93D8"), // Light Purple
                Color.parseColor("#80CBC4")  // Teal
        };

        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }
}