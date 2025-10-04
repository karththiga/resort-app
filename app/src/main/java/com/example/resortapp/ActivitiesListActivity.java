package com.example.resortapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resortapp.model.Room;
import com.google.firebase.firestore.*;

import java.lang.reflect.Field;
import java.util.*;

public class ActivitiesListActivity extends AppCompatActivity {
    private RoomListAdapter adapter;
    private ListenerRegistration reg;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activities_list);

        RecyclerView rv = findViewById(R.id.rvAllActivities);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoomListAdapter();
        rv.setAdapter(adapter);

        adapter.setOnRoomClick(r -> {
            Intent i = new Intent(this, ActivityDetailActivity.class);
            i.putExtra("activityId", r.getId());
            startActivity(i);
        });

        Query q = FirebaseFirestore.getInstance().collection("activities")
                .whereEqualTo("status","ACTIVE")
                .orderBy("pricePerPerson");

        reg = q.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) return;
            List<Room> list = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) {
                Room r = new Room();
                // map fields used by adapter
                try { Field f = Room.class.getDeclaredField("id"); f.setAccessible(true); f.set(r, d.getId()); } catch (Exception ignore){}
                try { Field f = Room.class.getDeclaredField("name"); f.setAccessible(true); f.set(r, d.getString("name")); } catch (Exception ignore){}
                try { Field f = Room.class.getDeclaredField("imageUrl"); f.setAccessible(true); f.set(r, d.getString("imageUrl")); } catch (Exception ignore){}
                try { Field f = Room.class.getDeclaredField("basePrice"); f.setAccessible(true); f.set(r, d.getDouble("pricePerPerson")); } catch (Exception ignore){}
                try { Field f = Room.class.getDeclaredField("description"); f.setAccessible(true); f.set(r, d.getString("description")); } catch (Exception ignore){}
                list.add(r);
            }
            adapter.submit(list);
        });
    }

    @Override protected void onDestroy() { super.onDestroy(); if (reg != null) { reg.remove(); reg = null; } }
}
