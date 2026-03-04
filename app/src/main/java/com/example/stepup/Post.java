package com.example.stepup;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Post {
    private String userName;
    private String content;
    private String userId;

    // שדות עבור האימון המצורף (כדי שנוכל לשתף אימון בתוך פוסט)
    private boolean hasWorkout;
    private String workoutType;
    private String workoutDetails;

    @ServerTimestamp
    private Date timestamp; // זמן יצירת הפוסט

    // חובה: בנאי ריק עבור Firebase
    public Post() {}

    // בנאי ליצירת פוסט חדש
    public Post(String userId, String userName, String content, String workoutType, String workoutDetails) {
        this.userId = userId;
        this.userName = userName;
        this.content = content;

        if (workoutType != null && !workoutType.isEmpty()) {
            this.hasWorkout = true;
            this.workoutType = workoutType;
            this.workoutDetails = workoutDetails;
        } else {
            this.hasWorkout = false;
        }
    }

    // Getters ו-Setters (חשובים מאוד כדי ש-Firestore יצליח לקרוא את הנתונים)
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isHasWorkout() { return hasWorkout; }
    public void setHasWorkout(boolean hasWorkout) { this.hasWorkout = hasWorkout; }

    public String getWorkoutType() { return workoutType; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

    public String getWorkoutDetails() { return workoutDetails; }
    public void setWorkoutDetails(String workoutDetails) { this.workoutDetails = workoutDetails; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}