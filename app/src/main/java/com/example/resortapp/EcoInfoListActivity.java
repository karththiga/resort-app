package com.example.resortapp;



import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resortapp.model.EcoInfo;
import com.google.firebase.firestore.*;

import java.util.*;

public class EcoInfoListActivity extends AppCompatActivity {

    private RecyclerView rvInitiatives, rvReserves, rvPractices;
    private EcoBulletAdapter adInitiatives, adReserves, adPractices;
    private ListenerRegistration regInit, regRes, regPrac;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eco_info_list);

        // find views
        rvInitiatives = findViewById(R.id.rvInitiatives);
        rvReserves    = findViewById(R.id.rvReserves);
        rvPractices   = findViewById(R.id.rvPractices);

        // layout managers
        rvInitiatives.setLayoutManager(new LinearLayoutManager(this));
        rvReserves.setLayoutManager(new LinearLayoutManager(this));
        rvPractices.setLayoutManager(new LinearLayoutManager(this));

        // adapters
        adInitiatives = new EcoBulletAdapter();
        adReserves    = new EcoBulletAdapter();
        adPractices   = new EcoBulletAdapter();

        // click -> detail
        EcoBulletAdapter.OnClick goDetail = info -> {
            Intent i = new Intent(this, EcoInfoDetailActivity.class);
            i.putExtra("ecoId", info.getId());
            startActivity(i);
        };
        adInitiatives.setOnClick(goDetail);
        adReserves.setOnClick(goDetail);
        adPractices.setOnClick(goDetail);

        rvInitiatives.setAdapter(adInitiatives);
        rvReserves.setAdapter(adReserves);
        rvPractices.setAdapter(adPractices);

        // queries
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Query qInitiatives = db.collection("eco_info")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("type", "initiative")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        Query qReserves = db.collection("eco_info")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("type", "nature_reserve")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        Query qPractices = db.collection("eco_info")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("type", "practice")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        // listen & populate (Activity-scoped listeners)
        regInit = qInitiatives.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) return;
            adInitiatives.submit(map(qs));
        });
        regRes = qReserves.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) return;
            adReserves.submit(map(qs));
        });
        regPrac = qPractices.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) return;
            adPractices.submit(map(qs));
        });
    }

    private List<EcoInfo> map(QuerySnapshot qs) {
        List<EcoInfo> list = new ArrayList<>();
        for (DocumentSnapshot d : qs.getDocuments()) {
            EcoInfo e = d.toObject(EcoInfo.class);
            if (e == null) continue;
            e.setId(d.getId());
            list.add(e);
        }
        return list;
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (regInit != null) regInit.remove();
        if (regRes  != null) regRes.remove();
        if (regPrac != null) regPrac.remove();
    }
}

