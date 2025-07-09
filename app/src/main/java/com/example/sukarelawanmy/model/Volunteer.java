package com.example.sukarelawanmy.model;

public class Volunteer {
    private String id;
    private String name;
    private String email;
    private String appliedEvent;
    private String status = "pending";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAppliedEvent() {
        return appliedEvent;
    }

    public void setAppliedEvent(String appliedEvent) {
        this.appliedEvent = appliedEvent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}