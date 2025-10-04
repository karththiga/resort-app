package com.example.resortapp.model;

public class EcoInfo {
    private String id;
    private String title;
    private String subtitle;
    private String description;
    private String imageUrl;
    private String type;   // "initiative" | "nature_reserve" | "practice"
    private String status;

    public EcoInfo() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getType() { return type; }
    public String getStatus() { return status; }
}

