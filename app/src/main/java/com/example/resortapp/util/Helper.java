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
        addRoom(batch, rooms, room("Eco Pod Queen Retreat", "Queen Bedroom",
                "Compact pod with queen bed, skylight, and solar-cooled airflow.",
                18500, 2, "eco_pod",
                "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb"));
        addRoom(batch, rooms, room("Eco Pod King Escape", "King Bedroom",
                "Spacious pod featuring king bed, bamboo finishes, and private deck.",
                21500, 2, "eco_pod",
                "https://images.unsplash.com/photo-1568605114967-8130f3a36994"));
        addRoom(batch, rooms, room("Eco Pod Family Haven", "Family Bedroom",
                "Interconnected pods with a queen, twin bunks, and reading nook.",
                25500, 4, "eco_pod",
                "https://images.unsplash.com/photo-1559599788-86c2f8d04d98"));

        // MOUNTAIN CABINS
        addRoom(batch, rooms, room("Mountain Cabin Queen Lookout", "Queen Bedroom",
                "Warm timber cabin with queen bed, fireplace, and terrace view.",
                26500, 2, "mountain_cabin",
                "https://images.unsplash.com/photo-1441974231531-c6227db76b6e"));
        addRoom(batch, rooms, room("Mountain Cabin King Summit", "King Bedroom",
                "King suite boasting vaulted ceilings, soaking tub, and ridge balcony.",
                31500, 3, "mountain_cabin",
                "https://images.unsplash.com/photo-1439130490301-25e322d88054"));
        addRoom(batch, rooms, room("Mountain Cabin Family Lodge", "Family Bedroom",
                "Two-bedroom family lodge with loft bunks and kitchenette.",
                36500, 5, "mountain_cabin",
                "https://images.unsplash.com/photo-1505691938895-1758d7feb511"));

        // RIVER HUTS
        addRoom(batch, rooms, room("River Hut Queen Breeze", "Queen Bedroom",
                "Riverside hut with queen canopy bed and open-air lounge.",
                20500, 2, "river_hut",
                "https://images.unsplash.com/photo-1505691723518-36a5ac3b2d53"));
        addRoom(batch, rooms, room("River Hut King Drift", "King Bedroom",
                "Waterfront hut featuring king bed, daybed, and bamboo shower.",
                23500, 2, "river_hut",
                "https://images.unsplash.com/photo-1470246973918-29a93221c455"));
        addRoom(batch, rooms, room("River Hut Family Cove", "Family Bedroom",
                "Dual-room family hut with queen master, twin bunks, and hammocks.",
                27500, 4, "river_hut",
                "https://images.unsplash.com/photo-1484154218962-a197022b5858"));

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
