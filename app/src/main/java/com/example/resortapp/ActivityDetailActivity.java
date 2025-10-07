package com.example.resortapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ActivityDetailActivity extends AppCompatActivity {

    private ImageView img; private TextView tvName, tvPrice, tvMeta, tvDesc, tvDate;
    private Button btnPickDate, btnReserve; private EditText etParticipants;

    private DocumentSnapshot activityDoc;
    private Long dateUtc = null; // activity day
    private long capacityPerSession = DEFAULT_ACTIVITY_CAPACITY;
    private boolean reserveInProgress = false;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private static final int DEFAULT_ACTIVITY_CAPACITY = 10;

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
        tvMeta = findViewById(R.id.tvMeta);
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
                    Long capacity = doc.getLong("capacityPerSession");
                    String imageUrl = doc.getString("imageUrl");

                    tvName.setText(name);
                    tvPrice.setText(String.format(Locale.getDefault(), "LKR %,.0f / person", price == null ? 0.0 : price));
                    tvDesc.setText(desc != null && !desc.trim().isEmpty()
                            ? desc
                            : getString(R.string.activity_detail_no_description));
                    if (capacity != null && capacity > 0) {
                        capacityPerSession = capacity;
                        tvMeta.setVisibility(View.VISIBLE);
                        tvMeta.setText(getString(R.string.activity_detail_meta_capacity_format, capacity));
                    } else {
                        tvMeta.setVisibility(View.GONE);
                    }
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
        if (reserveInProgress) return;
        if (dateUtc == null) { Toast.makeText(this, "Select a date", Toast.LENGTH_SHORT).show(); return; }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show(); return; }

        int participants = 1;
        try { participants = Integer.parseInt(etParticipants.getText().toString().trim()); } catch (Exception ignore) {}
        if (participants <= 0) { Toast.makeText(this, "Enter participants", Toast.LENGTH_SHORT).show(); return; }

        double price = activityDoc.getDouble("pricePerPerson") == null ? 0.0 : activityDoc.getDouble("pricePerPerson");
        double total = price * participants;

        Timestamp dayStart = new Timestamp(new Date(dateUtc));
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateUtc);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Timestamp nextDay = new Timestamp(cal.getTime());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        reserveInProgress = true;
        btnReserve.setEnabled(false);

        db.runTransaction(transaction -> {
                    Query query = db.collection("bookings")
                            .whereEqualTo("status", "CONFIRMED")
                            .whereEqualTo("kind", "ACTIVITY")
                            .whereEqualTo("activityId", activityDoc.getId())
                            .whereGreaterThanOrEqualTo("scheduleStart", dayStart)
                            .whereLessThan("scheduleStart", nextDay);

                    QuerySnapshot prefetch;
                    try {
                        prefetch = Tasks.await(query.get());
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    int reserved = 0;
                    for (DocumentSnapshot doc : prefetch.getDocuments()) {
                        DocumentSnapshot existing = transaction.get(doc.getReference());
                        Long existingParticipants = existing.getLong("participants");
                        if (existingParticipants != null) {
                            reserved += existingParticipants.intValue();
                        }
                    }

                    if (reserved + participants > capacityPerSession) {
                        throw new FirebaseFirestoreException(
                                "NO_AVAILABILITY",
                                FirebaseFirestoreException.Code.ABORTED);
                    }

                    DocumentReference newBookingRef = db.collection("bookings").document();
                    transaction.set(newBookingRef, buildActivityBookingPayload(uid, participants, price, total, dayStart));
                    return null;
                })
                .addOnSuccessListener(ignored -> {
                    reserveInProgress = false;
                    Toast.makeText(this, "Reserved! See in My Bookings.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    reserveInProgress = false;
                    btnReserve.setEnabled(true);
                    if (e instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
                        if (ffe.getCode() == FirebaseFirestoreException.Code.ABORTED &&
                                "NO_AVAILABILITY".equals(ffe.getMessage())) {
                            showNoAvailabilityDialog();
                            return;
                        }
                    }
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private Map<String, Object> buildActivityBookingPayload(String uid,
                                                            int participants,
                                                            double pricePerPerson,
                                                            double total,
                                                            Timestamp scheduleStart) {
        Map<String, Object> b = new HashMap<>();
        b.put("kind", "ACTIVITY");
        b.put("userId", uid);
        b.put("activityId", activityDoc.getId());
        b.put("activityName", activityDoc.getString("name"));
        b.put("activityImageUrl", activityDoc.getString("imageUrl"));
        b.put("priceAtBooking", pricePerPerson);
        b.put("participants", participants);
        b.put("scheduleStart", scheduleStart);
        b.put("totalAmount", total);
        b.put("status", "CONFIRMED");
        b.put("createdAt", FieldValue.serverTimestamp());
        return b;
    }

    private void showNoAvailabilityDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.activity_detail_no_availability_dialog_title)
                .setMessage(R.string.activity_detail_no_availability_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
