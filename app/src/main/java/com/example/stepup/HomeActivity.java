package com.example.stepup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.stepup.utils.PostAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * מסך הבית הראשי (HomeActivity):
 * משמש כמרכז האפליקציה (Dashboard). מציג נתוני משתמש וסטטיסטיקות (כוכבים ורצף) בזמן אמת,
 * מנהל את תפריט הניווט התחתון (BottomNavigationView), מאפשר מעבר לארבעת סוגי האימונים,
 * ומפעיל מנגנון רקע חכם לבדיקת תוקף הסטריק (מניעת התיישנות מעבר ל-48 שעות).
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";
    private TextView tvHelloUser, tvStreak, tvPoints;
    private LinearLayout btnRunning, btnStrength, btnCardio, btnPilates;
    private ArrayList<Post> postsList;
    private PostAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // רכיב Android Jetpack מודרני לניהול ובקשת הרשאות בזמן ריצה (Runtime Permissions) עבור התראות
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                } else {
                    Log.d(TAG, "Notification permission denied");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // אתחול הרשימה כדי שלא תקרוס
        postsList = new ArrayList<>();

        // הגדרת תצוגת EdgeToEdge ומניעת חפיפה עם שורת הסטטוס והניווט של המכשיר
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // הגדרה וניהול של תפריט הניווט התחתון (Bottom Navigation Bar)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_feed);
        bottomNav.setItemIconTintList(null); // מאפשר שימוש בצבעים המקוריים של האייקונים
        bottomNav.setSelectedItemId(R.id.nav_home); // הגדרת הפריט הנוכחי כמסומן
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_workouts) startActivity(new Intent(this, MyWorkoutsActivity.class));
            else if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));
            else if (id == R.id.nav_posts) startActivity(new Intent(this, PostsActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));

            // ביטול האנימציה הדיפולטיבית בין המסכים כדי ליצור חוויית מעבר חלקה לחלוטין (Tabs Style)
            overridePendingTransition(0, 0);
            return true;
        });

        initViews();               // קישור אלמנטים מה-XML
        setupListeners();           // הגדרת מאזיני לחיצה
        displayUserData();          // הצגת נתוני המשתמש והאזנה לשינויים בסטטיסטיקות
        checkAndResetStreak();      // בדיקת לוגיקת תוקף הסטריק היומי
        askNotificationPermission(); // בקשת הרשאת נוטיפיקציות (עבור אנדרואיד 13 ומעלה)

        // הפעלת האזנה בזמן אמת לפוסטים בקהילה
        registerToNewPosts();
    }

    /**
     * פונקציה המבצעת האזנה אקטיבית (Real-time Snapshot Listener) לאוסף הפוסטים.
     * הקוד מזהה באופן סלקטיבי שינויים (הוספה/עריכה/מחיקה) ומעדכן רענון אופטימלי ב-UI.
     */
    private void registerToNewPosts() {
        Log.d(TAG, "registerToNewPosts: start");
        //שאילתה לפוסטים חדשים בזמן אמת
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        if (snapshots != null) {
                            // שימוש ב-DocumentChange על מנת לעבד אך ורק את המסמכים שהשתנו, ולא להטעין הכל מחדש
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New post: " + dc.getDocument().getData());
                                        Post post = dc.getDocument().toObject(Post.class);
                                        // הוספה לראש הרשימה (אינדקס 0) כדי שהפוסטים החדשים ביותר יופיעו למעלה
                                        postsList.add(0, post);
                                        break;
                                    case MODIFIED:
                                        // כאן אפשר להוסיף לוגיקה לעדכון פוסט קיים אם תרצי בעתיד
                                        break;
                                    case REMOVED:
                                        // כאן אפשר להוסיף לוגיקה למחיקת פוסט אם תרצי בעתיד
                                        break;
                                }
                            }
                            // עדכון האדפטר של ה-RecyclerView כדי שהשינוי ישתקף מיידית במסך
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    private void initViews() {
        tvHelloUser = findViewById(R.id.tvHelloUser);
        tvStreak = findViewById(R.id.tvStreak);
        tvPoints = findViewById(R.id.tvPoints);
        btnRunning = findViewById(R.id.btnRunning);
        btnStrength = findViewById(R.id.btnStrength);
        btnCardio = findViewById(R.id.btnCardio);
        btnPilates = findViewById(R.id.btnPilates);
    }

    /**
     * בדיקה ובקשת הרשאות עבור הודעות מתפרצות (עבור מכשירים המריצים Android 13 API 33 ומעלה)
     */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * שליפת שם המשתמש הנוכחי מ-Firebase Auth והגדרתSnapshot Listener רציף למסמך שלו ב-Firestore.
     * כל שינוי בסטריק או בכוכבים במסכים האחרים (ריצה, כוח, פילאטיס, קארדיו) ישתקף כאן אוטומטית ללא צורך בריענון דף.
     */
    private void displayUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String currentUserId = user.getUid();
            String name = user.getDisplayName();
            tvHelloUser.setText("Hello, " + (name != null && !name.isEmpty() ? name : "User") + "!");

            //שאילתה להאזנה בזמן אמת על הסטרייק והכוכבים במסמך המשתמש
            db.collection("users").document(currentUserId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) return;
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Long streak = documentSnapshot.getLong("streak");
                            Long stars = documentSnapshot.getLong("totalStars");

                            tvStreak.setText("Today's streak: " + (streak != null ? streak : 0) + " days");
                            tvPoints.setText(String.valueOf(stars != null ? stars : 0));
                        }
                    });
        }
    }

    /**
     * פונקציה חכמה הבודקת את האימון האחרון שבוצע על ידי המשתמש (מכל סוג שהוא) באוסף Workouts.
     * השאילתה ממוינת בסדר יורד ומגבילה את התוצאה למסמך הבודד האחרון בלבד לחיסכון במשאבים.
     */
    private void checkAndResetStreak() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        //שאילתה לבדיקת האימון האחרון של המשתמש לצורך חישוב הסטריק
        db.collection("Workouts")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING) // הכי חדש ראשון
                .limit(1) // הבאת אימון אחד בלבד
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot lastDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Object timestampObj = lastDoc.get("timestamp");
                        Date lastWorkoutDate = null;

                        // תמיכה רחבה בסוגי המידע השונים (פולימורפיזם של ה-Timestamp) כדי למנוע קריסות המרה
                        if (timestampObj instanceof Timestamp) {
                            lastWorkoutDate = ((Timestamp) timestampObj).toDate();
                        } else if (timestampObj instanceof Long || timestampObj instanceof Double) {
                            lastWorkoutDate = new Date(((Number) timestampObj).longValue());
                        }

                        if (lastWorkoutDate != null) {
                            validateStreakLogic(uid, lastWorkoutDate);
                        }
                    } else {
                        // אם למשתמש אין אימונים בכלל במערכת, הסטריק מאופס אוטומטית ל-0
                        db.collection("users").document(uid).update("streak", 0);
                    }
                })
                .addOnFailureListener(e -> Log.e("Streak", "Error checking streak: " + e.getMessage()));
    }

    /**
     * לוגיקת החישוב של פקיעת תוקף הסטריק:
     * מחשבת את הפרש הזמנים הטהור במילישניות בין הזמן הנוכחי לזמן האימון האחרון.
     * במידה וההפרש שווה או עולה על יומיים (48 שעות קלנדריות) - הסטריק נשבר ומאופס ב-Database.
     */
    private void validateStreakLogic(String uid, Date lastWorkoutDate) {
        Date now = new Date();
        long diffInMillies = Math.abs(now.getTime() - lastWorkoutDate.getTime());
        long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        if (diffInDays >= 2) {
            // שאילתה איפוס הסטריק ל-0 במקרה שעברו יותר מ-48 שעות
            db.collection("users").document(uid).update("streak", 0);
            Log.d("Streak", "Streak reset - more than 48 hours passed");
        }
    }

    private void setupListeners() {
        // מעבר למסכי רישום האימונים השונים
        btnRunning.setOnClickListener(v -> startActivity(new Intent(this, RunningActivity.class)));
        btnStrength.setOnClickListener(v -> startActivity(new Intent(this, StrengthActivity.class)));
        btnCardio.setOnClickListener(v -> startActivity(new Intent(this, CardioActivity.class)));
        btnPilates.setOnClickListener(v -> startActivity(new Intent(this, PilatesActivity.class)));

        // לוגיקה חבויה ונוחה (Easter Egg): לחיצה ארוכה על טקסט הברכה מבצעת התנתקות (Sign Out) מסודרת מהאפליקציה
        tvHelloUser.setOnLongClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        });
    }
}