package com.example.stepup;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.stepup.utils.GeminiManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ChallengesActivity extends AppCompatActivity {

    private Button btnGenerate;
    private TextView tvAiResponse;
    private MaterialCardView cardResult;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GeminiManager geminiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_challenges);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        geminiManager = GeminiManager.getInstance();

        initViews();
        setupBottomNavigation();

        btnGenerate.setOnClickListener(v -> fetchWorkoutsAndGenerateChallenge());
    }

    private void initViews() {
        btnGenerate = findViewById(R.id.btnGenerateChallenge);
        tvAiResponse = findViewById(R.id.tvAiResponse);
        cardResult = findViewById(R.id.cardResult);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchWorkoutsAndGenerateChallenge() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מנתח את האימונים שלך ובונה אתגר...");
        pd.show();

        // שליפת 5 אימונים אחרונים כדי להבין את הרמה
        db.collection("Workouts")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder workoutHistory = new StringBuilder();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        workoutHistory.append("- ").append(doc.getString("type"))
                                .append(" (רמה: ").append(doc.get("difficulty")).append(")\n");
                    }

                    generateAiChallenge(workoutHistory.toString(), pd);
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    generateAiChallenge("אין היסטוריית אימונים עדיין", pd);
                });
    }

    private void generateAiChallenge(String history, ProgressDialog pd) {
        String prompt = "הנה היסטוריית האימונים האחרונה של המשתמש:\n" + history +
                "\nצור לו אתגר כושר שבועי מותאם אישית. " +
                "הוסף קישור לחיפוש ביוטיוב לסרטון הדרכה מתאים. " +
                "החזר את התשובה בצורה מעוצבת וברורה בעברית.";

        geminiManager.sendText(prompt, this, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                pd.dismiss();
                cardResult.setVisibility(View.VISIBLE);
                tvAiResponse.setText(result);
            }

            @Override
            public void onError(Throwable error) {
                pd.dismiss();
                Toast.makeText(ChallengesActivity.this, "שגיאה ביצירת אתגר", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_challenges);
        bottomNav.setSelectedItemId(R.id.nav_challenges);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_challenges) return true;
            if (id == R.id.nav_workouts) startActivity(new Intent(this, MyWorkoutsActivity.class));
            else if (id == R.id.nav_posts) startActivity(new Intent(this, PostsActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            else if (id == R.id.nav_home) startActivity(new Intent(this, FeedActivity.class));
            finish();
            return true;
        });
    }
}