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

    private List<Workout> workoutList;

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

        // הגנה מפני קריסה אם הנתונים ב-Firestore חסרים
        String type = workout.getType() != null ? workout.getType() : "Unknown";
        String typeLower = type.toLowerCase().trim();

        holder.tvType.setText(type);
        holder.tvDetails.setText("Difficulty: " + workout.getDifficulty() + " | Time: " + workout.getTime() + " min");
        holder.tvNotes.setText(workout.getNotes());

        // לוגיקת הצגת מרחק: רק אם זה Running
        if (typeLower.contains("running")) {
            holder.tvDistance.setVisibility(View.VISIBLE);
            holder.tvDistance.setText("Distance: " + workout.getDistance() + " km");
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // הגדרת צבעים לפי סוג אימון שביקשת
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
    }

    @Override
    public int getItemCount() {
        return workoutList != null ? workoutList.size() : 0;
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDetails, tvNotes, tvDistance; // הוספתי את tvDistance כאן
        CardView cardWorkout;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvItemType);
            tvDetails = itemView.findViewById(R.id.tvItemDetails);
            tvNotes = itemView.findViewById(R.id.tvItemNotes);
            tvDistance = itemView.findViewById(R.id.tvItemDistance); // חיבור ה-ID מה-XML
            cardWorkout = itemView.findViewById(R.id.cardWorkout);
        }
    }
}