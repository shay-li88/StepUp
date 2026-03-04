package com.example.stepup;

public class Workout {
    public String type;
    public String difficulty;
    public int time;
    public String notes;
    public double distance; // שדה חדש
    public long timestamp;

    public Workout() {} // חובה ל-Firestore

    public Workout(String type, String difficulty, int time, String notes, double distance) {
        this.type = type;
        this.difficulty = difficulty;
        this.time = time;
        this.notes = notes;
        this.distance = distance;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getType() { return type; }
    public String getDifficulty() { return difficulty; }
    public int getTime() { return time; }
    public String getNotes() { return notes; }
    public double getDistance() { return distance; }
}