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

/**
 * אדפטר האחראי להצגת רשימת האימונים של המשתמש בתוך RecyclerView.
 * מציג לכל אימון את הפרטים שלו, צובע אותו לפי הסוג, ומאפשר לשתף אותו כפוסט בפיד.
 */
public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private List<Workout> workoutList;
    private Context context; // משמש אותנו כ"כרטיס כניסה" לפעולות מערכת, כמו מעבר מסכים (StartActivity)

    public WorkoutAdapter(Context context, List<Workout> workoutList) {
        this.context = context;
        this.workoutList = workoutList;
    }

    /**
     * יוצר ומנפח את קובץ ה-XML של פריט האימון הבודד (item_workout)
     */
    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    /**
     * הזרקת הנתונים של אימון ספציפי לתוך רכיבי התצוגה וקביעת המראה שלו
     */
    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout workout = workoutList.get(position);

        // הגנה מפני קריסה אם סוג האימון חזר ריק (null) מהמסד נתונים
        String type = workout.getType() != null ? workout.getType() : "Unknown";
        String typeLower = type.toLowerCase().trim();

        // הזנת נתוני הטקסט הבסיסיים של האימון
        holder.tvType.setText(type);
        holder.tvDetails.setText("Difficulty: " + workout.getDifficulty() + " | Time: " + workout.getTime() + " min");
        holder.tvNotes.setText(workout.getNotes());

        // תנאי מיוחד: שדה המרחק (Distance) יוצג רק אם מדובר באימון ריצה.
        // בכל אימון אחר - השדה מועלם לחלוטין (GONE) כדי לא ליצור רווחים ריקים בעיצוב.
        if (typeLower.contains("running")) {
            holder.tvDistance.setVisibility(View.VISIBLE);
            holder.tvDistance.setText("Distance: " + workout.getDistance() + " km");
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // --- מנגנון התאמת צבעים דינמי ---
        // התאמת צבע הרקע של הכרטיס וצבע הפונטים בצורה אסתטית לפי סוג הספורט שבוצע
        int cardColor, textColor;
        if (typeLower.contains("strength")) {
            cardColor = Color.parseColor("#E7C7EB"); textColor = Color.parseColor("#4A148C"); // גווני סגול
        } else if (typeLower.contains("pilates")) {
            cardColor = Color.parseColor("#E3F2FD"); textColor = Color.parseColor("#1A4375"); // גווני תכלת
        } else if (typeLower.contains("cardio")) {
            cardColor = Color.parseColor("#EFB0C3"); textColor = Color.parseColor("#C2185B"); // גווני ורוד
        } else if (typeLower.contains("running")) {
            cardColor = Color.parseColor("#B3DCB5"); textColor = Color.parseColor("#2D6A4F"); // גווני ירוק
        } else {
            cardColor = Color.WHITE; textColor = Color.BLACK; // ברירת מחדל
        }

        // הגדרת הצבעים בפועל על רכיבי ה-UI
        holder.cardWorkout.setCardBackgroundColor(cardColor);
        holder.tvType.setTextColor(textColor);
        holder.tvDetails.setTextColor(textColor);
        holder.tvNotes.setTextColor(textColor);
        holder.tvDistance.setTextColor(textColor);
        holder.btnShareWorkout.setColorFilter(Color.parseColor("#444444")); // צביעת אייקון השיתוף באפור כהה

        // --- לוגיקת כפתור השיתוף ---
        // מעבר למסך יצירת פוסט חדש (AddPostsActivity) והעברת נתוני האימון הנוכחי "במזוודה" (Intent.putExtra)
        holder.btnShareWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddPostsActivity.class);

            // בניית כותרת ותוכן מוכנים מראש כדי לחסוך למשתמש זמן כתיבה
            String sharedTitle = "My " + type + " Workout!";
            String sharedContent = "Just finished a " + workout.getTime() + " min " + type + " session. Feeling great! #StepUp";

            // הזרקת המידע ל-Intent (מפתח וערך) כדי שמסך היעד ידע לקרוא אותם ולשתול אותם בתיבות הטקסט
            intent.putExtra("isShared", true);
            intent.putExtra("sharedTitle", sharedTitle);
            intent.putExtra("sharedContent", sharedContent);

            // העברת נתוני אימון גולמיים כדי שהפוסט המיוצר ייצבע ויוצג עם תג (Badge) מותאם
            intent.putExtra("workoutType", type);
            intent.putExtra("workoutDetails", workout.getTime() + " min • " + workout.getDifficulty());

            context.startActivity(intent); // ביצוע המעבר בפועל
        });
    }

    @Override
    public int getItemCount() {
        return workoutList != null ? workoutList.size() : 0;
    }

    /**
     * מחלקת עזר שמחזיקה את השלד של ה-XML ומקשרת את הרכיבים פעם אחת לזיכרון
     */
    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDetails, tvNotes, tvDistance;
        CardView cardWorkout;
        ImageButton btnShareWorkout;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvItemType);
            tvDetails = itemView.findViewById(R.id.tvItemDetails);
            tvNotes = itemView.findViewById(R.id.tvItemNotes);
            tvDistance = itemView.findViewById(R.id.tvItemDistance);
            cardWorkout = itemView.findViewById(R.id.cardWorkout);
            btnShareWorkout = itemView.findViewById(R.id.btnShareWorkout);
        }
    }
}