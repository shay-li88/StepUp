package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

// ייבוא קריטי כדי ש-R.id לא יהיה אדום
import com.example.stepup.R;

public class CardioActivity extends AppCompatActivity {

    private Button btnLight, btnModerate, btnHIIT, btnGo;
    private NumberPicker timePicker;
    private String selectedLevel = "Moderate"; // ברירת מחדל כמו בתמונה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // מאפשר תצוגה במסך מלא (אם הפרויקט שלך משתמש בזה)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cardio);

        // טיפול במרווחי מערכת (סטטוס בר למעלה) כדי שהעיצוב לא ייחתך
        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- אתחול וקישור הרכיבים מה-XML ---
        btnLight = findViewById(R.id.btnLight);
        btnModerate = findViewById(R.id.btnModerate);
        btnHIIT = findViewById(R.id.btnHIIT);
        btnGo = findViewById(R.id.btnGoCardio);
        timePicker = findViewById(R.id.cardioTimePicker);

        // הגדרת ה-Picker (גלילה)
        if (timePicker != null) {
            timePicker.setMinValue(5);
            timePicker.setMaxValue(120);
            timePicker.setValue(35);
        }

        // הגדרת לוגיקת הכפתורים
        setupLevelButton(btnLight);
        setupLevelButton(btnModerate);
        setupLevelButton(btnHIIT);

        // הגדרה ראשונית: Moderate נבחר (ורוד כהה עם טקסט לבן)
        resetButtons();
        selectButton(btnModerate);

        // מעבר לדף ה-Workouts ושליחת הנתונים
        if (btnGo != null) {
            btnGo.setOnClickListener(v -> {
                Intent intent = new Intent(CardioActivity.this, WorkoutsActivity.class);
                intent.putExtra("type", "Cardio");
                intent.putExtra("difficulty", selectedLevel);
                intent.putExtra("time", timePicker.getValue());
                startActivity(intent);
            });
        }
    }

    private void setupLevelButton(Button btn) {
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            resetButtons();
            selectButton(btn);
            selectedLevel = btn.getText().toString();
        });
    }

    private void selectButton(Button btn) {
        // צבע ורוד כהה לנבחר (מה-Drawable שיצרנו)
        btn.setBackgroundResource(R.drawable.cardio_btn_selected);
        btn.setTextColor(Color.WHITE);
    }

    private void resetButtons() {
        Button[] btns = {btnLight, btnModerate, btnHIIT};
        for (Button b : btns) {
            if (b != null) {
                // רקע שקוף וטקסט ורוד כהה כשלא נבחר
                b.setBackgroundResource(android.R.color.transparent);
                b.setTextColor(Color.parseColor("#C2185B"));
            }
        }
    }
}