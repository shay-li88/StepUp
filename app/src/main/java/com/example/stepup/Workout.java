package com.example.stepup;

public class Workout {
    public String type;
    public String difficulty;
    public int time;
    public String notes;
    public long timestamp;


    public Workout() {}

    public Workout(String type, String difficulty, int time, String notes) {
        this.type = type;
        this.difficulty = difficulty;
        this.time = time;
        this.notes = notes;
        this.timestamp = System.currentTimeMillis();
    }
    public String getType() { return type; }
    public String getDifficulty() { return difficulty; }
    public int getTime() { return time; }
    public String getNotes() { return notes; }

}
