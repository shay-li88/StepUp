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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
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

/**
 * אדפטר האחראי לקחת את רשימת הפוסטים (postList) ולהציג אותם בתוך ה-RecyclerView.
 * הוא מחבר בין הנתונים הגולמיים מה-Database לבין רכיבי העיצוב (XML) של כל פוסט בנפרד.
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private Context context;
    private List<Post> postList;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    /**
     * יוצר את ה-ViewHolder - מנפח (Inflate) את קובץ ה-XML של פוסט בודד (item_post).
     */
    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    /**
     * הפונקציה המרכזית שמזריקה את הנתונים של פוסט ספציפי לתוך רכיבי התצוגה.
     */
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        String currentUserId = FirebaseAuth.getInstance().getUid(); // המזהה של המשתמש שמביט במסך כרגע
        String authorId = post.getUserId(); // המזהה של המשתמש שכתב את הפוסט

        // 1. נתונים בסיסיים
        // מילוי טקסטים קבועים של הפוסט: שם כותב, כותרת, תוכן, ומתי הוא פורסם
        holder.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Anonymous");
        holder.tvPostTitle.setText(post.getTitle() != null ? post.getTitle() : "");
        holder.tvPostContent.setText(post.getContent());
        holder.tvPostTime.setText(getTimeAgo(post.getTimestamp()));

        // 2. טעינת תמונת פרופיל (דינמית מהמשתמש)
        // מאזין (SnapshotListener) לפרופיל של כותב הפוסט. אם הוא ישנה את תמונת הפרופיל שלו,
        // השינוי ישתקף מיד ובזמן אמת בכל הפוסטים שהוא אי פעם פרסם.
        if (authorId != null) {
            FirebaseFirestore.getInstance().collection("users").document(authorId)
                    .addSnapshotListener((doc, e) -> {
                        if (doc != null && doc.exists()) {
                            String imageUrl = doc.getString("profileImageUrl");
                            Long lastUpdate = doc.getLong("lastImageUpdate");
                            if (lastUpdate == null) lastUpdate = 0L;

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(context)
                                        .load(imageUrl)
                                        .signature(new ObjectKey(lastUpdate)) // מונע מגלייד להציג תמונה ישנה מהקאש
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_user)
                                        .into(holder.ivUserProfile);
                            } else {
                                holder.ivUserProfile.setImageResource(R.drawable.ic_user);
                            }
                        }
                    });
        }

        // 3. הצגת תמונת הפוסט (אם הועלתה כזו)
        // אם המשתמש צירף תמונה לפוסט, נציג את ה-CardView שלה ונטען אותה. אם לא - נעלים אותו (GONE) כדי שלא יתפוס מקום ריק.
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.cardPostImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .into(holder.ivPostImage);
        } else {
            holder.cardPostImage.setVisibility(View.GONE);
        }

        // 4. ספירת תגובות בלייב
        // מאזין לתת-אוסף (Collection) של התגובות בתוך הפוסט הספציפי.
        // ברגע שמישהו מוסיף תגובה, המספר על הפוסט מתעדכן אוטומטית בלי צורך ברענון.
        if (post.getPostId() != null) {
            FirebaseFirestore.getInstance().collection("posts")
                    .document(post.getPostId())
                    .collection("comments")
                    .addSnapshotListener((value, error) -> {
                        if (value != null) {
                            holder.tvCommentCount.setText(value.size() + " Comments");
                        }
                    });
        }

        // 5. צביעת רקע לפי סוג אימון
        // קריאה לפונקציה פנימית שמשנה את צבע כרטיס הפוסט בהתאם לסוג האימון המצורף (למשל: ירוק לריצה, סגול לכוח)
        updateCardBackground(holder, post);

        // 6. מחיקה (רק לבעל הפוסט)
        // בדיקת אבטחה: כפתור המחיקה יוצג (VISIBLE) רק אם ה-ID של המשתמש המחובר שווה ל-ID של כותב הפוסט.
        if (authorId != null && authorId.equals(currentUserId)) {
            holder.btnDeletePost.setVisibility(View.VISIBLE);
            holder.btnDeletePost.setOnClickListener(v -> showDeleteDialog(post.getPostId(), position));
        } else {
            holder.btnDeletePost.setVisibility(View.GONE);
        }

        // 7. לייקים
        // הפעלת הלוגיקה שמנהלת את מערכת הלייקים (סימון לב מלא/ריק ועדכון בבסיס הנתונים)
        setupLikeLogic(holder, post, currentUserId);

        // 8. פתיחת תגובות
        // לחיצה על כפתור התגובות מפתחת את ה-BottomSheetDialogFragment (גיליון תגובות תחתון) ומעבירה לו את ה-ID של הפוסט
        holder.btnComment.setOnClickListener(v -> {
            if (post.getPostId() != null) {
                CommentsSheet sheet = new CommentsSheet(post.getPostId());
                sheet.show(((AppCompatActivity) context).getSupportFragmentManager(), "comments");
            }
        });
    }

    /**
     * פונקציה המשנה את צבע הרקע של הפוסט דינמית לפי סוג האימון שבוצע.
     */
    private void updateCardBackground(PostViewHolder holder, Post post) {
        if (post.isHasWorkout() && post.getWorkoutType() != null) {
            String type = post.getWorkoutType().toLowerCase().trim();
            int color;
            if (type.contains("strength")) color = Color.parseColor("#E7C7EB"); // סגול בהיר
            else if (type.contains("pilates")) color = Color.parseColor("#E3F2FD"); // תכלת
            else if (type.contains("cardio")) color = Color.parseColor("#EFB0C3"); // ורוד/אדמדם
            else if (type.contains("running")) color = Color.parseColor("#B3DCB5"); // ירוק בהיר
            else color = Color.WHITE;
            holder.cardPost.setCardBackgroundColor(color);
        } else {
            holder.cardPost.setCardBackgroundColor(Color.WHITE); // פוסט רגיל ללא אימון יהיה לבן
        }
    }

    /**
     * מנהלת את הלחיצה על כפתור הלייק ומעדכנת את רשימת המשתמשים שעשו לייק (likedBy) ב-Firestore.
     */
    private void setupLikeLogic(PostViewHolder holder, Post post, String currentUserId) {
        List<String> likedBy = post.getLikedBy();
        // בדיקה האם ה-ID שלי כבר נמצא בתוך רשימת הלייקים של הפוסט הזה
        boolean isLiked = likedBy != null && likedBy.contains(currentUserId);
        updateLikeUI(holder, isLiked, likedBy != null ? likedBy.size() : 0);

        holder.btnLike.setOnClickListener(v -> {
            if (post.getPostId() == null) return;

            if (post.getLikedBy().contains(currentUserId)) {
                // אטומי ומקומי: הסרת הלייק מהרשימה המקומית ועדכון הממשק מיד (ללא השהייה)
                post.getLikedBy().remove(currentUserId);
                updateLikeUI(holder, false, post.getLikedBy().size());
                // עדכון השרת: מחיקת ה-ID מהמערך ב-Firestore באמצעות arrayRemove
                FirebaseFirestore.getInstance().collection("posts").document(post.getPostId())
                        .update("likedBy", FieldValue.arrayRemove(currentUserId));
            } else {
                // אטומי ומקומי: הוספת לייק מקומית ועדכון הממשק
                post.getLikedBy().add(currentUserId);
                updateLikeUI(holder, true, post.getLikedBy().size());
                // עדכון השרת: הוספת ה-ID למערך ב-Firestore באמצעות arrayUnion (מונע כפילויות)
                FirebaseFirestore.getInstance().collection("posts").document(post.getPostId())
                        .update("likedBy", FieldValue.arrayUnion(currentUserId));
            }
        });
    }

    /**
     * מעדכנת קוסמטית את נראות הלב (מלא באדום או ריק בכחול) ואת כמות הלייקים.
     */
    private void updateLikeUI(PostViewHolder holder, boolean isLiked, int count) {
        holder.tvLikeCount.setText(String.valueOf(count));
        if (isLiked) {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_full);
            holder.ivLikeIcon.setColorFilter(Color.RED);
        } else {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_empty);
            holder.ivLikeIcon.setColorFilter(Color.parseColor("#03124B"));
        }
    }

    /**
     * פונקציה מתמטית פשוטה ההופכת חותמת זמן (Timestamp) לטקסט ידידותי (למשל: לפני 5 דקות, לפני שעה).
     */
    private String getTimeAgo(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "Just now";
        long time = timestamp.getSeconds() * 1000;
        long diff = System.currentTimeMillis() - time; // חישוב ההפרש במיליסנדות בין עכשיו לזמן הפוסט
        if (diff < 60000) return "Just now"; // פחות מדקה
        if (diff < 3600000) return (diff / 60000) + "m ago"; // פחות משעה (מציג דקות)
        if (diff < 86400000) return (diff / 3600000) + "h ago"; // פחות מיממה (מציג שעות)
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(time)); // מעבר ליום קלנדרי רגיל
    }

    /**
     * מציגה דיאלוג אזהרה (AlertDialog) לפני מחיקת פוסט, ואם אושר - מוחקת את המסמך מ-Firestore.
     */
    private void showDeleteDialog(String postId, int position) {
        new AlertDialog.Builder(context).setTitle("מחיקת פוסט").setMessage("בטוח שאת רוצה למחוק?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("posts").document(postId).delete();
                }).setNegativeButton("ביטול", null).show();
    }

    @Override public int getItemCount() { return postList.size(); }

    /**
     * מחלקת עזר פנימית המחזיקה ומקשרת את רכיבי ה-XML (מציאתם לפי ה-ID שלהם) לשימוש חוזר ויעיל בזיכרון.
     */
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvPostTitle, tvPostContent, tvLikeCount, tvCommentCount, tvPostTime;
        ImageView ivLikeIcon, ivUserProfile, ivPostImage;
        LinearLayout btnLike, btnComment;
        CardView cardPost, cardPostImage;
        ImageButton btnDeletePost;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvPostUserName);
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
            ivUserProfile = itemView.findViewById(R.id.ivPostUserProfile);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            cardPostImage = itemView.findViewById(R.id.cardPostImage);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            cardPost = itemView.findViewById(R.id.cardPost);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
        }
    }
}