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

        // טעינת האתגר השמור מה-Firestore (אם קיים) ברגע שהדף נפתח
        loadSavedChallenge();

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

    // פונקציה לטעינת אתגר שמור ממסמך המשתמש
    private void loadSavedChallenge() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String savedChallenge = documentSnapshot.getString("lastAiChallenge");
                if (savedChallenge != null && !savedChallenge.isEmpty()) {
                    cardResult.setVisibility(View.VISIBLE);
                    displayFormattedChallenge(savedChallenge);
                }
            }
        }).addOnFailureListener(e -> Log.e("Challenges", "Error loading saved challenge", e));
    }

    // פונקציה לשמירת האתגר ב-Firestore תחת שדה ייעודי במסמך המשתמש
    private void saveChallengeToFirestore(String challengeText) {
        String uid = mAuth.getUid();
        if (uid != null) {
            db.collection("users").document(uid)
                    .update("lastAiChallenge", challengeText)
                    .addOnSuccessListener(aVoid -> Log.d("Challenges", "Challenge saved successfully!"))
                    .addOnFailureListener(e -> Log.e("Challenges", "Error saving challenge", e));
        }
    }

    // פונקציית עזר לעיצוב והצגת הטקסט (HTML) ב-TextView
    private void displayFormattedChallenge(String text) {
        // ניקוי כוכביות שאולי השתרבבו מה-AI
        String formatted = text.replace("**", "");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvAiResponse.setText(android.text.Html.fromHtml(formatted, android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvAiResponse.setText(android.text.Html.fromHtml(formatted));
        }

        // הופך קישורים ללחיצים
        tvAiResponse.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void fetchWorkoutsAndGenerateChallenge() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("Workouts")
                .whereEqualTo("userId", uid)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showNoWorkoutsMessage();
                        return;
                    }

                    ProgressDialog pd = new ProgressDialog(this);
                    pd.setMessage("מנתח את האימונים שלך...");
                    pd.show();

                    StringBuilder workoutHistory = new StringBuilder();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type") != null ? doc.getString("type") : "אימון כללי";
                        String diff = doc.get("difficulty") != null ? doc.get("difficulty").toString() : "לא ידוע";
                        workoutHistory.append("- ").append(type).append(" (רמה: ").append(diff).append(")\n");
                    }

                    generateAiChallenge(workoutHistory.toString(), pd);
                })
                .addOnFailureListener(e -> {
                    Log.e("Challenges", "Error: " + e.getMessage());
                    Toast.makeText(this, "אופס, משהו השתבש בגישה לנתונים", Toast.LENGTH_SHORT).show();
                });
    }

    private void showNoWorkoutsMessage() {
        cardResult.setVisibility(View.VISIBLE);
        tvAiResponse.setText("עדיין לא נרשמו אימונים במערכת.\n\nכדי שאוכל לייצר לך אתגר מותאם אישית, כדאי להתחיל להתאמן!");
        btnGenerate.setText("יאללה, בוא נתחיל להתאמן!");
        btnGenerate.setOnClickListener(v -> startActivity(new Intent(this, MyWorkoutsActivity.class)));
    }

    private void generateAiChallenge(String history, ProgressDialog pd) {
        String prompt = "הנה היסטוריית האימונים של המשתמש:\n" + history +
                "\nצור אתגר כושר שבועי תמציתי. הנחיות עיצוב:" +
                "\n1. את הכותרת תעטוף בתגית <b>." +
                "\n2. אחרי כל סעיף הוסף תגית <br><br>." +
                "\n3. בסוף, כתוב קישור הדרכה ב-HTML." +
                "\n4. אל תשתמש בכוכביות בכלל.";

        geminiManager.sendText(prompt, this, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                pd.dismiss();
                cardResult.setVisibility(View.VISIBLE);

                // 1. הצגת האתגר המעוצב על המסך
                displayFormattedChallenge(result);

                // 2. שמירת האתגר ב-Firestore כדי שיופיע בכניסה הבאה
                saveChallengeToFirestore(result);
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