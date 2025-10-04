package com.example.resortapp;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.*; import android.widget.*;
import androidx.annotation.*; import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class ProfileFragment extends Fragment {

    private TextInputEditText etName, etPhone;
    private AutoCompleteTextView etPreference;
    private Button btnSave;
    private MaterialSwitch swEco;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = i.inflate(R.layout.fragment_profile, c, false);

        etName = v.findViewById(R.id.etName);
        etPhone = v.findViewById(R.id.etPhone);
        etPreference = v.findViewById(R.id.etPreference);
        btnSave = v.findViewById(R.id.btnSave);
        Button btnLogout = v.findViewById(R.id.btnLogout);
        swEco = v.findViewById(R.id.swEco);


        btnLogout.setOnClickListener(view -> confirmAndLogout());

        String[] labels = new String[]{"Eco-pods", "Mountain-view cabins", "River side hut"};
        ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels);
        etPreference.setAdapter(ad);
        etPreference.setOnClickListener(v1 -> etPreference.showDropDown());

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
        new AlertDialog.Builder(requireContext())
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out", (d, which) -> {
                    FirebaseAuth.getInstance().signOut();

                    // If you also had Google Sign-In, you would signOut() there too.

                    // Go to Login and clear back stack so user can't return with Back
                    Intent i = new Intent(requireContext(), LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadUser() {
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    etName.setText(snap.getString("fullName"));
                    etPhone.setText(snap.getString("phone"));
                    String pc = snap.getString("preferredCategory");
                    etPreference.setText(human(pc), false);
                });
    }

    private void saveUser() {
        String uid = FirebaseAuth.getInstance().getUid();
        String name = String.valueOf(etName.getText()).trim();
        String phone = String.valueOf(etPhone.getText()).trim();
        String prefCode = mapCategory(String.valueOf(etPreference.getText()));

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fullName", name, "phone", phone, "preferredRoomType", prefCode)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
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
