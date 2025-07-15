package com.example.sukarelawanmy.model;

import com.google.firebase.Timestamp;

import java.util.Date;

public class UserParticipation {
    private UserModel user;
    private int participationCount;
    private Timestamp lastParticipationDate;
    // Getters and setters
    public UserModel getUser() { return user; }
    public void setUser(UserModel user) { this.user = user; }
    public int getParticipationCount() { return participationCount; }
    public void setParticipationCount(int participationCount) {
        this.participationCount = participationCount;
    }

    public Timestamp getLastParticipationDate() {
        return lastParticipationDate;
    }

    public void setLastParticipationDate(Timestamp lastParticipationDate) {
        this.lastParticipationDate = lastParticipationDate;
    }
}