package com.example.resortapp;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InboxFragment extends Fragment {

    public interface InboxHost {
        void onInboxVisibilityChanged(boolean visible);
        void onInboxViewed();
    }

    private static class InboxItem {
        final String id;
        final String title;
        final String message;
        final String meta;
        final long sortKey;

        InboxItem(@NonNull String id, @NonNull String title, @NonNull String message,
                  @Nullable String meta, long sortKey) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.meta = meta;
            this.sortKey = sortKey;
        }
    }

    private LinearLayout promoContainer;
    private LinearLayout notificationContainer;
    private final List<ListenerRegistration> registrations = new ArrayList<>();
    private final Map<String, InboxItem> manualNotifications = new HashMap<>();
    private final Map<String, InboxItem> bookingNotifications = new HashMap<>();

    @Nullable
    private InboxHost inboxHost;

    public InboxFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof InboxHost) {
            inboxHost = (InboxHost) context;
        } else {
            inboxHost = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        inboxHost = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_inbox, container, false);

        promoContainer = root.findViewById(R.id.inboxPromosContainer);
        notificationContainer = root.findViewById(R.id.inboxNotificationsContainer);

        subscribeToPromos();
        subscribeToNotifications();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (inboxHost != null) {
            inboxHost.onInboxVisibilityChanged(true);
            inboxHost.onInboxViewed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (inboxHost != null) {
            inboxHost.onInboxVisibilityChanged(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration registration : registrations) {
            if (registration != null) {
                registration.remove();
            }
        }
        registrations.clear();
        promoContainer = null;
        notificationContainer = null;
        manualNotifications.clear();
        bookingNotifications.clear();
    }

    private void subscribeToPromos() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ListenerRegistration reg = db.collection("promos")
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded()) {
                        return;
                    }
                    List<InboxItem> promos = buildPromoItems(snapshots);
                    updateSection(promoContainer, promos);
                });
        registrations.add(reg);
    }

    private List<InboxItem> buildPromoItems(@Nullable QuerySnapshot snapshots) {
        List<InboxItem> promos = new ArrayList<>();
        if (snapshots == null) {
            return promos;
        }
        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            String id = doc.getId();
            String title = safeString(doc.getString("title"));
            String message = safeString(doc.getString("message"));
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(message)) {
                continue;
            }
            Timestamp createdAt = doc.getTimestamp("createdAt");
            Timestamp expiresAt = doc.getTimestamp("validUntil");
            String meta = null;
            if (expiresAt != null) {
                meta = getString(R.string.promo_expires, formatDate(expiresAt.toDate()));
            } else if (createdAt != null) {
                meta = formatRelative(createdAt.toDate());
            }
            long sortKey = createdAt != null ? createdAt.toDate().getTime() : 0L;
            promos.add(new InboxItem(id, title, message, meta, sortKey));
        }
        promos.sort((a, b) -> Long.compare(b.sortKey, a.sortKey));
        return promos;
    }

    private void subscribeToNotifications() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();

        ListenerRegistration genericReg = db.collection("notifications")
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded()) {
                        return;
                    }
                    manualNotifications.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (!shouldDisplay(doc, uid)) {
                                continue;
                            }
                            InboxItem item = buildNotificationItem(doc);
                            if (item != null) {
                                manualNotifications.put(doc.getId(), item);
                            }
                        }
                    }
                    rebuildNotifications();
                });
        registrations.add(genericReg);

        if (uid != null) {
            Query bookingQuery = db.collection("booking_notifications")
                    .whereEqualTo("userId", uid);
            ListenerRegistration bookingReg = bookingQuery.addSnapshotListener((snapshots, e) -> {
                if (!isAdded()) {
                    return;
                }
                bookingNotifications.clear();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        InboxItem item = buildBookingNotification(doc);
                        if (item != null) {
                            bookingNotifications.put(doc.getId(), item);
                        }
                    }
                }
                rebuildNotifications();
            });
            registrations.add(bookingReg);
        }
    }

    private void rebuildNotifications() {
        if (!isAdded() || notificationContainer == null) {
            return;
        }
        List<InboxItem> combined = new ArrayList<>(manualNotifications.values());
        combined.addAll(bookingNotifications.values());
        combined.sort((a, b) -> Long.compare(b.sortKey, a.sortKey));
        updateSection(notificationContainer, combined);
    }

    private void updateSection(@Nullable LinearLayout parent, @NonNull List<InboxItem> items) {
        if (parent == null || getContext() == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        parent.removeAllViews();

        for (InboxItem item : items) {
            View card = inflater.inflate(R.layout.item_inbox_entry, parent, false);

            TextView titleView = card.findViewById(R.id.inboxItemTitle);
            TextView messageView = card.findViewById(R.id.inboxItemMessage);
            TextView metaView = card.findViewById(R.id.inboxItemMeta);

            titleView.setText(item.title);
            messageView.setText(item.message);

            if (TextUtils.isEmpty(item.meta)) {
                metaView.setVisibility(View.GONE);
            } else {
                metaView.setVisibility(View.VISIBLE);
                metaView.setText(item.meta);
            }

            parent.addView(card);
        }

        if (items.isEmpty()) {
            TextView emptyView = new TextView(requireContext());
            emptyView.setText(R.string.inbox_section_empty);
            emptyView.setTextAppearance(requireContext(),
                    android.R.style.TextAppearance_Material_Body2);
            int padding = (int) (8 * getResources().getDisplayMetrics().density);
            emptyView.setPadding(0, padding, 0, padding);
            parent.addView(emptyView);
        }

        if (inboxHost != null && isResumed()) {
            inboxHost.onInboxViewed();
        }
    }

    private boolean shouldDisplay(@NonNull DocumentSnapshot doc, @Nullable String uid) {
        String directUser = doc.getString("userId");
        if (!TextUtils.isEmpty(directUser)) {
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

    @Nullable
    private InboxItem buildNotificationItem(@NonNull DocumentSnapshot doc) {
        String id = doc.getId();
        String title = safeString(doc.getString("title"));
        String message = safeString(doc.getString("message"));
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(message)) {
            return null;
        }
        Timestamp createdAt = doc.getTimestamp("createdAt");
        String meta = createdAt != null ? formatRelative(createdAt.toDate()) : null;
        long sortKey = createdAt != null ? createdAt.toDate().getTime() : 0L;
        return new InboxItem(id, title, message, meta, sortKey);
    }

    @Nullable
    private InboxItem buildBookingNotification(@NonNull DocumentSnapshot doc) {
        String kind = safeString(doc.getString("kind"));
        String status = safeString(doc.getString("status"));
        Timestamp createdAt = doc.getTimestamp("createdAt");
        long sortKey = createdAt != null ? createdAt.toDate().getTime() : 0L;
        String reference = safeString(doc.getString("bookingId"));
        if (TextUtils.isEmpty(reference)) {
            reference = doc.getId();
        }

        String title;
        String message;

        if ("ACTIVITY".equalsIgnoreCase(kind)) {
            String name = safeString(doc.getString("activityName"));
            Timestamp start = doc.getTimestamp("scheduleStart");
            title = formatStatus(status) + ": " + (TextUtils.isEmpty(name) ? getString(R.string.inbox_activity_default_title) : name);
            StringBuilder body = new StringBuilder();
            if (!TextUtils.isEmpty(name)) {
                body.append(getString(R.string.inbox_activity_message_intro, name));
            } else {
                body.append(getString(R.string.inbox_activity_message_generic));
            }
            if (start != null) {
                body.append(" ").append(getString(R.string.inbox_activity_when, formatDateTime(start.toDate())));
            }
            body.append(" ").append(getString(R.string.inbox_reference, reference));
            message = body.toString().trim();
        } else {
            String name = safeString(doc.getString("roomName"));
            Timestamp checkIn = doc.getTimestamp("checkIn");
            Timestamp checkOut = doc.getTimestamp("checkOut");
            title = formatStatus(status) + ": " + (TextUtils.isEmpty(name) ? getString(R.string.inbox_room_default_title) : name);
            StringBuilder body = new StringBuilder();
            if (!TextUtils.isEmpty(name)) {
                body.append(getString(R.string.inbox_room_message_intro, name));
            } else {
                body.append(getString(R.string.inbox_room_message_generic));
            }
            if (checkIn != null && checkOut != null) {
                body.append(" ").append(getString(R.string.inbox_room_dates,
                        formatDate(checkIn.toDate()), formatDate(checkOut.toDate())));
            }
            body.append(" ").append(getString(R.string.inbox_reference, reference));
            message = body.toString().trim();
        }

        String meta = createdAt != null ? formatRelative(createdAt.toDate()) : null;
        return new InboxItem(doc.getId(), title, message, meta, sortKey);
    }

    private String safeString(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private String formatRelative(@NonNull Date date) {
        long now = System.currentTimeMillis();
        CharSequence relative = DateUtils.getRelativeTimeSpanString(date.getTime(), now,
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
        return relative.toString();
    }

    private String formatDate(@NonNull Date date) {
        return DateFormat.getMediumDateFormat(requireContext()).format(date);
    }

    private String formatDateTime(@NonNull Date date) {
        return DateFormat.getMediumDateFormat(requireContext()).format(date) + " " +
                DateFormat.getTimeFormat(requireContext()).format(date);
    }

    private String formatStatus(@Nullable String status) {
        if (TextUtils.isEmpty(status)) {
            return getString(R.string.inbox_status_update);
        }
        String lower = status.toLowerCase(Locale.getDefault());
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
