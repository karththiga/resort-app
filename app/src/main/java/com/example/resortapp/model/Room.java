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
    private transient Integer availableRooms;
    private transient boolean soldOut;

    public Room() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public Double getBasePrice() { return basePrice; }
    public String getImageUrl() { return imageUrl; }
    public String getStatus() { return status; }
    public Integer getCapacity() { return capacity; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(Integer availableRooms) { this.availableRooms = availableRooms; }

    public boolean isSoldOut() { return soldOut; }
    public void setSoldOut(boolean soldOut) { this.soldOut = soldOut; }

    public Room copy() {
        Room r = new Room();
        r.id = this.id;
        r.name = this.name;
        r.type = this.type;
        r.description = this.description;
        r.basePrice = this.basePrice;
        r.imageUrl = this.imageUrl;
        r.status = this.status;
        r.capacity = this.capacity;
        r.availableRooms = this.availableRooms;
        r.soldOut = this.soldOut;
        return r;
    }
}

