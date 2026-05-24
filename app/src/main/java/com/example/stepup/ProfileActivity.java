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

/**
 * מסך הפרופיל (ProfileActivity):
 * מסך זה מרכז את כל הנתונים הפיזיולוגיים של המשתמש, מדדי הגיימיפיקציה שלו (סטריק, כוכבים וותק),
 * ומציג גרף עמודות מתקדם המנתח את דקות האימון שלו לאורך ימי השבוע הנוכחי לפי סוגי האימונים השונים.
 */
public class ProfileActivity extends AppCompatActivity {
    // מנהלי המערכת ותקשורת מול Firebase Auth & Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // מחלקות עזר שיצרנו לניהול תמונות ועדכון פרופיל
    private UserImageSelector userImageSelector;
    private ProfileManager profileManager;

    // רכיבי ממשק משתמש (UI Elements)
    private ShapeableImageView ivUserProfile;
    private TextView tvUserName, tvAge, tvHeight, tvWeight, tvBMI;
    private TextView tvStreak, tvStars, tvLogs, tvWorkouts;
    private Button btnEditProfile, btnMyPosts, btnLogout;
    private BarChart barChart; // ספריית צד שלישי להצגת גרפים MPAndroidChart

    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // אתחול מנגנוני ה-Firebase ומחלקת ניהול הפרופיל
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        profileManager = new ProfileManager();

