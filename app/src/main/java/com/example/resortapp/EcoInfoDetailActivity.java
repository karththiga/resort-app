package com.example.resortapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.resortapp.model.EcoInfo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.*;

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
        View subtitleDivider = findViewById(R.id.subtitleDivider);
        TextView tvDesc = findViewById(R.id.tvDesc);

        String id = getIntent().getStringExtra("ecoId");
        if (id == null) { finish(); return; }

        FirebaseFirestore.getInstance().collection("eco_info").document(id)
                .get()
                .addOnSuccessListener(d -> {
                    EcoInfo e = d.toObject(EcoInfo.class);
                    if (e == null) { finish(); return; }
                    String title = e.getTitle();
                    if (title == null || title.trim().isEmpty()) {
                        title = getString(R.string.app_name);
                    }
                    tvTitle.setText(title);

                    String subtitle = e.getSubtitle();
                    if (subtitle == null || subtitle.trim().isEmpty()) {
                        tvSubtitle.setVisibility(View.GONE);
                        subtitleDivider.setVisibility(View.GONE);
                    } else {
                        tvSubtitle.setText(subtitle);
                        tvSubtitle.setVisibility(View.VISIBLE);
                        subtitleDivider.setVisibility(View.VISIBLE);
                    }

                    String desc = e.getDescription();
                    tvDesc.setText(desc != null && !desc.trim().isEmpty()
                            ? desc
                            : getString(R.string.eco_info_detail_no_description));

                    Glide.with(this)
                            .load(e.getImageUrl())
                            .placeholder(R.drawable.placeholder_room)
                            .into(img);
                })
                .addOnFailureListener(err -> { Toast.makeText(this, err.getMessage(), Toast.LENGTH_LONG).show(); finish(); });
    }
}
