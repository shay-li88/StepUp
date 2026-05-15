package com.example.stepup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsSheet extends BottomSheetDialogFragment {

    private String postId;
    private CommentAdapter adapter;
    private List<Comment> commentList;
    private FirebaseFirestore db;

    public CommentsSheet() {
        // Constructor ריק חובה
    }

    public CommentsSheet(String postId) {
        this.postId = postId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // וודאי שיש לך קובץ layout בשם layout_comments_sheet
        View v = inflater.inflate(R.layout.layout_comments_sheet, container, false);

        db = FirebaseFirestore.getInstance();

        RecyclerView rvComments = v.findViewById(R.id.rvComments);
        EditText etComment = v.findViewById(R.id.etComment);
        ImageButton btnSend = v.findViewById(R.id.btnSendComment);

        commentList = new ArrayList<>();
        adapter = new CommentAdapter(commentList);

        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(adapter);

        loadComments();

        btnSend.setOnClickListener(view -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty()) {
                sendComment(text, etComment);
            }
        });

        return v;
    }

    private void loadComments() {
        db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        commentList.clear();
                        commentList.addAll(value.toObjects(Comment.class));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void sendComment(String text, EditText etComment) {
        String userName = "Anonymous";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (userName == null || userName.isEmpty()) {
                String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                if (email != null) userName = email.split("@")[0];
            }
        }

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("userName", userName);
        commentData.put("commentText", text);
        commentData.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("posts").document(postId).collection("comments")
                .add(commentData)
                .addOnSuccessListener(documentReference -> {
                    etComment.setText("");
                    // עדכון מונה תגובות בפוסט
                    db.collection("posts").document(postId)
                            .update("commentCount", FieldValue.increment(1));
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בשליחת תגובה", Toast.LENGTH_SHORT).show());
    }
}