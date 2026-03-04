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

public class PostsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);

        // 1. אתחול Firestore ורשימת הפוסטים
        db = FirebaseFirestore.getInstance();
        postList = new ArrayList<>();

        // 2. הגדרת ה-RecyclerView (הצגת הפוסטים)
        recyclerView = findViewById(R.id.recyclerViewPosts); // ודאי שקיים ב-XML
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(adapter);

        // 3. כפתור הוספת פוסט (Add Post) -
        Button btnAddPost = findViewById(R.id.btnAddPostHeader); // ודאי שקיים ב-XML
        btnAddPost.setOnClickListener(v -> {
            startActivity(new Intent(PostsActivity.this, AddPostsActivity.class));
        });

        // 4. טעינת הנתונים מ-Firestore
        loadPostsFromFirestore();

        // 5. ניווט תחתון (הקוד המקורי שלך)
        setupBottomNavigation();
    }

    private void loadPostsFromFirestore() {
        // שליפת הפוסטים מסודרים לפי זמן (החדש ביותר למעלה)
        db.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        postList.clear();
                        postList.addAll(value.toObjects(Post.class));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_posts);
        bottomNav.setItemIconTintList(null);
        bottomNav.setSelectedItemId(R.id.nav_posts);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_posts) return true;

            if (id == R.id.nav_workouts) startActivity(new Intent(this, WorkoutsActivity.class));
            else if (id == R.id.nav_home) startActivity(new Intent(this, FeedActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            else if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));

            overridePendingTransition(0, 0);
            finish(); // סוגר את האקטיביטי הנוכחי כדי שלא יצטברו בזיכרון
            return true;
        });
    }
}