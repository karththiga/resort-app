package com.example.resortapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.resortapp.util.Helper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    // Animation targets
    private static final float SCALE_SELECTED   = 1.12f;
    private static final float SCALE_UNSELECTED = 1.00f;
    private static final float ALPHA_SELECTED   = 1.0f;
    private static final float ALPHA_UNSELECTED = 0.80f;
    private static final int   ANIM_DURATION_MS = 180;

    protected void onResume() {
        super.onResume();
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboardMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNav = findViewById(R.id.bottomNav);


//        Helper helper = new Helper();
//        helper.seedRoomsIfNeeded();
//        helper.seedActivitiesIfNeeded();
//        helper.seedEcoInfoIfNeeded();


        bottomNav.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId());
            animateSelection(item.getItemId());
            return true;
        });

        // Default tab
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            // Post to ensure views are laid out before animating
            bottomNav.post(() -> animateSelection(R.id.nav_home));
        }
    }

    private void switchFragment(int itemId) {
        Fragment f;
        if (itemId == R.id.nav_bookings)      f = new BookingsFragment();
        else if (itemId == R.id.nav_promo)    f = new PromoFragment();
        else if (itemId == R.id.nav_profile)  f = new ProfileFragment();
        else                                  f = new HomeFragment();

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, f)
                .commitAllowingStateLoss();
    }

    /** Scale + fade the selected item, reset others. No restricted APIs used. */
    private void animateSelection(@IdRes int selectedId) {
        // 1) Reset all items
        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            int itemId = bottomNav.getMenu().getItem(i).getItemId();
            View itemView = bottomNav.findViewById(itemId);
            if (itemView != null) applyAnim(itemView, SCALE_UNSELECTED, ALPHA_UNSELECTED);
        }

        // 2) Animate the selected item
        View selectedView = bottomNav.findViewById(selectedId);
        if (selectedView != null) applyAnim(selectedView, SCALE_SELECTED, ALPHA_SELECTED);
    }

    private void applyAnim(@NonNull View itemView, float scale, float alpha) {
        // Icon view
        View icon = itemView.findViewById(
                com.google.android.material.R.id.navigation_bar_item_icon_view);
        // Small label (when labelVisibilityMode=labeled this is the one used)
        View label = itemView.findViewById(
                com.google.android.material.R.id.navigation_bar_item_small_label_view);

        animateView(icon, scale, alpha);
        animateView(label, 1.0f, alpha); // label doesn’t need scale, only fade
    }

    private void animateView(View target, float scale, float alpha) {
        if (target == null) return;
        PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, scale);
        PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, scale);
        PropertyValuesHolder a  = PropertyValuesHolder.ofFloat(View.ALPHA,  alpha);
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(target, sx, sy, a);
        oa.setDuration(ANIM_DURATION_MS);
        oa.setInterpolator(new DecelerateInterpolator());
        oa.start();
    }
}

