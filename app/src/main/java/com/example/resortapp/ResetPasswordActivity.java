package com.example.resortapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSend;
    private TextView tvBack;
    private FirebaseAuth auth;
    private TextInputLayout tilEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etEmail = findViewById(R.id.etEmail);
        btnSend = findViewById(R.id.btnSend);
        tvBack  = findViewById(R.id.tvBack);
        tilEmail = findViewById(R.id.tilEmail);
        auth = FirebaseAuth.getInstance();

        btnSend.setOnClickListener(v -> sendReset());
        tvBack.setOnClickListener(v -> finish());
    }

    private void sendReset() {
        String email = etEmail.getText().toString().trim();

        tilEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            tilEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            tilEmail.requestFocus();
            return;
        }

        btnSend.setEnabled(false);

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Reset link sent. Check your email.", Toast.LENGTH_LONG).show();
                    finish(); // back to Login
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage();
                    if (msg != null && msg.toLowerCase().contains("no user record")) {
                        tilEmail.setError("No account found for this email.");
                        tilEmail.requestFocus();
                    } else {
                        Toast.makeText(this, msg != null ? msg : "Failed to send reset email", Toast.LENGTH_LONG).show();
                    }
                    btnSend.setEnabled(true);
                });
    }
}
