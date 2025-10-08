package com.example.resortapp;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
    private final Map<String, Integer> bookingsCountByRoomId = new HashMap<>();
    private final Map<String, RoomKind> roomKindsById = new HashMap<>();
    private Long travelStartUtc, travelEndUtc;
    private RoomKind selectedTypeFilter = null;
    private PriceFilter selectedPriceFilter = PriceFilter.ANY;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms_list);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        setSupportActionBar(bar);
        bar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String category = getIntent().getStringExtra("category");
        TextView title = findViewById(R.id.tvTitle);
        title.setText(human(category) + " — All Rooms");

        Spinner typeFilterView = findViewById(R.id.spRoomTypeFilter);
        Spinner priceFilterView = findViewById(R.id.spPriceFilter);

        if (typeFilterView != null) {
            ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.rooms_filter_type_options,
                    android.R.layout.simple_spinner_item);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeFilterView.setAdapter(typeAdapter);
            typeFilterView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position <= 0) {
                        selectedTypeFilter = null;
                    } else {
                        int idx = position - 1;
                        if (idx >= 0 && idx < RoomKind.values().length) {
                            selectedTypeFilter = RoomKind.values()[idx];
                        } else {
                            selectedTypeFilter = null;
                        }
                    }
                    updateRoomsAdapter();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (priceFilterView != null) {
            ArrayAdapter<CharSequence> priceAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.rooms_filter_price_options,
                    android.R.layout.simple_spinner_item);
            priceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            priceFilterView.setAdapter(priceAdapter);
            priceFilterView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < PriceFilter.values().length) {
                        selectedPriceFilter = PriceFilter.values()[position];
                    } else {
                        selectedPriceFilter = PriceFilter.ANY;
                    }
                    updateRoomsAdapter();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

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
            Map<String, RoomKind> kinds = new HashMap<>();
            for (DocumentSnapshot d : qs.getDocuments()) {
                Room room = FirestoreMappers.toRoom(d);
                RoomKind kind = RoomKind.fromRaw(room.getType());
                if (kind == null) {
                    kind = RoomKind.fromRaw(room.getName());
                }
                if (kind != null) {
                    kinds.put(room.getId(), kind);
                    list.add(room);
                }
            }
            roomKindsById.clear();
            roomKindsById.putAll(kinds);
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
            bookingsCountByRoomId.clear();
            updateRoomsAdapter();
            return;
        }

        Query q = FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("status", "CONFIRMED")
                .whereLessThan("checkIn", new Timestamp(new Date(travelEndUtc)));

        bookingsReg = q.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) return;
            Map<String, Integer> busyCounts = new HashMap<>();
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
                    busyCounts.merge(roomId, 1, Integer::sum);
                }
            }
            bookingsCountByRoomId.clear();
            bookingsCountByRoomId.putAll(busyCounts);
            updateRoomsAdapter();
        });
    }

    private void updateRoomsAdapter() {
        if (latestRooms == null) return;
        List<Room> filtered = new ArrayList<>();
        for (Room room : latestRooms) {
            RoomKind kind = roomKindsById.get(room.getId());
            if (kind == null) continue;
            if (selectedTypeFilter != null && kind != selectedTypeFilter) continue;
            if (!selectedPriceFilter.matches(room.getBasePrice())) continue;

            Room display = room.copy();
            String displayName = kind.getDisplayName(this);
            if (display.getName() == null || display.getName().trim().isEmpty()) {
                display.setName(displayName);
            }
            display.setType(displayName);

            int booked = bookingsCountByRoomId.getOrDefault(room.getId(), 0);
            int totalCapacity = resolveRoomCapacity(room);
            int available = totalCapacity - booked;
            if (available < 0) available = 0;
            display.setAvailableRooms(available);
            display.setSoldOut(available <= 0);
            filtered.add(display);
        }
        adapter.submit(filtered);
    }

    private int resolveRoomCapacity(Room room) {
        if (room == null) return 0;
        Integer capacity = room.getCapacity();
        if (capacity == null) return 0;
        return Math.max(0, capacity);
    }

    private String human(String code){
        if ("eco_pod".equals(code)) return "Eco‑pods";
        if ("mountain_cabin".equals(code)) return "Mountain‑view cabins";
        if ("river_hut".equals(code)) return "River side hut";
        return "Rooms";
    }

    private enum RoomKind {
        QUEEN("queen", R.string.rooms_filter_type_queen),
        KING("king", R.string.rooms_filter_type_king),
        FAMILY("family", R.string.rooms_filter_type_family);

        private final String keyword;
        private final int displayNameRes;

        RoomKind(String keyword, int displayNameRes) {
            this.keyword = keyword;
            this.displayNameRes = displayNameRes;
        }

        public String getDisplayName(RoomsListActivity activity) {
            return activity.getString(displayNameRes);
        }

        static RoomKind fromRaw(String raw) {
            if (raw == null) return null;
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            String sanitized = normalized.replaceAll("[^a-z]+", " ").trim();
            String[] tokens = sanitized.isEmpty() ? new String[0] : sanitized.split("\\s+");
            for (RoomKind kind : values()) {
                if (matches(tokens, kind.keyword)) {
                    return kind;
                }
            }
            return null;
        }

        private static boolean matches(String[] tokens, String keyword) {
            for (String token : tokens) {
                if (token == null || token.isEmpty()) continue;
                if (token.equals(keyword) || token.equals(keyword + "s")) {
                    return true;
                }
            }
            return false;
        }
    }

    private enum PriceFilter {
        ANY {
            @Override boolean matches(Double price) { return true; }
        },
        BELOW_20000 {
            @Override boolean matches(Double price) { return toValue(price) < 20000.0; }
        },
        BETWEEN_20000_40000 {
            @Override boolean matches(Double price) {
                double value = toValue(price);
                return value >= 20000.0 && value <= 40000.0;
            }
        },
        ABOVE_40000 {
            @Override boolean matches(Double price) { return toValue(price) > 40000.0; }
        };

        abstract boolean matches(Double price);

        static double toValue(Double price) {
            return price == null ? 0.0 : price;
        }
    }
}
