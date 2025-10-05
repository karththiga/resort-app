package com.example.resortapp;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityDetailActivity extends AppCompatActivity {

    private ImageView img; private TextView tvName, tvPrice, tvDesc, tvDate;
    private Button btnPickDate, btnReserve; private EditText etParticipants;

    private DocumentSnapshot activityDoc;
    private Long dateUtc = null; // activity day
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        if (bar != null) {
            setSupportActionBar(bar);
            bar.setNavigationOnClickListener(v -> onBackPressed());
        }

        img = findViewById(R.id.img);
        tvName = findViewById(R.id.tvName);
        tvPrice = findViewById(R.id.tvPrice);
        tvDesc = findViewById(R.id.tvDesc);
        tvDate = findViewById(R.id.tvDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnReserve = findViewById(R.id.btnReserve);
        etParticipants = findViewById(R.id.etParticipants);

        String id = getIntent().getStringExtra("activityId");
        if (id == null) { finish(); return; }

        FirebaseFirestore.getInstance().collection("activities").document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    activityDoc = doc;
                    String name = doc.getString("name");
                    Double price = doc.getDouble("pricePerPerson");
                    String desc = doc.getString("description");
                    String imageUrl = doc.getString("imageUrl");

                    tvName.setText(name);
                    tvPrice.setText(String.format(Locale.getDefault(), "LKR %,.0f / person", price == null ? 0.0 : price));
                    tvDesc.setText(desc != null && !desc.trim().isEmpty()
                            ? desc
                            : getString(R.string.activity_detail_no_description));
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.placeholder_room).into(img);
                })
                .addOnFailureListener(e -> { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); finish(); });

        btnPickDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> dp = MaterialDatePicker.Builder
                    .datePicker().setTitleText("Select date").build();
            dp.addOnPositiveButtonClickListener(sel -> {
                dateUtc = sel; tvDate.setText(fmt.format(new Date(dateUtc)));
            });
            dp.show(getSupportFragmentManager(), "date");
        });

        btnReserve.setOnClickListener(v -> reserve());
    }

    private void reserve() {
        if (activityDoc == null) return;
        if (dateUtc == null) { Toast.makeText(this, "Select a date", Toast.LENGTH_SHORT).show(); return; }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show(); return; }

        int participants = 1;
        try { participants = Integer.parseInt(etParticipants.getText().toString().trim()); } catch (Exception ignore) {}
        if (participants <= 0) { Toast.makeText(this, "Enter participants", Toast.LENGTH_SHORT).show(); return; }

        double price = activityDoc.getDouble("pricePerPerson") == null ? 0.0 : activityDoc.getDouble("pricePerPerson");
        double total = price * participants;

        Map<String,Object> b = new HashMap<>();
        b.put("kind", "ACTIVITY");
        b.put("userId", uid);
        b.put("activityId", activityDoc.getId());
        b.put("activityName", activityDoc.getString("name"));
        b.put("activityImageUrl", activityDoc.getString("imageUrl"));
        b.put("priceAtBooking", price);
        b.put("participants", participants);
        b.put("scheduleStart", new Timestamp(new Date(dateUtc)));
        b.put("totalAmount", total);
        b.put("status", "CONFIRMED");
        b.put("createdAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("bookings").add(b)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Reserved! See in My Bookings.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
