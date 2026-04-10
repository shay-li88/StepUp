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
import com.example.stepup.Post;
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

public class FeedActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";
    private TextView tvHelloUser, tvStreak, tvPoints;
    private LinearLayout btnRunning, btnStrength, btnCardio, btnPilates;
    private ArrayList<Post> postsList;
    private PostAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
        setContentView(R.layout.activity_feed);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // אתחול הרשימה כדי שלא תקרוס
        postsList = new ArrayList<>();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        displayUserData();
        checkAndResetStreak();
        setupBottomNavigation();
        askNotificationPermission();

        // הפעלת האזנה בזמן אמת לפוסטים
        registerToNewPosts();
    }

    // הפונקציה החדשה לעדכונים בזמן אמת
    private void registerToNewPosts() {
        Log.d(TAG, "registerToNewPosts: start");

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
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New post: " + dc.getDocument().getData());
                                        Post post = dc.getDocument().toObject(Post.class);
                                        // הוספה לראש הרשימה
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
                            // עדכון האדפטר שלך כדי שהשינוי יופיע במסך
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

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void displayUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String currentUserId = user.getUid();
            String name = user.getDisplayName();
            tvHelloUser.setText("Hello, " + (name != null && !name.isEmpty() ? name : "User") + "!");

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

    private void checkAndResetStreak() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("Workouts")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot lastDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Object timestampObj = lastDoc.get("timestamp");
                        Date lastWorkoutDate = null;

                        if (timestampObj instanceof Timestamp) {
                            lastWorkoutDate = ((Timestamp) timestampObj).toDate();
                        } else if (timestampObj instanceof Long || timestampObj instanceof Double) {
                            lastWorkoutDate = new Date(((Number) timestampObj).longValue());
                        }

                        if (lastWorkoutDate != null) {
                            validateStreakLogic(uid, lastWorkoutDate);
                        }
                    } else {
                        db.collection("users").document(uid).update("streak", 0);
                    }
                })
                .addOnFailureListener(e -> Log.e("Streak", "Error checking streak: " + e.getMessage()));
    }

    private void validateStreakLogic(String uid, Date lastWorkoutDate) {
        Date now = new Date();
        long diffInMillies = Math.abs(now.getTime() - lastWorkoutDate.getTime());
        long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        if (diffInDays >= 2) {
            db.collection("users").document(uid).update("streak", 0);
            Log.d("Streak", "Streak reset - more than 48 hours passed");
        }
    }

    private void setupListeners() {
        btnRunning.setOnClickListener(v -> startActivity(new Intent(this, RunningActivity.class)));
        btnStrength.setOnClickListener(v -> startActivity(new Intent(this, StrengthActivity.class)));
        btnCardio.setOnClickListener(v -> startActivity(new Intent(this, CardioActivity.class)));
        btnPilates.setOnClickListener(v -> startActivity(new Intent(this, PilatesActivity.class)));

        tvHelloUser.setOnLongClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_feed);
        if (bottomNav != null) {
            bottomNav.setItemIconTintList(null);
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;

                Intent intent = null;
                if (id == R.id.nav_workouts) intent = new Intent(this, MyWorkoutsActivity.class);
                else if (id == R.id.nav_posts) intent = new Intent(this, PostsActivity.class);
                else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);
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