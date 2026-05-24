package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stepup.utils.PostAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * מסך פיד הרשת החברתית (PostsActivity):
 * מסך זה מציג את כל הפוסטים והשיתופים של הקהילה באפליקציה באמצעות RecyclerView.
 * הנתונים נמשכים מאוסף "posts" ב-Firestore בטכניקת Real-time Snapshot Listener,
 * וממוינים אוטומטית מהפוסט החדש ביותר לישן ביותר.
 */
public class PostsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);

        db = FirebaseFirestore.getInstance();
        postList = new ArrayList<>(); // אתחול הרשימה הגנרית המכילה את האובייקטים

        // אתחול והגדרת ה-RecyclerView להצגת הפיד
        recyclerView = findViewById(R.id.recyclerViewPosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // הצגה רשימתית אנכית דילולטיבית
        adapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(adapter);

        // כפתור מעבר למסך יצירת פוסט חדש (AddPostsActivity) בראש ה-Header
        Button btnAddPost = findViewById(R.id.btnAddPostHeader);
        btnAddPost.setOnClickListener(v -> {
            startActivity(new Intent(PostsActivity.this, AddPostsActivity.class));
        });

        // הגדרה וניהול של תפריט הניווט התחתון (Bottom Navigation Bar)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_posts);
        bottomNav.setItemIconTintList(null); // שמירה על הצבעים המקוריים של האייקונים
        bottomNav.setSelectedItemId(R.id.nav_posts); // הדגשת הטאב הנוכחי (Posts)
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_posts) return true;
            if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));
            else if (id == R.id.nav_home) startActivity(new Intent(this, HomeActivity.class));
            else if (id == R.id.nav_workouts) startActivity(new Intent(this, MyWorkoutsActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));

            // ביטול האנימציה המובנית בין המעברים ליצירת תחושה של טאבים מהירים
            overridePendingTransition(0, 0);
            return true;
        });

        // הפעלת הפונקציה לשליפת הנתונים והאזנה לשינויים בפיד
        loadPostsFromFirestore();
    }

    /**
     * פונקציה אסינכרונית המאזינה לשינויים באוסף ה-Posts ב-Firestore.
     * כל פוסט חדש שנוסף על ידי משתמש כלשהו ברשת יוקפץ ויעודכן כאן אוטומטית על גבי ה-UI בזמן אמת.
     */
    private void loadPostsFromFirestore() {
        // שאילתה טעינת כל הפוסטים בפיד הציבורי ומיון מהחדש לישן
        // שימוש ב-"posts" באות קטנה להתאמה מלאה ל-DB
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING) // הצגת הפוסטים העדכניים ביותר למעלה
                //האזנה בזמן אמת לכל פוסט חדש שנוסף על ידי משתמש כלשהו
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "שגיאה בטעינת פוסטים", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        postList.clear(); // ניקוי הרשימה הישנה כדי למנוע כפילויות בזמן קבלת Snapshot חדש

                        // ריצה בלולאה על כל ה-Documents שהתקבלו מהשאילתה
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class); // המרת ה-Document של Firestore לאובייקט Java מסוג Post (דה-סריאליזציה)
                            if (post != null) {
                                // שמירת ה-ID של המסמך בתוך האובייקט (קריטי ללייקים ותגובות)
                                // מאחר וה-ID של המסמך עצמו אינו חלק משדות ה-Class הפנימיים, אנו שולפים אותו דינמית באמצעות doc.getId()
                                post.setPostId(doc.getId());
                                postList.add(post);
                            }
                        }
                        // פקודה המודיעה לאדפטר שהרשימה השתנתה לחלוטין ויש לצייר מחדש את ה-Views על המסך
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}