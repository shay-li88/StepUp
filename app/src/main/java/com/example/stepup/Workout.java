package com.example.stepup;

import com.google.firebase.Timestamp;

public class Workout {
    public String type;
    public String difficulty;
    public int time;
    public String notes;
    public double distance;
    public String userId;    // שדה חובה לסינון
    public Timestamp timestamp; // פורמט Firebase למיון מדויק

    public Workout() {} // חובה ל-Firestore

    public Workout(String type, String difficulty, int time, String notes, double distance) {
        this.type = type;
        this.difficulty = difficulty;
        this.time = time;
        this.notes = notes;
        this.distance = distance;
        // כברירת מחדל נוצר עם הזמן הנוכחי
        this.timestamp = Timestamp.now();
    }

    // Getters & Setters
    public String getType() { return type; }
    public String getDifficulty() { return difficulty; }
    public int getTime() { return time; }
    public String getNotes() { return notes; }
    public double getDistance() { return distance; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}