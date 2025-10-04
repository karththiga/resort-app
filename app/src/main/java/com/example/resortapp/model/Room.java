package com.example.resortapp.model;

public class Room {
    private String id;
    private String name;
    private String type;
    private String description;
    private Double basePrice;
    private String imageUrl;
    private String status;
    private Integer capacity;

    public Room() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public Double getBasePrice() { return basePrice; }
    public String getImageUrl() { return imageUrl; }
    public String getStatus() { return status; }
    public Integer getCapacity() { return capacity; }
}

