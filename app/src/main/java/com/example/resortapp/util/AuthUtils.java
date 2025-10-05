package com.example.resortapp.util;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.resortapp.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

/** Utility helpers related to authentication actions. */
public final class AuthUtils {

    private AuthUtils() {
        // Utility class
    }

    /**
     * Signs the current Firebase user out and navigates back to the login screen.
     * Clears the activity back stack so the user cannot return after logging out.
     */
    public static void signOut(@NonNull Activity activity) {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
}
