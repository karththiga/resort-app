package com.example.resortapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
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
                .whereEqualTo("status","ACTIVE");

        reg = q.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) {
                Log.e("ActivitiesList", "Activities query failed", e);
                adapter.submit(Collections.emptyList());
                return;
            }
            List<Pair<Room, Double>> rows = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) {
                Room r = new Room();
                // map fields used by adapter
                try { Field f = Room.class.getDeclaredField("id"); f.setAccessible(true); f.set(r, d.getId()); } catch (Exception ignore){}
                try { Field f = Room.class.getDeclaredField("name"); f.setAccessible(true); f.set(r, d.getString("name")); } catch (Exception ignore){}
                try { Field f = Room.class.getDeclaredField("imageUrl"); f.setAccessible(true); f.set(r, d.getString("imageUrl")); } catch (Exception ignore){}
                Double price = null;
                try { Field f = Room.class.getDeclaredField("basePrice"); f.setAccessible(true); price = d.getDouble("pricePerPerson"); f.set(r, price); } catch (Exception ignore){}
                try { Field f = Room.class.getDeclaredField("description"); f.setAccessible(true); f.set(r, d.getString("description")); } catch (Exception ignore){}
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
            adapter.submit(list);
        });
    }

    @Override protected void onDestroy() { super.onDestroy(); if (reg != null) { reg.remove(); reg = null; } }
}
