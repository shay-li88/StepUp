package com.example.stepup;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // משתמשים ב-layout פשוט של אנדרואיד או אחד שיצרת
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.text1.setText(comment.getUserName()); // השם בבולד
        holder.text1.setTypeface(null, android.graphics.Typeface.BOLD);
        holder.text2.setText(comment.getCommentText()); // תוכן התגובה
    }

    @Override
    public int getItemCount() { return commentList.size(); }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
