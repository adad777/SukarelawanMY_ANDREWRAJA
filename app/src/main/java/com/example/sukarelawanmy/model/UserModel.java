package com.example.sukarelawanmy.model;

import java.util.Date;

public class UserModel {
    private String uid;
    private String fullName;
    private String email;
    private String role;
    private Date joinDate;
    public UserModel() {}

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public UserModel(String uid, String fullName, String email, String role) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public String getUid() { return uid; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public void setUid(String uid) { this.uid = uid; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
}
