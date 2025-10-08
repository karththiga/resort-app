package com.example.resortapp.util;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper that records user-facing booking notifications inside Firestore so the inbox can read them
 * even when the underlying booking document changes or is removed.
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepo";
    private static final String COLLECTION = "booking_notifications";

    private static final NotificationRepository INSTANCE = new NotificationRepository();

    private final FirebaseFirestore db;

    private NotificationRepository() {
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    public static NotificationRepository getInstance() {
        return INSTANCE;
    }

    public void recordRoomEvent(@NonNull String userId,
                                @NonNull String bookingId,
                                @NonNull String roomId,
                                @Nullable String roomName,
                                @Nullable Timestamp checkIn,
                                @Nullable Timestamp checkOut,
                                @NonNull String status,
                                @NonNull String eventType) {
        Map<String, Object> payload = basePayload(userId, bookingId, "ROOM", status, eventType);
        payload.put("roomId", roomId);
        if (!TextUtils.isEmpty(roomName)) {
            payload.put("roomName", roomName);
        }
        if (checkIn != null) {
            payload.put("checkIn", checkIn);
        }
        if (checkOut != null) {
            payload.put("checkOut", checkOut);
        }
        writeNotification(payload);
    }

    public void recordActivityEvent(@NonNull String userId,
                                    @NonNull String bookingId,
                                    @NonNull String activityId,
                                    @Nullable String activityName,
                                    @Nullable Timestamp scheduleStart,
                                    int participants,
                                    @NonNull String status,
                                    @NonNull String eventType) {
        Map<String, Object> payload = basePayload(userId, bookingId, "ACTIVITY", status, eventType);
        payload.put("activityId", activityId);
        if (!TextUtils.isEmpty(activityName)) {
            payload.put("activityName", activityName);
        }
        if (scheduleStart != null) {
            payload.put("scheduleStart", scheduleStart);
        }
        if (participants > 0) {
            payload.put("participants", participants);
        }
        writeNotification(payload);
    }

    private Map<String, Object> basePayload(@NonNull String userId,
                                            @NonNull String bookingId,
                                            @NonNull String kind,
                                            @NonNull String status,
                                            @NonNull String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("bookingId", bookingId);
        payload.put("kind", kind);
        payload.put("status", status);
        payload.put("eventType", eventType);
        payload.put("createdAt", FieldValue.serverTimestamp());
        return payload;
    }

    private void writeNotification(@NonNull Map<String, Object> payload) {
        db.collection(COLLECTION)
                .add(payload)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to record notification", e));
    }
}
