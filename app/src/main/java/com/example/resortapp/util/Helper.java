package com.example.resortapp.util;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Helper {

    // ---------------- Rooms ----------------
    public void seedRoomsIfNeeded() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        CollectionReference rooms = db.collection("rooms");

        // ECO PODS
        addRoom(batch, rooms, room("Eco Pod A", "Eco-Pod",
                "Compact pod built with recycled materials; skylight & solar power.",
                12000, 2, "eco_pod",
                "https://images.unsplash.com/photo-1505691723518-36a5ac3b2d53"));
        addRoom(batch, rooms, room("Eco Pod B", "Eco-Pod",
                "Minimalist pod near herb garden; great stargazing.",
                12500, 2, "eco_pod",
                "https://images.unsplash.com/photo-1519710164239-da123dc03ef4"));
        addRoom(batch, rooms, room("Eco Pod C", "Eco-Pod",
                "Cozy pod with bamboo interior, low‑energy AC.",
                11800, 2, "eco_pod",
                "https://images.unsplash.com/photo-1501183638710-841dd1904471"));

        // MOUNTAIN CABINS
        addRoom(batch, rooms, room("Mountain Cabin 1", "Cabin",
                "Rustic cabin with fireplace and panoramic mountain view.",
                26000, 3, "mountain_cabin",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267"));
        addRoom(batch, rooms, room("Mountain Cabin 2", "Cabin",
                "Two‑bedroom timber cabin; balcony & hammock.",
                28500, 4, "mountain_cabin",
                "https://images.unsplash.com/photo-1505691938895-1758d7feb511"));
        addRoom(batch, rooms, room("Deluxe Mountain Suite", "Cabin",
                "Spacious suite, kitchenette, ridge‑line view.",
                32000, 5, "mountain_cabin",
                "https://images.unsplash.com/photo-1496412705862-e0088f16f791"));

        // RIVER HUTS
        addRoom(batch, rooms, room("River Hut A", "Hut",
                "Thatched hut by the river; mosquito nets; solar lamps.",
                18000, 2, "river_hut",
                "https://images.unsplash.com/photo-1505692794403-34d4982f88aa"));
        addRoom(batch, rooms, room("River Hut B", "Hut",
                "Open‑air veranda, reed walls, gentle river breeze.",
                19500, 3, "river_hut",
                "https://images.unsplash.com/photo-1484154218962-a197022b5858"));
        addRoom(batch, rooms, room("Family River Hut", "Hut",
                "Family‑sized hut with two rooms and eco‑fans.",
                21000, 4, "river_hut",
                "https://images.unsplash.com/photo-1499696010180-025ef6e1a8f6"));

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d("SEED", "Rooms seeded");
                })
                .addOnFailureListener(e -> Log.e("SEED", "Rooms seeding failed", e));
    }

    // ---------------- Activities ----------------
    public void seedActivitiesIfNeeded() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        CollectionReference acts = db.collection("activities");

        addActivity(batch, acts, activity("Guided Hike",
                "Sunrise hike with ranger; 2–3 hrs. Includes snacks.", 7500, 12,
                "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee"));
        addActivity(batch, acts, activity("Eco‑Tour",
                "Electric buggy tour around the resort & wetlands.", 6500, 8,
                "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429"));
        addActivity(batch, acts, activity("Kayaking",
                "Calm‑water kayaking on the river; equipment included.", 9000, 10,
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e"));
        addActivity(batch, acts, activity("Bird Watching",
                "Early‑morning session with binoculars and guide.", 5500, 10,
                "https://images.unsplash.com/photo-1506629082955-511b1aa562c8"));

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d("SEED", "Activities seeded");

                })
                .addOnFailureListener(e -> Log.e("SEED", "Activities seeding failed", e));
    }

    // ---------------- Promos ----------------
    public void seedPromosIfNeeded() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        CollectionReference promos = db.collection("promos");

        addPromo(batch, promos, promo(
                "Stay Longer & Save",
                "Book three nights and enjoy a complimentary sunset kayaking session for two.",
                addDays(new Date(), 30)));

        addPromo(batch, promos, promo(
                "Midweek Wellness Treat",
                "Reserve a weekday stay to receive a free 30-minute spa massage upgrade.",
                addDays(new Date(), 21)));

        addPromo(batch, promos, promo(
                "Family Adventure Bundle",
                "Combine a riverside hut with our guided nature trail and save 15% on activities.",
                addDays(new Date(), 45)));

        batch.commit()
                .addOnSuccessListener(unused -> Log.d("SEED", "Promos seeded"))
                .addOnFailureListener(e -> Log.e("SEED", "Promos seeding failed", e));
    }

    public Map<String, Object> room(String name, String type, String desc,
                                     int basePriceLKR, int capacity,
                                     String category, String imageUrl) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("type", type);
        m.put("description", desc);
        m.put("basePrice", basePriceLKR);
        m.put("capacity", capacity);
        m.put("imageUrl", imageUrl);
        m.put("status", "ACTIVE");
        m.put("isBookable", true);
        m.put("category", category);
        m.put("createdAt", FieldValue.serverTimestamp());
        return m;
    }

    public void seedEcoInfoIfNeeded() {

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        com.google.firebase.firestore.CollectionReference col = db.collection("eco_info");

        addEco(batch, col, eco("Rainwater Harvesting",
                "How we save ~40% fresh water",
                "EcoStay uses rooftop catchments and gravity-fed filtration to offset municipal usage...",
                "https://images.unsplash.com/photo-1502303756785-c6b6a1c7a43f", "practice"));

        addEco(batch, col, eco("Cloudhill Wetlands Reserve",
                "Local nature reserve near EcoStay",
                "A protected marsh ecosystem rich with migratory birds. Guided walks every weekend.",
                "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee", "nature_reserve"));

        addEco(batch, col, eco("Solar & Low‑Energy Lighting",
                "Cutting nighttime energy use",
                "Cabins and pathways use solar arrays with motion sensors for minimalist light pollution.",
                "https://images.unsplash.com/photo-1509395176047-4a66953fd231", "initiative"));

        addEco(batch, col, eco("Earth Hour at EcoStay",
                "Join our stargazing hour",
                "One-hour lights-off, storytelling and stargazing at the lawn. Complimentary cocoa.",
                "https://images.unsplash.com/photo-1496307653780-42ee777d4833", "event"));

        batch.commit().addOnSuccessListener(unused -> {

        }).addOnFailureListener(e -> android.util.Log.e("SEED", "Eco info seeding failed", e));
    }

    public void addRoom(WriteBatch batch, CollectionReference col, Map<String, Object> r) {
        DocumentReference doc = col.document();
        r.put("id", doc.getId()); // optional mirror
        batch.set(doc, r);
    }

    public Map<String, Object> activity(String name, String desc,
                                         int pricePerPerson, int capacityPerSession,
                                         String imageUrl) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("description", desc);
        m.put("pricePerPerson", pricePerPerson);
        m.put("capacityPerSession", capacityPerSession);
        m.put("imageUrl", imageUrl);
        m.put("status", "ACTIVE");
        m.put("createdAt", FieldValue.serverTimestamp());
        return m;
    }

    public void addActivity(WriteBatch batch, CollectionReference col, Map<String, Object> a) {
        DocumentReference doc = col.document();
        a.put("id", doc.getId()); // optional mirror
        batch.set(doc, a);
    }

    public Map<String, Object> promo(String title, String message, Date validUntil) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("message", message);
        m.put("createdAt", FieldValue.serverTimestamp());
        if (validUntil != null) {
            m.put("validUntil", validUntil);
        }
        m.put("status", "ACTIVE");
        return m;
    }

    public void addPromo(WriteBatch batch, CollectionReference col, Map<String, Object> promo) {
        DocumentReference doc = col.document();
        promo.put("id", doc.getId());
        batch.set(doc, promo);
    }

    public Map<String,Object> eco(String title, String subtitle, String desc, String imageUrl, String type) {
        Map<String,Object> m = new HashMap<>();
        m.put("title", title);
        m.put("subtitle", subtitle);
        m.put("description", desc);
        m.put("imageUrl", imageUrl);
        m.put("type", type);
        m.put("status", "ACTIVE");
        m.put("createdAt", FieldValue.serverTimestamp());
        return m;
    }
    public void addEco(WriteBatch b, CollectionReference c, Map<String,Object> x) {
        DocumentReference d = c.document();
        x.put("id", d.getId());
        b.set(d, x);
    }

    public static Date addDays(Date base, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(base); c.add(Calendar.DATE, days);
        return c.getTime();
    }
    public static Date clearTime(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
