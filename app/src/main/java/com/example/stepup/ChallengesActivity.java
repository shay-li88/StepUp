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

/**
 * מסך האתגרים (ChallengesActivity):
 * מסך זה משתמש בבינה מלאכותית (Gemini API) כדי לנתח את היסטוריית האימונים של המשתמש
 * מתוך ה-Firestore, ולייצר עבורו אתגר כושר שבועי מותאם אישית.
 */
public class ChallengesActivity extends AppCompatActivity {

    // רכיבי ה-UI של המסך
    private Button btnGenerate;
    private TextView tvAiResponse;
    private MaterialCardView cardResult;

    // מנהלי מסדי הנתונים, ה-Auth והבינה המלאכותית
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GeminiManager geminiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // מאפשר עיצוב קצה לקצה (מסך מלא כולל שורת הסטטוס)
        setContentView(R.layout.activity_challenges);

        // אתחול מנהלי המערכת וה-Singleton של Gemini
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        geminiManager = GeminiManager.getInstance();

        // הגדרה וניהול של תפריט הניווט התחתון (Bottom Navigation)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_challenges);
        bottomNav.setItemIconTintList(null);
        bottomNav.setSelectedItemId(R.id.nav_challenges);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_challenges) return true;
            if (id == R.id.nav_workouts) startActivity(new Intent(this, MyWorkoutsActivity.class));
            else if (id == R.id.nav_home) startActivity(new Intent(this, HomeActivity.class));
            else if (id == R.id.nav_posts) startActivity(new Intent(this, PostsActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0); // ביטול האנימציה למעבר ניווט חלק
            return true;
        });

        initViews();

        // טעינת האתגר השמור מה-Firestore (אם קיים) ברגע שהדף נפתח
        loadSavedChallenge();

        // הגדרת לחיצה על כפתור יצירת האתגר
        btnGenerate.setOnClickListener(v -> fetchWorkoutsAndGenerateChallenge());
    }

    /**
     * קישור רכיבי ה-XML ל-Java וטיפול במרווחי מערכת (Padding של שורת הסטטוס)
     */
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

    /**
     * פונקציה לטעינת אתגר שמור ממסמך המשתמש (שליפה חד-פעמית באמצעות .get())
     * מונעת מהמשתמש לבזבז קריאות AI מיותרות בכל פעם שהוא נכנס למסך.
     */
    private void loadSavedChallenge() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // משיכת הטקסט השמור של האתגר האחרון שיוצר
                String savedChallenge = documentSnapshot.getString("lastAiChallenge");
                if (savedChallenge != null && !savedChallenge.isEmpty()) {
                    cardResult.setVisibility(View.VISIBLE); // הצגת כרטיס התוצאה
                    displayFormattedChallenge(savedChallenge); // הצגת הטקסט המעוצב
                }
            }
        }).addOnFailureListener(e -> Log.e("Challenges", "Error loading saved challenge", e));
    }

    /**
     * פונקציה לשמירת האתגר ב-Firestore תחת שדה ייעודי במסמך המשתמש
     */
    private void saveChallengeToFirestore(String challengeText) {
        String uid = mAuth.getUid();
        if (uid != null) {
            // עדכון שדה בודד (update) במסמך המשתמש עם הטקסט החדש שהתקבל מה-AI
            db.collection("users").document(uid)
                    .update("lastAiChallenge", challengeText)
                    .addOnSuccessListener(aVoid -> Log.d("Challenges", "Challenge saved successfully!"))
                    .addOnFailureListener(e -> Log.e("Challenges", "Error saving challenge", e));
        }
    }

    /**
     * פונקציית עזר לעיצוב והצגת הטקסט (HTML) ב-TextView.
     * הופכת תגיות כמו <b> ו-<br> לעיצוב ויזואלי אמיתי על המסך.
     */
    private void displayFormattedChallenge(String text) {
        // ניקוי כוכביות שאולי השתרבבו מה-AI ליתר ביטחון
        String formatted = text.replace("**", "");

        // התאמת קוד ה-Html.fromHtml לפי גרסת האנדרואיד של המכשיר
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvAiResponse.setText(android.text.Html.fromHtml(formatted, android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvAiResponse.setText(android.text.Html.fromHtml(formatted));
        }

        // הופך קישורי אינטרנט (A href) בתוך הטקסט ללחיצים עבור המשתמש
        tvAiResponse.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    /**
     * שליפת חמשת האימונים האחרונים כדי ליצור אתגר מותאם שאילתה.
     * הפונקציה ניגשת לאוסף Workouts, מסננת לפי המשתמש, ומגבילה ל-5 מסמכים בלבד (.limit(5)).
     */
    private void fetchWorkoutsAndGenerateChallenge() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("Workouts")
                .whereEqualTo("userId", uid)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // הגנה: אם המשתמש מעולם לא התאמן, אין ל-AI נתונים לנתח
                    if (queryDocumentSnapshots.isEmpty()) {
                        showNoWorkoutsMessage();
                        return;
                    }

                    // פידבק למשתמש (חלון טעינה) בזמן שהאפליקציה פונה לשרתי ה-AI
                    ProgressDialog pd = new ProgressDialog(this);
                    pd.setMessage("מנתח את האימונים שלך...");
                    pd.show();

                    // בניית מחרוזת (String) המרכזת את סוגי האימונים והקושי שלהם
                    StringBuilder workoutHistory = new StringBuilder();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type") != null ? doc.getString("type") : "אימון כללי";
                        String diff = doc.get("difficulty") != null ? doc.get("difficulty").toString() : "לא ידוע";
                        workoutHistory.append("- ").append(type).append(" (רמה: ").append(diff).append(")\n");
                    }

                    // העברת היסטוריית הטקסט הבנויה לפונקציה שמדברת עם Gemini
                    generateAiChallenge(workoutHistory.toString(), pd);
                })
                .addOnFailureListener(e -> {
                    Log.e("Challenges", "Error: " + e.getMessage());
                    Toast.makeText(this, "אופס, משהו השתבש בגישה לנתונים", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * מציגה הודעה ידידותית אם המשתמש עדיין לא ביצע אף אימון, ומפנה אותו להתאמן
     */
    private void showNoWorkoutsMessage() {
        cardResult.setVisibility(View.VISIBLE);
        tvAiResponse.setText("עדיין לא נרשמו אימונים במערכת.\n\nכדי שאוכל לייצר לך אתגר מותאם אישית, כדאי להתחיל להתאמן!");
        btnGenerate.setText("יאללה, בוא נתחיל להתאמן!");
        btnGenerate.setOnClickListener(v -> startActivity(new Intent(this, MyWorkoutsActivity.class)));
    }

    /**
     * פנייה לשרתי Gemini באמצעות ה-GeminiManager.
     * מרכיבה את ה"פרומפט" (ההנחיה לבינה המלאכותית), שולחת אותו ומטפלת בתשובה שמחזיר השרת.
     */
    private void generateAiChallenge(String history, ProgressDialog pd) {
        // בניית הפרומפט: הזרקת היסטוריית האימונים ומתן הנחיות עיצוב קשוחות (כדי שיחזור טקסט בפורמט HTML תקין)
        String prompt = "הנה היסטוריית האימונים של המשתמש:\n" + history +
                "\nצור אתגר כושר שבועי תמציתי. הנחיות עיצוב:" +
                "\n1. את הכותרת תעטוף בתגית <b>." +
                "\n2. אחרי כל סעיף הוסף תגית <br><br>." +
                "\n3. בסוף, כתוב קישור הדרכה ב-HTML." +
                "\n4. אל תשתמש בכוכביות בכלל.";

        // קריאה אסינכרונית ל-API של Gemini
        geminiManager.sendText(prompt, this, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                pd.dismiss(); // סגירת חלון הטעינה
                cardResult.setVisibility(View.VISIBLE);

                // 1. הצגת האתגר המעוצב על המסך
                displayFormattedChallenge(result);

                // 2. שמירת האתגר ב-Firestore כדי שיופיע בכניסה הבאה של המשתמש
                saveChallengeToFirestore(result);
            }

            @Override
            public void onError(Throwable error) {
                pd.dismiss();
                Toast.makeText(ChallengesActivity.this, "שגיאה ביצירת אתגר", Toast.LENGTH_SHORT).show();
            }
        });
    }
}