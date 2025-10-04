package com.example.resortapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resortapp.model.EcoInfo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.*;

import java.util.*;

public class EcoInfoListActivity extends AppCompatActivity {

    private RecyclerView rvInitiatives, rvReserves, rvPractices;
    private EcoBulletAdapter adInitiatives, adReserves, adPractices;
    private ListenerRegistration regInit, regRes, regPrac;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eco_info_list);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        setSupportActionBar(bar);
        bar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

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
                .whereEqualTo("type", "initiative");

        Query qReserves = db.collection("eco_info")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("type", "nature_reserve");

        Query qPractices = db.collection("eco_info")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("type", "practice");

        // listen & populate (Activity-scoped listeners)
        regInit = qInitiatives.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) {
                Log.e("EcoInfoList", "Initiatives query failed", e);
                adInitiatives.submit(Collections.emptyList());
                return;
            }
            adInitiatives.submit(map(qs));
        });
        regRes = qReserves.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) {
                Log.e("EcoInfoList", "Reserves query failed", e);
                adReserves.submit(Collections.emptyList());
                return;
            }
            adReserves.submit(map(qs));
        });
        regPrac = qPractices.addSnapshotListener(this, (qs, e) -> {
            if (e != null || qs == null) {
                Log.e("EcoInfoList", "Practices query failed", e);
                adPractices.submit(Collections.emptyList());
                return;
            }
            adPractices.submit(map(qs));
        });
    }

    private List<EcoInfo> map(QuerySnapshot qs) {
        List<Pair<EcoInfo, com.google.firebase.Timestamp>> rows = new ArrayList<>();
        for (DocumentSnapshot d : qs.getDocuments()) {
            EcoInfo e = d.toObject(EcoInfo.class);
            if (e == null) continue;
            e.setId(d.getId());
            rows.add(Pair.create(e, d.getTimestamp("createdAt")));
        }
        rows.sort((a, b) -> {
            long bTime = b.second != null ? b.second.toDate().getTime() : Long.MIN_VALUE;
            long aTime = a.second != null ? a.second.toDate().getTime() : Long.MIN_VALUE;
            return Long.compare(bTime, aTime);
        });
        List<EcoInfo> list = new ArrayList<>();
        for (Pair<EcoInfo, com.google.firebase.Timestamp> row : rows) {
            list.add(row.first);
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

