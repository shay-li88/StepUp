package com.example.stepup;

public class Comment {
    private String userName;
    private String commentText;

    // חובה Constructor ריק עבור Firebase
    public Comment() {}

    public Comment(String userName, String commentText) {
        this.userName = userName;
        this.commentText = commentText;
    }

    public String getUserName() { return userName; }
    public String getCommentText() { return commentText; }
}