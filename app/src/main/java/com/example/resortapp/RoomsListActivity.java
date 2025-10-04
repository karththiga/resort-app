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
import com.google.firebase.firestore.*;

import java.util.*;

public class RoomsListActivity extends AppCompatActivity {

    private RoomListAdapter adapter;
    private ListenerRegistration reg;

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

        reg = q.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) return;
            List<Room> list = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) list.add(FirestoreMappers.toRoom(d));
            adapter.submit(list);
        });

        adapter.setOnRoomClick(room -> {
            Intent i = new Intent(RoomsListActivity.this, RoomDetailActivity.class);
            i.putExtra("roomId", room.getId());
            startActivity(i);
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private String human(String code){
        if ("eco_pod".equals(code)) return "Eco‑pods";
        if ("mountain_cabin".equals(code)) return "Mountain‑view cabins";
        if ("river_hut".equals(code)) return "River side hut";
        return "Rooms";
    }
}
