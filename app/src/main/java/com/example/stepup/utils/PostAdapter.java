package com.example.stepup.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stepup.Post;
import com.example.stepup.R;
import java.util.List;

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

        // הגדרת נתוני טקסט בסיסיים
        holder.tvUserName.setText(post.getUserName());
        holder.tvPostContent.setText(post.getContent());

        // טיפול בזמן (אופציונלי - אם הוספת שדה זמן ב-Post.java)
        if (post.getTimestamp() != null) {
            holder.tvPostTime.setText("Just now");
        }

        // טיפול בתג האימון (Workout Badge) -
        if (post.isHasWorkout()) {
            holder.layoutWorkoutBadge.setVisibility(View.VISIBLE);
            holder.tvWorkoutSummary.setText(post.getWorkoutType() + " • " + post.getWorkoutDetails());

            // התאמת צבעים לפי סוג האימון בדומה לעיצוב המבוקש
            updateBadgeStyle(holder, post.getWorkoutType());
        } else {
            holder.layoutWorkoutBadge.setVisibility(View.GONE);
        }

        // לוגיקה לכפתור לייק
        holder.btnLike.setOnClickListener(v -> {
            // שינוי הלב לאדום (במציאות נרצה לשמור זאת ב-Firebase)
            holder.ivLikeIcon.setImageResource(android.R.drawable.btn_star_big_on);
            holder.ivLikeIcon.setColorFilter(Color.RED);
            int currentLikes = Integer.parseInt(holder.tvLikeCount.getText().toString());
            holder.tvLikeCount.setText(String.valueOf(currentLikes + 1));
        });

        // לוגיקה לכפתור תגובה
        holder.btnComment.setOnClickListener(v -> {
            // כאן תוכלי לפתוח דף תגובות בעתיד
        });
    }

    private void updateBadgeStyle(PostViewHolder holder, String type) {
        if (type == null) return;

        // התאמת צבעים לפי הקטגוריות מהמסך Add Post
        switch (type) {
            case "Running":
                holder.layoutWorkoutBadge.getBackground().setTint(Color.parseColor("#D1E9F6"));
                holder.tvWorkoutSummary.setTextColor(Color.parseColor("#2D6A4F"));
                break;
            case "Strength":
                holder.layoutWorkoutBadge.getBackground().setTint(Color.parseColor("#EADCF7"));
                holder.tvWorkoutSummary.setTextColor(Color.parseColor("#5E548E"));
                break;
            case "Cardio":
                holder.layoutWorkoutBadge.getBackground().setTint(Color.parseColor("#FAD2E1"));
                holder.tvWorkoutSummary.setTextColor(Color.parseColor("#C2185B"));
                break;
            case "Pilates":
                holder.layoutWorkoutBadge.getBackground().setTint(Color.parseColor("#D0E1F9"));
                holder.tvWorkoutSummary.setTextColor(Color.parseColor("#1A4375"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvPostContent, tvPostTime, tvWorkoutSummary, tvLikeCount, tvCommentCount;
        ImageView ivPostUserProfile, ivPostImage, ivLikeIcon, ivWorkoutIcon;
        LinearLayout layoutWorkoutBadge, btnLike, btnComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvPostUserName);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvWorkoutSummary = itemView.findViewById(R.id.tvWorkoutSummary);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            ivPostUserProfile = itemView.findViewById(R.id.ivPostUserProfile);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
            ivWorkoutIcon = itemView.findViewById(R.id.ivWorkoutIcon);
            layoutWorkoutBadge = itemView.findViewById(R.id.layoutWorkoutBadge);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }
}