package com.example.resortapp;


import android.os.Bundle;
import android.view.*; import android.widget.*;
import androidx.annotation.*; import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.resortapp.util.AuthUtils;

public class ProfileFragment extends Fragment {

    private TextInputEditText etName, etPhone, etTravelStart, etTravelEnd;
    private AutoCompleteTextView etPreference;
    private Button btnSave;
    private MaterialSwitch swEco;
    private Long travelStartUtc, travelEndUtc;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = i.inflate(R.layout.fragment_profile, c, false);

        etName = v.findViewById(R.id.etName);
        etPhone = v.findViewById(R.id.etPhone);
        etPreference = v.findViewById(R.id.etPreference);
        btnSave = v.findViewById(R.id.btnSave);
        Button btnLogout = v.findViewById(R.id.btnLogout);
        swEco = v.findViewById(R.id.swEco);
        etTravelStart = v.findViewById(R.id.etTravelStart);
        etTravelEnd = v.findViewById(R.id.etTravelEnd);


        btnLogout.setOnClickListener(view -> confirmAndLogout());

        String[] labels = new String[]{"Eco-pods", "Mountain-view cabins", "River side hut"};
        ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels);
        etPreference.setAdapter(ad);
        etPreference.setOnClickListener(v1 -> etPreference.showDropDown());

        View.OnClickListener datesClickListener = v13 -> showDateRangePicker();
        etTravelStart.setOnClickListener(datesClickListener);
        etTravelEnd.setOnClickListener(datesClickListener);
        etTravelStart.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) showDateRangePicker(); });
        etTravelEnd.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) showDateRangePicker(); });

        loadUser();

        btnSave.setOnClickListener(v12 -> saveUser());

        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    Boolean eco = snap.getBoolean("ecoNotifications");
                    swEco.setChecked(eco != null && eco);
                });

        swEco.setOnCheckedChangeListener((button, checked) -> {
            FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .update("ecoNotifications", checked);
            // OPTIONAL: if you wire FCM topics later, subscribe/unsubscribe here.
            // if (checked) FirebaseMessaging.getInstance().subscribeToTopic("eco_events");
            // else FirebaseMessaging.getInstance().unsubscribeFromTopic("eco_events");
        });


        return v;
    }

    private void confirmAndLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.logout_positive, (d, which) ->
                        AuthUtils.signOut(requireActivity()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadUser() {
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    etName.setText(snap.getString("fullName"));
                    etPhone.setText(snap.getString("phone"));
                    String pc = snap.getString("preferredRoomType");
                    if (pc == null) pc = snap.getString("preferredCategory");
                    etPreference.setText(human(pc), false);
                    Timestamp start = snap.getTimestamp("travelStart");
                    Timestamp end = snap.getTimestamp("travelEnd");
                    travelStartUtc = start != null ? start.toDate().getTime() : null;
                    travelEndUtc = end != null ? end.toDate().getTime() : null;
                    bindTravelDates();
                });
    }

    private void saveUser() {
        String uid = FirebaseAuth.getInstance().getUid();
        String name = String.valueOf(etName.getText()).trim();
        String phone = String.valueOf(etPhone.getText()).trim();
        String prefCode = mapCategory(String.valueOf(etPreference.getText()));

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fullName", name,
                        "phone", phone,
                        "preferredRoomType", prefCode,
                        "travelStart", travelStartUtc != null ? new Timestamp(new Date(travelStartUtc)) : null,
                        "travelEnd", travelEndUtc != null ? new Timestamp(new Date(travelEndUtc)) : null)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select travel dates");
        if (travelStartUtc != null && travelEndUtc != null) {
            builder.setSelection(androidx.core.util.Pair.create(travelStartUtc, travelEndUtc));
        }
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel == null) return;
            travelStartUtc = sel.first;
            travelEndUtc = sel.second;
            bindTravelDates();
        });
        picker.show(getParentFragmentManager(), "travel_range");
    }

    private void bindTravelDates() {
        etTravelStart.setText(travelStartUtc != null ? fmt.format(new Date(travelStartUtc)) : "");
        etTravelEnd.setText(travelEndUtc != null ? fmt.format(new Date(travelEndUtc)) : "");
    }

    private String mapCategory(String label){
        if (label == null) return null;
        label = label.toLowerCase();
        if (label.contains("eco")) return "eco_pod";
        if (label.contains("mountain")) return "mountain_cabin";
        if (label.contains("river")) return "river_hut";
        return null;
    }
    private String human(String code){
        if ("eco_pod".equals(code)) return "Eco-pods";
        if ("mountain_cabin".equals(code)) return "Mountain-view cabins";
        if ("river_hut".equals(code)) return "River side hut";
        return "";
    }
}
