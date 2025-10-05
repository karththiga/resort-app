package com.example.resortapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.resortapp.model.EcoInfo;
import com.google.firebase.firestore.*;
import com.google.android.material.appbar.MaterialToolbar;

public class EcoInfoDetailActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eco_info_detail);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        if (bar != null) {
            setSupportActionBar(bar);
            bar.setNavigationOnClickListener(v -> onBackPressed());
        }

        ImageView img = findViewById(R.id.img);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        TextView tvDesc = findViewById(R.id.tvDesc);

        String id = getIntent().getStringExtra("ecoId");
        if (id == null) { finish(); return; }

        FirebaseFirestore.getInstance().collection("eco_info").document(id)
                .get()
                .addOnSuccessListener(d -> {
                    EcoInfo e = d.toObject(EcoInfo.class);
                    if (e == null) { finish(); return; }
                    tvTitle.setText(e.getTitle());

                    String subtitle = e.getSubtitle();
                    if (subtitle != null && !subtitle.trim().isEmpty()) {
                        tvSubtitle.setText(subtitle);
                        tvSubtitle.setVisibility(View.VISIBLE);
                    } else {
                        tvSubtitle.setVisibility(View.GONE);
                    }

                    String description = e.getDescription();
                    tvDesc.setText(description != null && !description.trim().isEmpty()
                            ? description
                            : getString(R.string.eco_detail_no_description));

                    Glide.with(this)
                            .load(e.getImageUrl())
                            .placeholder(R.drawable.placeholder_room)
                            .into(img);
                })
                .addOnFailureListener(err -> { Toast.makeText(this, err.getMessage(), Toast.LENGTH_LONG).show(); finish(); });
    }
}
