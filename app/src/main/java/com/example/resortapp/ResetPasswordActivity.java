package com.example.resortapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSend;
    private TextView tvBack;
    private FirebaseAuth auth;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etEmail = findViewById(R.id.etEmail);
        btnSend = findViewById(R.id.btnSend);
        tvBack  = findViewById(R.id.tvBack);
        auth = FirebaseAuth.getInstance();

        btnSend.setOnClickListener(v -> sendReset());
        tvBack.setOnClickListener(v -> finish());
    }

    private void sendReset() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
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
                    // Friendly messages for common cases
                    if (msg != null && msg.toLowerCase().contains("no user record")) {
                        msg = "No account found for this email.";
                    }
                    Toast.makeText(this, msg != null ? msg : "Failed to send reset email", Toast.LENGTH_LONG).show();
                    btnSend.setEnabled(true);
                });
    }
}
