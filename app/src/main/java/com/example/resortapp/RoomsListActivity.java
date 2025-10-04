package com.example.resortapp;


import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resortapp.model.Room;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class RoomsListActivity extends AppCompatActivity {

    private RoomListAdapter adapter;
    private ListenerRegistration roomsReg;
    private ListenerRegistration bookingsReg;
    private ListenerRegistration userReg;
    private List<Room> latestRooms = new ArrayList<>();
    private Set<String> unavailableRoomIds = new HashSet<>();
    private Long travelStartUtc, travelEndUtc;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms_list);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        setSupportActionBar(bar);
        bar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String category = getIntent().getStringExtra("category");
        TextView title = findViewById(R.id.tvTitle);
        title.setText(human(category) + " — All Rooms");

        RecyclerView rv = findViewById(R.id.rvAllRooms);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoomListAdapter();
        rv.setAdapter(adapter);

        Query q = FirebaseFirestore.getInstance().collection("rooms")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("category", category)
                .orderBy("basePrice");

        roomsReg = q.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) return;
            List<Room> list = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) list.add(FirestoreMappers.toRoom(d));
            latestRooms = list;
            updateRoomsAdapter();
        });

        adapter.setOnRoomClick(room -> {
            Intent i = new Intent(RoomsListActivity.this, RoomDetailActivity.class);
            i.putExtra("roomId", room.getId());
            startActivity(i);
        });

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            userReg = FirebaseFirestore.getInstance().collection("users").document(uid)
                    .addSnapshotListener(this, (snap, e) -> {
                        if (e != null || snap == null) return;
                        Timestamp start = snap.getTimestamp("travelStart");
                        Timestamp end = snap.getTimestamp("travelEnd");
                        travelStartUtc = start != null ? start.toDate().getTime() : null;
                        travelEndUtc = end != null ? end.toDate().getTime() : null;
                        subscribeBookings();
                        updateRoomsAdapter();
                    });
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (roomsReg != null) { roomsReg.remove(); roomsReg = null; }
        if (bookingsReg != null) { bookingsReg.remove(); bookingsReg = null; }
        if (userReg != null) { userReg.remove(); userReg = null; }
    }

    private void subscribeBookings() {
        if (bookingsReg != null) { bookingsReg.remove(); bookingsReg = null; }
        if (travelStartUtc == null || travelEndUtc == null) {
            unavailableRoomIds.clear();
            updateRoomsAdapter();
            return;
        }

        Query q = FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("status", "CONFIRMED")
                .whereLessThan("checkIn", new Timestamp(new Date(travelEndUtc)));

        bookingsReg = q.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) return;
            Set<String> busy = new HashSet<>();
            for (DocumentSnapshot d : qs.getDocuments()) {
                String kind = d.getString("kind");
                if (kind != null && !"ROOM".equals(kind)) continue;
                String roomId = d.getString("roomId");
                Timestamp checkIn = d.getTimestamp("checkIn");
                Timestamp checkOut = d.getTimestamp("checkOut");
                if (roomId == null || checkIn == null || checkOut == null) continue;
                long ci = checkIn.toDate().getTime();
                long co = checkOut.toDate().getTime();
                if (ci < travelEndUtc && co > travelStartUtc) {
                    busy.add(roomId);
                }
            }
            unavailableRoomIds = busy;
            updateRoomsAdapter();
        });
    }

    private void updateRoomsAdapter() {
        if (latestRooms == null) return;
        List<Room> filtered = new ArrayList<>();
        for (Room r : latestRooms) {
            if (travelStartUtc != null && travelEndUtc != null && unavailableRoomIds.contains(r.getId())) {
                continue;
            }
            filtered.add(r);
        }
        adapter.submit(filtered);
    }

    private String human(String code){
        if ("eco_pod".equals(code)) return "Eco‑pods";
        if ("mountain_cabin".equals(code)) return "Mountain‑view cabins";
        if ("river_hut".equals(code)) return "River side hut";
        return "Rooms";
    }
}
