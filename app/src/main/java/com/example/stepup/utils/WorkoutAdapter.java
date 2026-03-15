package com.example.stepup.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stepup.AddPostsActivity;
import com.example.stepup.R;
import com.example.stepup.Workout;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private List<Workout> workoutList;
    private Context context; // הוספנו Context בשביל ה-Intent

    public WorkoutAdapter(Context context, List<Workout> workoutList) {
        this.context = context;
        this.workoutList = workoutList;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout workout = workoutList.get(position);

        String type = workout.getType() != null ? workout.getType() : "Unknown";
        String typeLower = type.toLowerCase().trim();

        holder.tvType.setText(type);
        holder.tvDetails.setText("Difficulty: " + workout.getDifficulty() + " | Time: " + workout.getTime() + " min");
        holder.tvNotes.setText(workout.getNotes());

        if (typeLower.contains("running")) {
            holder.tvDistance.setVisibility(View.VISIBLE);
            holder.tvDistance.setText("Distance: " + workout.getDistance() + " km");
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // הגדרת צבעים
        int cardColor, textColor;
        if (typeLower.contains("strength")) {
            cardColor = Color.parseColor("#E7C7EB"); textColor = Color.parseColor("#4A148C");
        } else if (typeLower.contains("pilates")) {
            cardColor = Color.parseColor("#E3F2FD"); textColor = Color.parseColor("#1A4375");
        } else if (typeLower.contains("cardio")) {
            cardColor = Color.parseColor("#EFB0C3"); textColor = Color.parseColor("#C2185B");
        } else if (typeLower.contains("running")) {
            cardColor = Color.parseColor("#B3DCB5"); textColor = Color.parseColor("#2D6A4F");
        } else {
            cardColor = Color.WHITE; textColor = Color.BLACK;
        }

        holder.cardWorkout.setCardBackgroundColor(cardColor);
        holder.tvType.setTextColor(textColor);
        holder.tvDetails.setTextColor(textColor);
        holder.tvNotes.setTextColor(textColor);
        holder.tvDistance.setTextColor(textColor);
        holder.btnShareWorkout.setColorFilter(Color.parseColor("#444444")); // צביעת החץ בערך שביקשת

        // --- לוגיקת כפתור השיתוף ---
        holder.btnShareWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddPostsActivity.class);

            String sharedTitle = "My " + type + " Workout!";
            String sharedContent = "Just finished a " + workout.getTime() + " min " + type + " session. Feeling great! #StepUp";

            intent.putExtra("isShared", true);
            intent.putExtra("sharedTitle", sharedTitle);
            intent.putExtra("sharedContent", sharedContent);

            // העברת נתוני האימון לטובת ה-Badge בפוסט
            intent.putExtra("workoutType", type);
            intent.putExtra("workoutDetails", workout.getTime() + " min • " + workout.getDifficulty());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return workoutList != null ? workoutList.size() : 0;
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDetails, tvNotes, tvDistance;
        CardView cardWorkout;
        ImageButton btnShareWorkout; // הכפתור החדש

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvItemType);
            tvDetails = itemView.findViewById(R.id.tvItemDetails);
            tvNotes = itemView.findViewById(R.id.tvItemNotes);
            tvDistance = itemView.findViewById(R.id.tvItemDistance);
            cardWorkout = itemView.findViewById(R.id.cardWorkout);
            btnShareWorkout = itemView.findViewById(R.id.btnShareWorkout); // וודאי שזה ה-ID ב-XML
        }
    }
}