        // הגנה קריטית: בודקים שהמשתמש אכן מחובר. אם כן - טוענים נתונים, אם לא - זורקים אותו חזרה
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            initViews();               // קישור רכיבי ה-XML לקוד ה-Java
            setupProfileImageLogic();  // הגדרת לוגיקת בחירת תמונת הפרופיל
            loadUserData();            // שליפת נתוני משתמש מ-Firestore בזמן אמת
            loadWorkoutStats();        // שליפת היסטוריית האימונים לצורך חישוב סטטיסטיקות
            setupChart();              // הגדרת העיצוב הראשוני של הגרף
        } else {
            finish(); // סגירת המסך אם אין משתמש מחובר
        }

        // הגדרת כפתור ההתנתקות מהמערכת וחזרה למסך הלוגין
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());

        // אתחול והגדרת תפריט הניווט התחתון (BottomNavigationView)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_Profile);
        bottomNav.setItemIconTintList(null); // ביטול ה-Tint המובנה כדי לשמור על הצבעים המקוריים של האייקונים
        bottomNav.setSelectedItemId(R.id.nav_profile); // סימון מסך הפרופיל כנבחר כעת
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) return true; // אם המשתמש כבר בפרופיל, אל תעשה כלום
            if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));
            else if (id == R.id.nav_home) startActivity(new Intent(this, HomeActivity.class));
            else if (id == R.id.nav_workouts) startActivity(new Intent(this, MyWorkoutsActivity.class));
            else if (id == R.id.nav_posts) startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0); // ביטול האנימציה בין המעברים למראה ניווט חלק ומהיר
            return true;
        });
    }

    /**
     * פונקציה המקשרת את רכיבי ה-XML ל-Java ומגדירה את מאזיני הלחיצות (Listeners)
     */
    private void initViews() {
        ivUserProfile = findViewById(R.id.ivUserProfile);
        tvUserName = findViewById(R.id.tvUserNameProfile);
        tvAge = findViewById(R.id.tvAge);
        tvHeight = findViewById(R.id.tvHeight);
        tvWeight = findViewById(R.id.tvWeight);
        tvBMI = findViewById(R.id.tvBMI);
        tvStreak = findViewById(R.id.tvStreakCount);
        tvStars = findViewById(R.id.tvTotalStars);
        tvWorkouts = findViewById(R.id.tvTotalWorkouts);
        tvLogs = findViewById(R.id.tvDaysLogs);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnMyPosts = findViewById(R.id.btnMyPosts);
        btnLogout = findViewById(R.id.btnLogout);
        barChart = findViewById(R.id.barChart);

        // מעבר למסכים משניים באפליקציה באמצעות Intents
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        btnMyPosts.setOnClickListener(v -> startActivity(new Intent(this, MyPostsActivity.class)));
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    /**
     * ביצוע ניתוק (Sign Out) מסודר מ-Firebase ומעבר אטומי למסך הכניסה
     */
    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        // דגלים המנקים את היסטוריית המסכים (Stack) כדי שהמשתמש לא יוכל ללחוץ 'חזור' ולהיכנס שוב לפרופיל
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * מאתחלת את הדיאלוג לבחירת מקור התמונה (מצלמה או גלריה) ומגדירה Callback לאחרי הבחירה
     */
    private void setupProfileImageLogic() {
        userImageSelector = new UserImageSelector(this, ivUserProfile, new OnResultCallback() {
            @Override
            public void onResult(boolean success, String url, String error) {
                if (success) {
                    // אם המשתמש בחר בהצלחה תמונה מקומית, נתחיל בתהליך ההעלאה לענן
                    uploadAndUpdateProfilePicture();
                } else {
                    Log.e(TAG, "Image selection failed: " + error);
                }
            }
        });

        // לחיצה על תמונת הפרופיל פותחת את תפריט הבחירה (מצלמה/גלריה)
        ivUserProfile.setOnClickListener(v -> userImageSelector.showImageSourceDialog());
    }

    /**
     * שאילתת שליפה בזמן אמת (addSnapshotListener) מתוך אוסף המשתמשים ב-Firestore.
     * הפונקציה מאזינה לשינויים במסמך המשתמש ומעדכנת את רכיבי הטקסט ותמונת הפרופיל בהתאם.
     */
    private void loadUserData() {
        db.collection("users").document(userId).addSnapshotListener((doc, e) -> {
            if (e != null) return; // במקרה של שגיאת תקשורת - עצור
            if (doc != null && doc.exists()) {
                tvUserName.setText(doc.getString("name") != null ? doc.getString("name") : "User");

                // שליפת הקישור לתמונה וחותמת הזמן (Timestamp) של העדכון האחרון
                String profileImageUrl = doc.getString("profileImageUrl");
                Long lastUpdate = doc.getLong("lastImageUpdate");
                if (lastUpdate == null) lastUpdate = 0L;

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    // שימוש בספריית Glide לטעינה יעילה של תמונות מהרשת עם מנגנון קאש מתוחכם
                    Glide.with(this)
                            .load(profileImageUrl)
                            // חיוני: ה-Signature מבוסס על מילסנדות העדכון האחרון.
                            // זה מכריח את גלייד לרענן את התמונה מהשרת רק אם היא באמת השתנתה, במקום להציג גרסה ישנה מהזיכרון.
                            .signature(new ObjectKey(lastUpdate))
                            .placeholder(R.drawable.ic_user) // תמונת ברירת מחדל בזמן הטעינה
                            .error(R.drawable.ic_user)       // תמונת ברירת מחדל במקרה של שגיאה
                            .circleCrop()                    // חיתוך אוטומטי של התמונה לצורה עגולה מושלמת
                            .into(ivUserProfile);
                }

                // עדכון מדדי הרצף (Streak) והכוכבים מתוך הנתונים שנכתבו במסכי הרישום
                tvStreak.setText(String.valueOf(doc.getLong("streak") != null ? doc.getLong("streak") : 0));
                tvStars.setText(String.valueOf(doc.getLong("totalStars") != null ? doc.getLong("totalStars") : 0));

                // שליפת מדדים פיזיולוגיים וחישוב נתונים
                Long age = doc.getLong("age");
                Double h = doc.getDouble("height");
                Double w = doc.getDouble("weight");
                Double bmi = doc.getDouble("bmi");

                tvAge.setText("Age " + (age != null ? age : 0));
                tvHeight.setText((h != null ? h : 0) + " cm");
                tvWeight.setText((w != null ? w : 0) + " kg");
                tvBMI.setText("BMI " + (bmi != null ? String.format("%.1f", bmi) : "0.0"));

                updateSeniorityStatus(); // עדכון ותצוגה של ותק המשתמש באפליקציה
            }
        });
    }

    /**
     * פונקציה המטפלת בהעלאת קובץ התמונה לשרת הענן (Firebase Storage)
     * ועדכון ה-URL החדש בתוך ה-Database (Firestore) באמצעות ProfileManager.
     */
    private void uploadAndUpdateProfilePicture() {
        File imageFile = userImageSelector.createImageFile();

        if (imageFile != null) {
            // יצירת חלון טעינה (ProgressDialog) כדי להעניק פידבק (UX) שהאפליקציה עובדת ברקע
            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage("מעלה תמונה ומעדכן פרופיל...");
            pd.setCancelable(false); // חסימת המשתמש מלחיצה מחוץ לחלון כדי למנוע קטיעת הפעולה
            pd.show();

            // קריאה לפעולה אסינכרונית ב-ProfileManager המבצעת את ההעלאה והסנכרון במכה אחת
            profileManager.uploadAndSyncProfilePicture(imageFile, new ProfileManager.OnProfileUpdateListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    pd.dismiss(); // העלמת חלון הטעינה
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

    /**
     * שאילתת סינון (whereEqualTo) בזמן אמת השולפת מאוסף ה-Workouts הכללי
     * רק את מסמכי האימונים ששייכים ל-userId הספציפי שמחובר כרגע.
     */
    private void loadWorkoutStats() {
        db.collection("Workouts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((querySnap, e) -> {
                    if (e != null) return;
                    if (querySnap != null) {
                        int workoutCount = querySnap.size(); // ספירת סך כל האימונים שבוצעו
                        int calculatedStars = workoutCount * 3; // לוגיקה עסקית: כל אימון מעניק 3 כוכבים

                        tvWorkouts.setText(String.valueOf(workoutCount));
                        tvStars.setText(String.valueOf(calculatedStars));

                        // שאילתת עדכון (Write Query) השומרת ומסנכרנת את סך הכוכבים המחושב בחזרה למסמך המשתמש
                        db.collection("users").document(userId).update("totalStars", calculatedStars);

                        // שליחת רשימת מסמכי האימונים הגולמיים לעיבוד וריסוק הנתונים עבור הגרף
                        updateChartWithRealData(querySnap.getDocuments());
                    }
                });
    }

    /**
     * ה"מוח" מאחורי עיבוד הנתונים של הגרף: הפונקציה עוברת על כל היסטוריית האימונים,
     * ממיינת אותם לפי ימי השבוע (ראשון-שבת), סוכמת את דקות האימון, וקובעת איזה סוג אימון היה הדומיננטי בכל יום.
     */
    private void updateChartWithRealData(List<DocumentSnapshot> workouts) {
        float[] daysTimeSum = new float[7]; // מערך בגודל 7 המייצג את סך כל דקות האימון לכל יום בשבוע

        // רשימה של HashMaps שתשמש אותנו כטבלת שכיחויות וזמנים לכל סוג אימון בכל יום בנפרד
        ArrayList<java.util.HashMap<String, Long>> typesTimeCounter = new ArrayList<>();
        for (int i = 0; i < 7; i++) typesTimeCounter.add(new java.util.HashMap<>());

        Calendar cal = Calendar.getInstance();

        // שלב א': ריצה בלולאה על כל מסמכי האימונים ופירוק המידע
        for (DocumentSnapshot doc : workouts) {
            Object timestampObj = doc.get("timestamp");
            Date date = null;

            // המרה בטוחה (Safe Casting) של ה-Timestamp שחוזר מהענן לפורמט Date של Java
            if (timestampObj instanceof com.google.firebase.Timestamp) {
                date = ((com.google.firebase.Timestamp) timestampObj).toDate();
            } else if (timestampObj instanceof Long) {
                date = new Date((Long) timestampObj);
            }

            if (date != null) {
                cal.setTime(date);
                // שליפת היום בשבוע (נעה בין 1 ל-7, אנו מפחיתים 1 כדי להתאים למערכים שמתחילים מ-0)
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
                Long workoutMinutes = doc.getLong("time");
                String type = doc.getString("type");

                if (workoutMinutes != null) {
                    daysTimeSum[dayOfWeek] += workoutMinutes; // הוספת הדקות של האימון הנוכחי לסך הכל היומי

                    if (type != null) {
                        java.util.HashMap<String, Long> dayMap = typesTimeCounter.get(dayOfWeek);
                        // סכימת הדקות המצטברות לפי סוג האימון הספציפי באותו היום
                        dayMap.put(type, dayMap.getOrDefault(type, 0L) + workoutMinutes);
                    }
                }
            }
        }

        // שלב ב': בניית מערך העמודות (BarEntries) לגרף והתאמת צבע העמודה לסוג האימון הדומיננטי
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, daysTimeSum[i])); // מיקום X הוא היום (0=Sun), מיקום Y הוא סך הדקות

            String dominantType = "";
            long maxMinutes = -1;

            // אלגוריתם למציאת סוג האימון שקיבל את מירב הדקות באותו היום
            if (typesTimeCounter.get(i) != null) {
                for (java.util.Map.Entry<String, Long> entry : typesTimeCounter.get(i).entrySet()) {
                    if (entry.getValue() > maxMinutes) {
                        maxMinutes = entry.getValue();
                        dominantType = entry.getKey();
                    }
                }
            }
            // קריאה לפונקציית צבעים שמחזירה את הקוד הצבעוני המתאים לסוג האימון הדומיננטי
            colors.add(getColorForType(dominantType));
        }

        // שלב ג': הזנת הנתונים המעובדים לתוך רכיב ה-BarChart של הספרייה ורענונו
        BarDataSet dataSet = new BarDataSet(entries, "Workout Duration (Minutes)");
        dataSet.setColors(colors); // הגדרת מערך הצבעים הדינמי לעמודות
        dataSet.setDrawValues(true); // הצגת הערך המספרי המדויק (דקות) מעל כל עמודה בגרף

        barChart.setData(new BarData(dataSet));
        // הגדרת תוויות הימים בציר ה-X של הגרף
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}));
        barChart.invalidate(); // פקודה קריטית המאלצת את הרכיב לצייר את עצמו מחדש עם הנתונים החדשים
    }

    /**
     * פונקציה המחזירה ייצוג צבעוני קבוע (Color Hex) בהתאם למילות המפתח של סוג האימון.
     * מיושם עם חסינות לשגיאות כתיב (אותיות קטנות/גדולות ורווחים מיותרים).
     */
    private int getColorForType(String type) {
        if (type == null) return Color.LTGRAY;

        String lowerType = type.toLowerCase().trim();

        if (lowerType.contains("running")) {
            return Color.parseColor("#75E285"); // ירוק לריצה
        } else if (lowerType.contains("pilates")) {
            return Color.parseColor("#80DEEA"); // תכלת לפילאטיס
        } else if (lowerType.contains("strength")) {
            return Color.parseColor("#EF9BFD"); // סגול לאימוני כוח
        } else if (lowerType.contains("cardio")) {
            return Color.parseColor("#F48FB1"); // ורוד לקארדיו
        } else {
            return Color.parseColor("#90CAF9"); // כחול בהיר כברירת מחדל
        }
    }

    /**
     * הגדרות עיצוב קוסמטיות ומבניות קבועות עבור רכיב הגרף (צירים, רשתות ומקרא)
     */
    private void setupChart() {
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM); // העברת ציר ה-X לחלק התחתון של הגרף
        barChart.getXAxis().setDrawGridLines(false); // ביטול קווי הרשת האנכיים למראה נקי
        barChart.getAxisRight().setEnabled(false);   // ביטול ציר ה-Y הימני המיותר
        barChart.getAxisLeft().setGranularity(1f);   // הגדרת קפיצות ציר ה-Y ביחידות של 1 לפחות
        barChart.getAxisLeft().setAxisMinimum(0f);   // קביעה שציר ה-Y יתחיל תמיד מ-0 (מניעת ערכים שליליים)
        barChart.getDescription().setEnabled(false); // הסרת טקסט התיאור המובנה של הספרייה בפינה
        barChart.getLegend().setEnabled(false);      // הסרת המקרא (Legend) התחתון
    }

    /**
     * פונקציה המחשבת באופן אטומי ומקומי את ותק המשתמש באפליקציה (בימים)
     * באמצעות משיכת חותמת זמן הרישום המקורית מתוך ה-Metadata של השרת.
     */
    private void updateSeniorityStatus() {
        if (mAuth.getCurrentUser() != null) {
            // משיכת זמן יצירת החשבון המדויק במיליסנדות משרת ה-Auth
            long signupTime = mAuth.getCurrentUser().getMetadata().getCreationTimestamp();
            // חישוב הפרש הזמנים והמרתו לימים (נוסחת מעבר ממיליסנדות ליממה) + 1 עבור היום הנוכחי
            long days = ((System.currentTimeMillis() - signupTime) / (1000 * 60 * 60 * 24)) + 1;

            tvLogs.setText(String.valueOf(days)); // הצגת ותק הימים ברכיב הטקסט
        }
    }
}