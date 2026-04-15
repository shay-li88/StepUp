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

        // הורדנו את ה-orderBy כרגע כדי למנוע קריסה אם לא הגדרת אינדקס ב-Firebase
        db.collection("Workouts")
                .whereEqualTo("userId", uid)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // 1. בדיקה אם הרשימה ריקה לגמרי
                    if (queryDocumentSnapshots.isEmpty()) {
                        showNoWorkoutsMessage();
                        return;
                    }

                    // 2. אם הגענו לכאן, יש לפחות אימון אחד!
                    ProgressDialog pd = new ProgressDialog(this);
                    pd.setMessage("מנתח את האימונים שלך...");
                    pd.show();

                    StringBuilder workoutHistory = new StringBuilder();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type") != null ? doc.getString("type") : "אימון כללי";
                        String diff = doc.get("difficulty") != null ? doc.get("difficulty").toString() : "לא ידוע";

                        workoutHistory.append("- ").append(type)
                                .append(" (רמה: ").append(diff).append(")\n");
                    }

                    generateAiChallenge(workoutHistory.toString(), pd);
                })
                .addOnFailureListener(e -> {
                    // אם בכל זאת יש שגיאה (למשל בעיית רשת)
                    Log.e("Challenges", "Error: " + e.getMessage());
                    Toast.makeText(this, "אופס, משהו השתבש בגישה לנתונים", Toast.LENGTH_SHORT).show();
                });
    }

    // פונקציית עזר להצגת ההודעה שביקשת
    private void showNoWorkoutsMessage() {
        cardResult.setVisibility(View.VISIBLE);
        tvAiResponse.setText("עדיין לא נרשמו אימונים במערכת.\n\nכדי שאוכל לייצר לך אתגר מותאם אישית, כדאי להתחיל להתאמן או לייצר אימון חדש בדף האימונים! 💪");

        // אופציונלי: שינוי הטקסט בכפתור כדי להניע אותו לפעולה
        btnGenerate.setText("יאללה, בוא נתחיל להתאמן!");
        btnGenerate.setOnClickListener(v -> {
            startActivity(new Intent(this, MyWorkoutsActivity.class));
        });
    }
    private void generateAiChallenge(String history, ProgressDialog pd) {
        // הוספת הנחיה לתמציתיות בריבוע (בקשת ריכוז בנקודות חשובות)
        String prompt = "הנה היסטוריית האימונים של המשתמש:\n" + history +
                "\nצור אתגר כושר שבועי תמציתי. הנחיות:" +
                "\n1. בלי כוכביות (**) ובלי Markdown בכלל." +
                "\n2. בסוף, הוסף את המשפט המדויק: 'לחץ כאן לסרטון הדרכה' ואחריו את ה-URL של יוטיוב." +
                "\n3. ישר לעניין בעברית.";

        geminiManager.sendText(prompt, this, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                pd.dismiss();
                cardResult.setVisibility(View.VISIBLE);

                // 1. ניקוי סימני Markdown אם נשארו
                String cleanText = result.replace("**", "").replace("*", "");

                // 2. זיהוי הקישור והפיכתו ל-HTML Link
                // אנחנו מחפשים את הכתובת שמתחילה ב-http ומחליפים אותה בתגית לחיצה
                String htmlText = cleanText.replaceAll(
                        "(https?://[^\\s]+)",
                        "<a href=\"$1\">לחץ כאן לסרטון הדרכה</a>"
                );

                // 3. הצגת הטקסט כ-HTML
                tvAiResponse.setText(android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_COMPACT));

                // 4. חשוב מאוד: מאפשר ללחוץ על הקישור בתוך ה-TextView
                tvAiResponse.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
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