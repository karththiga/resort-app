package com.example.resortapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.resortapp.model.Room;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RoomDetailActivity extends AppCompatActivity {

    private ImageView img;
    private TextView tvName, tvPrice, tvMeta, tvDesc, tvDates;
    private Button btnPickDates, btnBook;

    private Room room;
    private Long startUtc = null, endUtc = null; // UTC millis

    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        if (bar != null) {
            setSupportActionBar(bar);
            bar.setNavigationOnClickListener(v -> onBackPressed());
        }

        img = findViewById(R.id.img);
        tvName = findViewById(R.id.tvName);
        tvPrice = findViewById(R.id.tvPrice);
        tvMeta = findViewById(R.id.tvMeta);
        tvDesc = findViewById(R.id.tvDesc);
        tvDates = findViewById(R.id.tvDates);
        btnPickDates = findViewById(R.id.btnPickDates);
        btnBook = findViewById(R.id.btnBook);

        String roomId = getIntent().getStringExtra("roomId");
        if (roomId == null) { finish(); return; }

        FirebaseFirestore.getInstance().collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(snap -> {
                    room = snap.toObject(Room.class);
                    if (room == null) { finish(); return; }
                    // ensure id field
                    try {
                        java.lang.reflect.Field f = Room.class.getDeclaredField("id");
                        f.setAccessible(true); f.set(room, snap.getId());
                    } catch (Exception ignored) {}

                    bindRoom();
                })
                .addOnFailureListener(e -> { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); finish(); });

        btnPickDates.setOnClickListener(v -> pickDates());
        btnBook.setOnClickListener(v -> createBooking());
    }

    private void bindRoom() {
        String displayName = room.getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = room.getType();
        }
        tvName.setText(displayName);

        double price = room.getBasePrice() == null ? 0.0 : room.getBasePrice();
        tvPrice.setText(String.format(Locale.getDefault(), "LKR %,.0f / night", price));

        StringBuilder metaBuilder = new StringBuilder();
        if (room.getType() != null && !room.getType().trim().isEmpty()) {
            metaBuilder.append(room.getType().trim());
        }
        if (room.getCapacity() != null && room.getCapacity() > 0) {
            if (metaBuilder.length() > 0) metaBuilder.append(" • ");
            metaBuilder.append("Sleeps ").append(room.getCapacity());
        }
        if (metaBuilder.length() > 0) {
            tvMeta.setText(metaBuilder.toString());
            tvMeta.setVisibility(View.VISIBLE);
        } else {
            tvMeta.setVisibility(View.GONE);
        }

        String description = room.getDescription();
        tvDesc.setText(description != null && !description.trim().isEmpty()
                ? description
                : getString(R.string.room_detail_no_description));

        Glide.with(this).load(room.getImageUrl()).placeholder(R.drawable.placeholder_room).into(img);
    }

    private void pickDates() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select dates");
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> dp = builder.build();
        dp.addOnPositiveButtonClickListener(sel -> {
            if (sel == null) return;
            startUtc = sel.first;
            endUtc   = sel.second;
            tvDates.setText(fmt.format(new Date(startUtc)) + " → " + fmt.format(new Date(endUtc)));
        });
        dp.show(getSupportFragmentManager(), "range");
    }

    private void createBooking() {
        if (room == null) return;
        if (startUtc == null || endUtc == null) {
            Toast.makeText(this, "Please select dates", Toast.LENGTH_SHORT).show(); return;
        }

        long nights = TimeUnit.MILLISECONDS.toDays(endUtc - startUtc);
        if (nights <= 0) {
            Toast.makeText(this, "Invalid date range", Toast.LENGTH_SHORT).show(); return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show(); return; }

        double price = room.getBasePrice() == null ? 0.0 : room.getBasePrice();
        double total = nights * price;

        Map<String, Object> b = new HashMap<>();
        b.put("kind", "ROOM");
        b.put("userId", uid);
        b.put("roomId", room.getId());
        b.put("roomName", room.getName() != null ? room.getName() : room.getType());
        b.put("roomImageUrl", room.getImageUrl());
        b.put("priceAtBooking", price);
        b.put("checkIn", new Timestamp(new Date(startUtc)));
        b.put("checkOut", new Timestamp(new Date(endUtc)));
        b.put("nights", nights);
        b.put("totalAmount", total);
        b.put("status", "CONFIRMED");
        b.put("createdAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("bookings").add(b)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Booked! See in My Bookings.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
