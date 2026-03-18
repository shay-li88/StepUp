package com.example.stepup;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity {

    private RecyclerView rvMyPosts;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvMyPosts = findViewById(R.id.rvMyPosts);
        rvMyPosts.setLayoutManager(new LinearLayoutManager(this));

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadMyPosts();
    }

    private void loadMyPosts() {
        // כאן אנחנו מחפשים בתוך אוסף שנקרא "posts" את כל מה שה-authorId שלו הוא המשתמש הנוכחי
        db.collection("posts")
                .whereEqualTo("authorId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        // כאן נכניס את הלוגיקה של ה-Adapter (הצגת הפוסטים)
                        // בינתיים זה יציג רשימה ריקה עד שנוסיף פוסטים ל-DB
                    }
                });
    }
}