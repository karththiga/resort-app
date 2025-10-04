package com.example.resortapp.model;

public class User {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String preferredRoomType;

    public User() {} // Firestore needs no-arg

    public User(String id, String fullName, String email, String phone, String role, String preferredRoomType) {
        this.id = id; this.fullName = fullName; this.email = email; this.phone = phone;
        this.role = role; this.preferredRoomType = preferredRoomType;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getPreferredRoomType() { return preferredRoomType; }
}
