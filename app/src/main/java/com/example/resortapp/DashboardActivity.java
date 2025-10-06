package com.example.resortapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.resortapp.util.Helper;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity implements InboxFragment.InboxHost {

    private BottomNavigationView bottomNav;

    // Animation targets
    private static final float SCALE_SELECTED   = 1.12f;
    private static final float SCALE_UNSELECTED = 1.00f;
    private static final float ALPHA_SELECTED   = 1.0f;
    private static final float ALPHA_UNSELECTED = 0.80f;
    private static final int   ANIM_DURATION_MS = 180;

    private static final String PREF_INBOX = "inbox_prefs";
    private static final String KEY_LAST_SEEN = "last_seen";

    private final List<ListenerRegistration> inboxBadgeRegistrations = new ArrayList<>();
    private final Map<String, Long> promoBadgeTimes = new HashMap<>();
    private final Map<String, Long> manualNotificationBadgeTimes = new HashMap<>();
    private final Map<String, Long> bookingBadgeTimes = new HashMap<>();

    private boolean inboxVisible = false;

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

        setupInboxBadgeWatchers();


//        Helper helper = new Helper();
//        helper.seedRoomsIfNeeded();
//        helper.seedActivitiesIfNeeded();
//        helper.seedEcoInfoIfNeeded();
//        helper.seedPromosIfNeeded();


        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_inbox) {
                markInboxSeen();
            }
            switchFragment(itemId);
            animateSelection(itemId);
            return true;
        });

        // Default tab
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            // Post to ensure views are laid out before animating
            bottomNav.post(() -> animateSelection(R.id.nav_home));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ListenerRegistration registration : inboxBadgeRegistrations) {
            if (registration != null) {
                registration.remove();
            }
        }
        inboxBadgeRegistrations.clear();
    }

    private void switchFragment(int itemId) {
        Fragment f;
        if (itemId == R.id.nav_bookings)      f = new BookingsFragment();
        else if (itemId == R.id.nav_inbox)    f = new InboxFragment();
        else if (itemId == R.id.nav_profile)  f = new ProfileFragment();
        else                                  f = new HomeFragment();

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, f)
                .commitAllowingStateLoss();
    }

    private void setupInboxBadgeWatchers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        ListenerRegistration promoReg = db.collection("promos")
                .addSnapshotListener((snapshots, e) -> {
                    promoBadgeTimes.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            promoBadgeTimes.put(doc.getId(), timestampOrNow(doc.getTimestamp("createdAt")));
                        }
                    }
                    updateInboxBadge();
                });
        inboxBadgeRegistrations.add(promoReg);

        ListenerRegistration notificationReg = db.collection("notifications")
                .addSnapshotListener((snapshots, e) -> {
                    manualNotificationBadgeTimes.clear();
                    String uid = FirebaseAuth.getInstance().getUid();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (!shouldDisplay(doc, uid)) {
                                continue;
                            }
                            manualNotificationBadgeTimes.put(doc.getId(),
                                    timestampOrNow(doc.getTimestamp("createdAt")));
                        }
                    }
                    updateInboxBadge();
                });
        inboxBadgeRegistrations.add(notificationReg);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            Query bookingQuery = db.collection("bookings")
                    .whereEqualTo("userId", uid);
            ListenerRegistration bookingReg = bookingQuery.addSnapshotListener((snapshots, e) -> {
                bookingBadgeTimes.clear();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        bookingBadgeTimes.put(doc.getId(),
                                timestampOrNow(doc.getTimestamp("createdAt")));
                    }
                }
                updateInboxBadge();
            });
            inboxBadgeRegistrations.add(bookingReg);
        }

        updateInboxBadge();
    }

    private void updateInboxBadge() {
        runOnUiThread(() -> {
            if (bottomNav == null) {
                return;
            }
            if (inboxVisible) {
                markInboxSeenInternal(latestTimestamp());
                return;
            }

            long lastSeen = getLastInboxSeen();
            int newCount = countNewerThan(promoBadgeTimes, lastSeen)
                    + countNewerThan(manualNotificationBadgeTimes, lastSeen)
                    + countNewerThan(bookingBadgeTimes, lastSeen);

            if (newCount > 0) {
                BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_inbox);
                badge.setVisible(true);
                badge.setNumber(newCount);
            } else {
                clearInboxBadge();
            }
        });
    }

    private int countNewerThan(@NonNull Map<String, Long> source, long lastSeen) {
        int count = 0;
        for (long timestamp : source.values()) {
            if (timestamp > lastSeen) {
                count++;
            }
        }
        return count;
    }

    private long latestTimestamp() {
        long latest = 0L;
        for (long value : promoBadgeTimes.values()) {
            if (value > latest) latest = value;
        }
        for (long value : manualNotificationBadgeTimes.values()) {
            if (value > latest) latest = value;
        }
        for (long value : bookingBadgeTimes.values()) {
            if (value > latest) latest = value;
        }
        return latest > 0 ? latest : System.currentTimeMillis();
    }

    private long timestampOrNow(@Nullable Timestamp ts) {
        return ts != null ? ts.toDate().getTime() : System.currentTimeMillis();
    }

    private void markInboxSeenInternal(long timestamp) {
        SharedPreferences prefs = getSharedPreferences(PREF_INBOX, MODE_PRIVATE);
        long stored = prefs.getLong(KEY_LAST_SEEN, 0L);
        if (timestamp < stored) {
            timestamp = stored;
        }
        prefs.edit().putLong(KEY_LAST_SEEN, timestamp).apply();
        clearInboxBadge();
    }

    private long getLastInboxSeen() {
        SharedPreferences prefs = getSharedPreferences(PREF_INBOX, MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_SEEN, 0L);
    }

    private void clearInboxBadge() {
        if (bottomNav != null) {
            bottomNav.removeBadge(R.id.nav_inbox);
        }
    }

    private boolean shouldDisplay(@NonNull DocumentSnapshot doc, @Nullable String uid) {
        String directUser = doc.getString("userId");
        if (directUser != null && !directUser.isEmpty()) {
            return uid != null && uid.equals(directUser);
        }
        Object audience = doc.get("audience");
        if (audience instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) audience;
            if (list.contains("ALL")) {
                return true;
            }
            return uid != null && list.contains(uid);
        }
        return true;
    }

    private void markInboxSeen() {
        markInboxSeenInternal(System.currentTimeMillis());
    }

    @Override
    public void onInboxVisibilityChanged(boolean visible) {
        inboxVisible = visible;
        if (visible) {
            markInboxSeen();
        }
    }

    @Override
    public void onInboxViewed() {
        markInboxSeen();
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

