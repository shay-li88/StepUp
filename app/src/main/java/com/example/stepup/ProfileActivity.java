package com.example.stepup;

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
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvAge, tvHeight, tvWeight, tvBMI;
    private TextView tvStreak, tvStars, tvLogs, tvWorkouts;
    private Button btnEditProfile, btnMyPosts;
    private BarChart barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. אתחול רכיבים
        initViews();

        // 2. הגדרת הגרף (גם אם אין נתונים, שיראו את הצירים)
        setupChart();

        // 3. דוגמה להגדרת נתונים ראשונית (במציאות זה יגיע מ-Firebase)
        mockDataCheck();
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

    private void mockDataCheck() {
        // בדיקה אם הנתונים הם 0 (כמו שביקשת)
        int age = 0; // נניח שזה מה שחזר מה-DB

        if (age == 0) {
            btnEditProfile.setText("Add Details");
            tvAge.setText("Age 0");
            tvHeight.setText("0 cm");
            tvWeight.setText("0 kg");
            tvBMI.setText("BMI 0.0");
        } else {
            btnEditProfile.setText("Edit Details");
            // כאן תכניסי נתונים אמיתיים
        }
    }

    private void setupChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        // הוספת נתונים לדוגמה לימי השבוע (Mon-Sun)
        entries.add(new BarEntry(0, 1.5f)); // Mon
        entries.add(new BarEntry(1, 2.2f)); // Tue
        entries.add(new BarEntry(2, 2.5f)); // Wed
        entries.add(new BarEntry(3, 3.8f)); // Thu
        entries.add(new BarEntry(4, 4.2f)); // Fri
        entries.add(new BarEntry(5, 5.0f)); // Sat
        entries.add(new BarEntry(6, 3.5f)); // Sun

        BarDataSet dataSet = new BarDataSet(entries, "Activity");

        // הגדרת צבעים פסטליים לעמודות (Running, Cardio, Pilates, Strength)
        int[] colors = {
                Color.parseColor("#A5D6A7"), // Green (Running)
                Color.parseColor("#F48FB1"), // Pink (Cardio)
                Color.parseColor("#90CAF9"), // Blue (Pilates)
                Color.parseColor("#B39DDB")  // Purple (Strength)
        };
        dataSet.setColors(colors);
        dataSet.setDrawValues(false); // שלא יראו מספרים מעל העמודות

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f); // רוחב העמודות

        barChart.setData(data);

        // עיצוב הצירים
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        barChart.getAxisRight().setEnabled(false); // ביטול ציר ימין
        barChart.getDescription().setEnabled(false); // ביטול טקסט תיאור
        barChart.animateY(1000); // אנימציה של עליה
        barChart.invalidate();
    }
}