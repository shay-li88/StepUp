package com.example.stepup;

import com.google.firebase.Timestamp;

public class Post {
    private String userId;
    private String userName;
    private String content;
    private String workoutType;
    private String workoutDetails;
    private boolean hasWorkout;
    private Timestamp timestamp;

    // חובה לבנות Constructor ריק עבור Firebase
    public Post() {}

    public Post(String userId, String userName, String content, String workoutType, String workoutDetails, boolean hasWorkout) {
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.workoutType = workoutType;
        this.workoutDetails = workoutDetails;
        this.hasWorkout = hasWorkout;
        this.timestamp = Timestamp.now();
    }

    // Getters ו-Setters (חובה!)
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getContent() { return content; }
    public String getWorkoutType() { return workoutType; }
    public String getWorkoutDetails() { return workoutDetails; }
    public boolean isHasWorkout() { return hasWorkout; }
    public Timestamp getTimestamp() { return timestamp; }
}