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

        initViews();
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadUserData();
        }

        setupChart();

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
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
            if (doc != null && doc.exists()) {
                // שם המשתמש
                tvUserName.setText(doc.getString("name") != null ? doc.getString("name") : "User");

                // נתונים עליונים (דואג שזה לא יהיה 0 אם יש נתון)
                tvStreak.setText(String.valueOf(doc.getLong("streak") != null ? doc.getLong("streak") : 0));
                tvStars.setText(String.valueOf(doc.getLong("totalStars") != null ? doc.getLong("totalStars") : 0));
                tvLogs.setText(String.valueOf(doc.getLong("logs") != null ? doc.getLong("logs") : 0));
                tvWorkouts.setText(String.valueOf(doc.getLong("totalWorkouts") != null ? doc.getLong("totalWorkouts") : 0));

                // נתוני גוף
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

    private void setupChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 1.5f)); entries.add(new BarEntry(1, 2.2f));
        entries.add(new BarEntry(2, 2.5f)); entries.add(new BarEntry(3, 3.8f));
        entries.add(new BarEntry(4, 4.2f)); entries.add(new BarEntry(5, 5.0f));
        entries.add(new BarEntry(6, 3.5f));

        BarDataSet dataSet = new BarDataSet(entries, "Weekly Activity");
        dataSet.setColors(new int[]{Color.parseColor("#A5D6A7"), Color.parseColor("#F48FB1"), Color.parseColor("#90CAF9"), Color.parseColor("#B39DDB")});
        dataSet.setDrawValues(false);
        barChart.setData(new BarData(dataSet));
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate();
    }
}