package com.example.resortapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resortapp.model.EcoInfo;
import com.example.resortapp.model.Room;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class HomeFragment extends Fragment {

    private RecyclerView rv;
    private RoomListAdapter adapter;
    private String currentCategoryCode; // eco_pod | mountain_cabin | river_hut
    // --- in HomeFragment fields:
    private RecyclerView rvActivities;
    private RoomListAdapter activitiesAdapter; // reuse adapter class; shows name/img/price
    private ListenerRegistration roomsReg;
    private ListenerRegistration bookingsReg;
    private Long travelStartUtc, travelEndUtc;
    private List<Room> latestRooms = new ArrayList<>();
    private Set<String> unavailableRoomIds = new HashSet<>();

    // Fields
    private RecyclerView rvEco;
    private EcoInfoCardAdapter ecoAdapter;
    private ListenerRegistration ecoReg;


    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_home, c, false);

        TextView tvGreeting = v.findViewById(R.id.tvGreeting);
        String name = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "Guest";
        tvGreeting.setText("Good evening, " + (name != null ? name : "Guest") + "!");

        rv = v.findViewById(R.id.rvRooms);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // (Optional) smoother list
        RecyclerView.ItemAnimator ia = rv.getItemAnimator();
        if (ia instanceof androidx.recyclerview.widget.DefaultItemAnimator) {
            ((androidx.recyclerview.widget.DefaultItemAnimator) ia).setSupportsChangeAnimations(false);
        }

        adapter = new RoomListAdapter(RoomListAdapter.LayoutMode.CARD);
        rv.setAdapter(adapter);
        adapter.setOnRoomClick(room -> {
            Intent i = new Intent(requireContext(), RoomDetailActivity.class);
            i.putExtra("roomId", room.getId());
            startActivity(i);
        });

        // --- in onCreateView after Rooms setup:
        rvActivities = v.findViewById(R.id.rvActivities);
        rvActivities.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        activitiesAdapter = new RoomListAdapter(RoomListAdapter.LayoutMode.CARD); // it binds name/img/price; description can be empty
        rvActivities.setAdapter(activitiesAdapter);



        // open detail on tap:
        activitiesAdapter.setOnRoomClick(room -> {
            // We'll pass a flag telling detail screen it's an activity
            Intent i = new Intent(requireContext(), ActivityDetailActivity.class);
            i.putExtra("activityId", room.getId()); // adapter expects Room, so we'll map Activity -> pseudo Room object (see fetch)
            startActivity(i);
        });

        rvEco = v.findViewById(R.id.rvEcoInfo);
        rvEco.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext(),
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        ecoAdapter = new EcoInfoCardAdapter();
        rvEco.setAdapter(ecoAdapter);

        // Tap -> open detail
        ecoAdapter.setOnEcoClick(info -> {
            android.content.Intent i = new android.content.Intent(requireContext(), EcoInfoDetailActivity.class);
            i.putExtra("ecoId", info.getId());
            startActivity(i);
        });

        // Important when nested in a scroll container:
//        rv.setNestedScrollingEnabled(false);
//        rvActivities.setNestedScrollingEnabled(false);
//        rvEco.setNestedScrollingEnabled(false);

        // (Optional) smoother changes
        disableChangeAnimations(rv);
        disableChangeAnimations(rvActivities);
        disableChangeAnimations(rvEco);


// See more
        v.findViewById(R.id.tvSeeMoreEco).setOnClickListener(btn ->
                startActivity(new android.content.Intent(requireContext(), EcoInfoListActivity.class))
        );

// Subscribe to eco_info (top 10 by createdAt)
        com.google.firebase.firestore.Query q = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("eco_info")
                .whereEqualTo("status", "ACTIVE");

        ecoReg = q.addSnapshotListener((qs, e) -> {
            if (e != null || qs == null) {
                Log.e("HomeFragment", "Eco highlights query failed", e);
                ecoAdapter.submit(java.util.Collections.emptyList());
                return;
            }
            java.util.List<Pair<EcoInfo, com.google.firebase.Timestamp>> rows = new java.util.ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot d : qs.getDocuments()) {
                EcoInfo info = d.toObject(EcoInfo.class);
                if (info == null) continue;
                info.setId(d.getId());
                rows.add(Pair.create(info, d.getTimestamp("createdAt")));
            }
            rows.sort((a, n) -> {
                long bTime = n.second != null ? n.second.toDate().getTime() : Long.MIN_VALUE;
                long aTime = a.second != null ? a.second.toDate().getTime() : Long.MIN_VALUE;
                return Long.compare(bTime, aTime);
            });
            java.util.List<EcoInfo> list = new java.util.ArrayList<>();
            for (int i = 0; i < rows.size() && i < 10; i++) {
                list.add(rows.get(i).first);
            }
            ecoAdapter.submit(list);
        });

