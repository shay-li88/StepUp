package com.example.stepup;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.stepup.utils.OnResultCallback;
import com.example.stepup.utils.ProfileManager;
import com.example.stepup.utils.UserImageSelector;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private UserImageSelector userImageSelector;
    private ProfileManager profileManager; // שימוש ב-Manager החדש
    private ShapeableImageView ivUserProfile;
    private TextView tvUserName, tvAge, tvHeight, tvWeight, tvBMI;
    private TextView tvStreak, tvStars, tvLogs, tvWorkouts;
    private Button btnEditProfile, btnMyPosts;
    private BarChart barChart;
    private FirebaseFirestore db;
    private String userId;

    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        profileManager = new ProfileManager(); // אתחול המנהל

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            initViews();
            setupProfileImageLogic();
            loadUserData();
            loadWorkoutStats();
            setupChart();
            setupBottomNavigation();
        } else {
            finish();
        }
    }

    private void initViews() {
        ivUserProfile = findViewById(R.id.ivUserProfile);
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

        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        btnMyPosts.setOnClickListener(v -> startActivity(new Intent(this, MyPostsActivity.class)));
    }

    private void setupProfileImageLogic() {
        // אתחול בחירת תמונה
        userImageSelector = new UserImageSelector(this, ivUserProfile, new OnResultCallback() {
            @Override
            public void onResult(boolean success, String url, String error) {
                if (success) {
                    uploadAndUpdateProfilePicture();
                } else {
                    Log.e(TAG, "Image selection failed: " + error);
                }
            }
        });

        ivUserProfile.setOnClickListener(v -> userImageSelector.showImageSourceDialog());
    }

    private void loadUserData() {
        db.collection("users").document(userId).addSnapshotListener((doc, e) -> {
            if (e != null) return;
            if (doc != null && doc.exists()) {
                tvUserName.setText(doc.getString("name") != null ? doc.getString("name") : "User");

                // משיכת URL וחותמת זמן לעדכון Glide
                String profileImageUrl = doc.getString("profileImageUrl");
                Long lastUpdate = doc.getLong("lastImageUpdate");
                if (lastUpdate == null) lastUpdate = 0L;

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(profileImageUrl)
                            // השימוש ב-Signature מבטיח רענון תמונה כשיש עדכון ב-Firestore
                            .signature(new ObjectKey(lastUpdate))
                            .placeholder(R.drawable.ic_user)
                            .error(R.drawable.ic_user)
                            .circleCrop()
                            .into(ivUserProfile);
                }

                tvStreak.setText(String.valueOf(doc.getLong("streak") != null ? doc.getLong("streak") : 0));
                tvStars.setText(String.valueOf(doc.getLong("totalStars") != null ? doc.getLong("totalStars") : 0));

                Long age = doc.getLong("age");
                Double h = doc.getDouble("height");
                Double w = doc.getDouble("weight");
                Double bmi = doc.getDouble("bmi");

                tvAge.setText("Age " + (age != null ? age : 0));
                tvHeight.setText((h != null ? h : 0) + " cm");
                tvWeight.setText((w != null ? w : 0) + " kg");
                tvBMI.setText("BMI " + (bmi != null ? String.format("%.1f", bmi) : "0.0"));
            }
        });
    }

    private void uploadAndUpdateProfilePicture() {
        File imageFile = userImageSelector.createImageFile();

        if (imageFile != null) {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage("מעלה תמונה ומעדכן פרופיל...");
            pd.setCancelable(false);
            pd.show();

            // שימוש ב-ProfileManager לביצוע ההעלאה והעדכון ב-Firestore במכה אחת
            profileManager.uploadAndSyncProfilePicture(imageFile, new ProfileManager.OnProfileUpdateListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    pd.dismiss();
                    Toast.makeText(ProfileActivity.this, "הפרופיל עודכן בהצלחה!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    pd.dismiss();
                    Toast.makeText(ProfileActivity.this, "שגיאה: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
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
                        db.collection("users").document(userId).update("totalStars", calculatedStars);
                        updateChartWithRealData(querySnap.getDocuments());
                    }
                });
    }

    private void updateChartWithRealData(List<DocumentSnapshot> workouts) {
        float[] daysTimeSum = new float[7];
        ArrayList<java.util.HashMap<String, Integer>> typesCounter = new ArrayList<>();
        for (int i = 0; i < 7; i++) typesCounter.add(new java.util.HashMap<>());

        Calendar cal = Calendar.getInstance();
        for (DocumentSnapshot doc : workouts) {
            Object timestampObj = doc.get("timestamp");
            Date date = null;
            if (timestampObj instanceof com.google.firebase.Timestamp) date = ((com.google.firebase.Timestamp) timestampObj).toDate();
            else if (timestampObj instanceof Long) date = new Date((Long) timestampObj);

            if (date != null) {
                cal.setTime(date);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
                Long workoutMinutes = doc.getLong("time");
                if (workoutMinutes != null) daysTimeSum[dayOfWeek] += workoutMinutes;
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
            String dominantType = "";
            int maxCount = -1;
            if (typesCounter.get(i) != null) {
                for (java.util.Map.Entry<String, Integer> entry : typesCounter.get(i).entrySet()) {
                    if (entry.getValue() > maxCount) {
                        maxCount = entry.getValue();
                        dominantType = entry.getKey();
                    }
                }
            }
            colors.add(getColorForType(dominantType));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Workout Duration (Minutes)");
        dataSet.setColors(colors);
        dataSet.setDrawValues(true);
        barChart.setData(new BarData(dataSet));
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}));
        barChart.invalidate();
    }

    private int getColorForType(String type) {
        if (type == null) return Color.LTGRAY;
        switch (type) {
            case "Running": return Color.parseColor("#75E285");
            case "Pilates": return Color.parseColor("#80DEEA");
            case "Strength": return Color.parseColor("#EF9BFD");
            case "Cardio": return Color.parseColor("#F48FB1");
            default: return Color.parseColor("#90CAF9");
        }
    }

    private void setupChart() {
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_feed);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_profile) return true;
                Intent intent = null;
                if (id == R.id.nav_workouts) intent = new Intent(this, MyWorkoutsActivity.class);
                else if (id == R.id.nav_posts) intent = new Intent(this, PostsActivity.class);
                else if (id == R.id.nav_home) intent = new Intent(this, FeedActivity.class);
                else if (id == R.id.nav_challenges) intent = new Intent(this, ChallengesActivity.class);
                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
                return true;
            });
        }
    }
}