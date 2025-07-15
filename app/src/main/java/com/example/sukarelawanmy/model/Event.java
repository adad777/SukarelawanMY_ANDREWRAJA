package com.example.sukarelawanmy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Event implements Serializable {
    private String eventId;
    private String title;
    private String location;
    private String date;
    private String description;
    private String imageUrl;
    private int maxParticipants;
    private int currentParticipants;
    private boolean requiresApproval;
    private String ngoId;
    private String time;
    private String organizerName;
    private ArrayList<String> requirement;
    private String eventCategory;

    private boolean isJoined;

    public boolean isJoined() {
        return isJoined;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public void setJoined(boolean joined) {
        isJoined = joined;
    }

    public ArrayList<String> getRequirement() {
        return requirement;
    }

    public void setRequirement(ArrayList<String> requirement) {
        this.requirement = requirement;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNgoId() {
        return ngoId;
    }

    public void setNgoId(String ngoId) {
        this.ngoId = ngoId;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;}
// With getters and setters


    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public int getMaxParticipants() {
        return maxParticipants;
    }
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    public int getCurrentParticipants() {
        return currentParticipants;
    }
    public void setCurrentParticipants(int currentParticipants) {
        this.currentParticipants = currentParticipants;}
    public boolean isFull() {
        return maxParticipants > 0 && currentParticipants >= maxParticipants;
    }
}