// load activities:
        subscribeActivities();

// see more:
        v.findViewById(R.id.tvSeeMoreActivities).setOnClickListener(btn -> {
            startActivity(new Intent(requireContext(), ActivitiesListActivity.class));
        });


        v.findViewById(R.id.tvSeeMore).setOnClickListener(btn -> {
            if (currentCategoryCode == null) {
                Toast.makeText(getContext(), "Set your room preference in Profile", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i1 = new Intent(requireContext(), RoomsListActivity.class);
            i1.putExtra("category", currentCategoryCode);
            startActivity(i1);
        });

        // Load user preference, then subscribe to rooms
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    currentCategoryCode = snap.getString("preferredRoomType");
                    Timestamp start = snap.getTimestamp("travelStart");
                    Timestamp end = snap.getTimestamp("travelEnd");
                    travelStartUtc = start != null ? start.toDate().getTime() : null;
                    travelEndUtc = end != null ? end.toDate().getTime() : null;
                    subscribeRooms(currentCategoryCode);
                    subscribeBookings();
                });

        return v;
    }

    private void disableChangeAnimations(RecyclerView rv) {
        RecyclerView.ItemAnimator ia = rv.getItemAnimator();
        if (ia instanceof androidx.recyclerview.widget.DefaultItemAnimator) {
            ((androidx.recyclerview.widget.DefaultItemAnimator) ia).setSupportsChangeAnimations(false);
        }
    }
    private ListenerRegistration activitiesReg;
    private void subscribeActivities() {
        if (activitiesReg != null) { activitiesReg.remove(); activitiesReg = null; }
        Query q = FirebaseFirestore.getInstance().collection("activities")
                .whereEqualTo("status","ACTIVE");
        activitiesReg = q.addSnapshotListener( (qs, e) -> {
            if (e != null || qs == null) {
                Log.e("HomeFragment", "Activities query failed", e);
                activitiesAdapter.submit(java.util.Collections.emptyList());
                return;
            }
            // Map Activity docs to a lightweight object your adapter can show.
            List<Pair<Room, Double>> rows = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) {
                // reuse Room model fields used by adapter
                Room r = new Room();
                try {
                    java.lang.reflect.Field f = Room.class.getDeclaredField("id");
                    f.setAccessible(true); f.set(r, d.getId());
                } catch (Exception ignored) {}
                try { java.lang.reflect.Field f = Room.class.getDeclaredField("name");
                    f.setAccessible(true); f.set(r, d.getString("name")); } catch (Exception ignored) {}
                try { java.lang.reflect.Field f = Room.class.getDeclaredField("imageUrl");
                    f.setAccessible(true); f.set(r, d.getString("imageUrl")); } catch (Exception ignored) {}
                Double price = null;
                try {
                    java.lang.reflect.Field f = Room.class.getDeclaredField("basePrice");
                    f.setAccessible(true);
                    price = d.getDouble("pricePerPerson");
                    f.set(r, price);
                } catch (Exception ignored) {}
                rows.add(Pair.create(r, price));
            }
            rows.sort((a, b) -> {
                double aPrice = a.second != null ? a.second : Double.MAX_VALUE;
                double bPrice = b.second != null ? b.second : Double.MAX_VALUE;
                return Double.compare(aPrice, bPrice);
            });
            List<Room> list = new ArrayList<>();
            for (Pair<Room, Double> row : rows) {
                list.add(row.first);
            }
            activitiesAdapter.submit(list);
        });
    }



    private void subscribeRooms(@Nullable String categoryCode) {
        // Stop previous listener (if any)
        if (roomsReg != null) { roomsReg.remove(); roomsReg = null; }

        Query q = FirebaseFirestore.getInstance().collection("rooms")
                .whereEqualTo("status", "ACTIVE");
        if (categoryCode != null) q = q.whereEqualTo("category", categoryCode);
        q = q.orderBy("basePrice");

        roomsReg = q.addSnapshotListener((qs, err) -> {
            if (err != null || qs == null) return;
            List<Room> list = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) list.add(FirestoreMappers.toRoom(d));
            latestRooms = list;
            updateRoomsAdapter();
        });
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

        bookingsReg = q.addSnapshotListener((qs, e) -> {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (roomsReg != null) { roomsReg.remove(); roomsReg = null; }
        if (bookingsReg != null) { bookingsReg.remove(); bookingsReg = null; }
        if (activitiesReg != null) { activitiesReg.remove(); activitiesReg = null; }
        if (ecoReg != null) { ecoReg.remove(); ecoReg = null; }
    }

}

