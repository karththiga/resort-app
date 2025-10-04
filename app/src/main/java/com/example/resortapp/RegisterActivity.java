package com.example.resortapp;


import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPhone, etPassword;
    private Button btnRegister;
    private TextView tvGoLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AutoCompleteTextView etPreference;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);
        etPreference = findViewById(R.id.etPreference);

        // Simple dropdown
        String[] labels = new String[]{"Eco-pods", "Mountain-view cabins", "River side hut"};
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        etPreference.setAdapter(ad);
        etPreference.setOnClickListener(v -> etPreference.showDropDown());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> doRegister());
        tvGoLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private void doRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String pass = etPassword.getText().toString();

        if (name.isEmpty() || email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Fill name, email and 6+ char password", Toast.LENGTH_SHORT).show();
            return;
        }

        String prefLabel = String.valueOf(etPreference.getText());
        String preferredCategory = mapCategory(prefLabel); // normalize code

        btnRegister.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fu = auth.getCurrentUser();
                        if (fu == null) return;

                        String uid = fu.getUid();
                        Map<String, Object> doc = new HashMap<>();
                        doc.put("fullName", name);
                        doc.put("email", email);
                        doc.put("phone", phone);
                        doc.put("role", "GUEST");
                        doc.put("preferredRoomType", preferredCategory);
                        doc.put("createdAt", FieldValue.serverTimestamp());


                        db.collection("users").document(uid).set(doc)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Registered!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, DashboardActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    btnRegister.setEnabled(true);
                                });
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this, e != null ? e.getMessage() : "Registration error", Toast.LENGTH_LONG).show();
                        btnRegister.setEnabled(true);
                    }
                });
    }

    private String mapCategory(String label){
        if (label == null) return null;
        label = label.toLowerCase();
        if (label.contains("eco")) return "eco_pod";
        if (label.contains("mountain")) return "mountain_cabin";
        if (label.contains("river")) return "river_hut";
        return null;
    }
}
