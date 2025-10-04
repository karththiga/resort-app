package com.example.resortapp;

import com.example.resortapp.model.Room;
import com.google.firebase.firestore.DocumentSnapshot;

public final class FirestoreMappers {
    private FirestoreMappers(){}
    public static Room toRoom(DocumentSnapshot d){
        Room r = d.toObject(Room.class);
        if (r == null) r = new Room();
        // ensure id is populated for stableIds/DiffUtil
        try {
            java.lang.reflect.Field f = Room.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, d.getId());
        } catch (Exception ignored) {}
        return r;
    }
}

