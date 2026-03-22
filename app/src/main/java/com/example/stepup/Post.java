package com.example.stepup;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private String postId;
    private String userId;
    private String userName;
    private String title; // השדה החדש לכותרת
    private String content;
    private String imageUrl;
    private String workoutType;
    private String workoutDetails;
    private boolean hasWorkout;
    private Timestamp timestamp;
    private List<String> likedBy;
    private int commentCount;

    public Post() {
        this.likedBy = new ArrayList<>();
    }

    // Getters & Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getWorkoutType() { return workoutType; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }
    public String getWorkoutDetails() { return workoutDetails; }
    public void setWorkoutDetails(String workoutDetails) { this.workoutDetails = workoutDetails; }
    public boolean isHasWorkout() { return hasWorkout; }
    public void setHasWorkout(boolean hasWorkout) { this.hasWorkout = hasWorkout; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public List<String> getLikedBy() {
        if (likedBy == null) likedBy = new ArrayList<>();
        return likedBy;
    }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }
}
