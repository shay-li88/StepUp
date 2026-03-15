package com.example.stepup.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stepup.CommentsSheet;
import com.example.stepup.Post;
import com.example.stepup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private Context context;
    private List<Post> postList;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        String currentUserId = FirebaseAuth.getInstance().getUid();

        // 1. הצגת נתונים בסיסיים
        holder.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Anonymous");
        holder.tvPostTitle.setText(post.getTitle() != null ? post.getTitle() : "");
        holder.tvPostContent.setText(post.getContent());
        holder.tvCommentCount.setText(post.getCommentCount() + " Comments");

        // עדכון הזמן מ-"Just now" לזמן אמיתי
        holder.tvPostTime.setText(getTimeAgo(post.getTimestamp()));

        // 2. צביעת רקע הפוסט לפי סוג האימון
        if (post.isHasWorkout() && post.getWorkoutType() != null) {
            String type = post.getWorkoutType().toLowerCase().trim();
            int color;
            if (type.contains("strength")) color = Color.parseColor("#E7C7EB");
            else if (type.contains("pilates")) color = Color.parseColor("#E3F2FD");
            else if (type.contains("cardio")) color = Color.parseColor("#EFB0C3");
            else if (type.contains("running")) color = Color.parseColor("#B3DCB5");
            else color = Color.WHITE;

            holder.cardPost.setCardBackgroundColor(color);
            holder.layoutWorkoutBadge.setVisibility(View.GONE);
        } else {
            holder.cardPost.setCardBackgroundColor(Color.WHITE);
            holder.layoutWorkoutBadge.setVisibility(View.GONE);
        }

        // 3. כפתור מחיקה
        if (post.getUserId() != null && post.getUserId().equals(currentUserId)) {
            holder.btnDeletePost.setVisibility(View.VISIBLE);
            holder.btnDeletePost.setOnClickListener(v -> showDeleteDialog(post.getPostId(), position));
        } else {
            holder.btnDeletePost.setVisibility(View.GONE);
        }

        // 4. לייקים
        int likeCount = post.getLikedBy() != null ? post.getLikedBy().size() : 0;
        holder.tvLikeCount.setText(String.valueOf(likeCount));
        if (post.getLikedBy() != null && post.getLikedBy().contains(currentUserId)) {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_full);
            holder.ivLikeIcon.setColorFilter(Color.RED);
        } else {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_empty);
            holder.ivLikeIcon.clearColorFilter();
        }

        holder.btnLike.setOnClickListener(v -> handleLikeClick(post, currentUserId));

        holder.btnComment.setOnClickListener(v -> {
            if (post.getPostId() != null) {
                CommentsSheet sheet = new CommentsSheet(post.getPostId());
                sheet.show(((AppCompatActivity) context).getSupportFragmentManager(), "comments");
            }
        });
    }

    // פונקציית עזר לחישוב כמה זמן עבר מאז הפרסום
    private String getTimeAgo(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "Just now";

        long time = timestamp.getSeconds() * 1000;
        long now = System.currentTimeMillis();
        long diff = now - time;

        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + "m ago";
        if (diff < 86400000) return (diff / 3600000) + "h ago";

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private void showDeleteDialog(String postId, int position) {
        new AlertDialog.Builder(context)
                .setTitle("מחיקת פוסט")
                .setMessage("בטוח שאת רוצה למחוק את הפוסט הזה?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("Posts").document(postId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "הפוסט נמחק", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void handleLikeClick(Post post, String userId) {
        if (post.getPostId() == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (post.getLikedBy().contains(userId)) {
            db.collection("Posts").document(post.getPostId()).update("likedBy", FieldValue.arrayRemove(userId));
        } else {
            db.collection("Posts").document(post.getPostId()).update("likedBy", FieldValue.arrayUnion(userId));
        }
    }

    @Override public int getItemCount() { return postList.size(); }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvPostTitle, tvPostContent, tvLikeCount, tvCommentCount, tvPostTime;
        ImageView ivLikeIcon;
        LinearLayout btnLike, btnComment, layoutWorkoutBadge;
        CardView cardPost;
        ImageButton btnDeletePost;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvPostUserName);
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvPostTime = itemView.findViewById(R.id.tvPostTime); // חיבור ה-ID של הזמן
            ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            layoutWorkoutBadge = itemView.findViewById(R.id.layoutWorkoutBadge);
            cardPost = itemView.findViewById(R.id.cardPost);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
        }
    }
}