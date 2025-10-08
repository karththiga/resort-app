package com.example.resortapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilPreference;
    private MaterialAutoCompleteTextView etPreference;
    private TextView tvGoLogin;
    private Button btnRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilPreference = findViewById(R.id.tilPreference);
        etPreference = findViewById(R.id.etPreference);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        String[] labels = new String[]{"Eco-pods", "Mountain-view cabins", "River side hut"};
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        etPreference.setAdapter(ad);
        etPreference.setOnClickListener(v -> etPreference.showDropDown());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        clearErrorOnTextChange(tilName);
        clearErrorOnTextChange(tilEmail);
        clearErrorOnTextChange(tilPhone);
        clearErrorOnTextChange(tilPassword);
        clearErrorOnTextChange(tilPreference);

        btnRegister.setOnClickListener(v -> doRegister());
        tvGoLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private void doRegister() {
        EditText etName = (EditText) tilName.getEditText();
        EditText etEmail = (EditText) tilEmail.getEditText();
        EditText etPhone = (EditText) tilPhone.getEditText();
        EditText etPassword = (EditText) tilPassword.getEditText();

        if (etName == null || etEmail == null || etPhone == null || etPassword == null) {
            Toast.makeText(this, "Unable to read form", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String prefLabel = String.valueOf(etPreference.getText()).trim();

        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilPreference.setError(null);

        if (name.isEmpty()) {
            tilName.setError("Tell us your name");
            tilName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            tilEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            tilEmail.requestFocus();
            return;
        }

        if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
            tilPhone.setError("Enter a valid phone number");
            tilPhone.requestFocus();
            return;
        }

        if (pass.length() < 6) {
            tilPassword.setError("Create a password with at least 6 characters");
            tilPassword.requestFocus();
            return;
        }

        if (prefLabel.isEmpty()) {
            tilPreference.setError("Choose a room preference");
            tilPreference.requestFocus();
            return;
        }

        String preferredCategory = mapCategory(prefLabel);
        btnRegister.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fu = auth.getCurrentUser();
                        if (fu == null) {
                            btnRegister.setEnabled(true);
                            Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_LONG).show();
                            return;
                        }

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
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            tilEmail.setError("This email is already registered");
                            tilEmail.requestFocus();
                        } else {
                            Toast.makeText(this, e != null ? e.getMessage() : "Registration error", Toast.LENGTH_LONG).show();
                        }
                        btnRegister.setEnabled(true);
                    }
                });
    }

    private String mapCategory(String label) {
        if (label == null) return null;
        label = label.toLowerCase();
        if (label.contains("eco")) return "eco_pod";
        if (label.contains("mountain")) return "mountain_cabin";
        if (label.contains("river")) return "river_hut";
        return null;
    }

    private void clearErrorOnTextChange(TextInputLayout layout) {
        if (layout.getEditText() == null) {
            return;
        }
        layout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }
}
