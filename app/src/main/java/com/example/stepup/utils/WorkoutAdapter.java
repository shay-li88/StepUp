package com.example.stepup.utils;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stepup.R;
import com.example.stepup.Workout;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private List<Workout> workoutList; // ודאי שזה השם המדויק כאן

    public WorkoutAdapter(List<Workout> workoutList) {
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

        // שימוש ב-Getters - ודאי שהם קיימים ב-Workout.java
        holder.tvType.setText(workout.getType());
        holder.tvDetails.setText("Difficulty: " + workout.getDifficulty() + " | Time: " + workout.getTime() + " min");
        holder.tvNotes.setText(workout.getNotes());

        String type = workout.getType().toLowerCase();

        // לוגיקת צביעה לפי סוג האימון
        if (type.contains("running")) {
            holder.cardWorkout.setCardBackgroundColor(Color.parseColor("#B3DCB5"));
        } else if (type.contains("pilates")) {
            holder.cardWorkout.setCardBackgroundColor(Color.parseColor("#D0E1F9"));
        } else if (type.contains("strength")) {
            holder.cardWorkout.setCardBackgroundColor(Color.parseColor("#E8DFF5"));
        } else if (type.contains("cardio")) {
            holder.cardWorkout.setCardBackgroundColor(Color.parseColor("#FAD2E1"));
        } else {
            holder.cardWorkout.setCardBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return workoutList != null ? workoutList.size() : 0;
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDetails, tvNotes;
        CardView cardWorkout;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvItemType);
            tvDetails = itemView.findViewById(R.id.tvItemDetails);
            tvNotes = itemView.findViewById(R.id.tvItemNotes);
            cardWorkout = itemView.findViewById(R.id.cardWorkout);
        }
    }
}