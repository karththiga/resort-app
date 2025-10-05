package com.example.resortapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;

public class InboxFragment extends Fragment {

    private static class InboxItem {
        final String title;
        final String message;
        final String meta;

        InboxItem(String title, String message, String meta) {
            this.title = title;
            this.message = message;
            this.meta = meta;
        }
    }

    private static final List<InboxItem> PROMO_ITEMS = Arrays.asList(
            new InboxItem(
                    "Save 15% on your next stay",
                    "Because you loved the ocean-view suite, enjoy an exclusive loyalty discount when you book before the end of the month.",
                    "Expires Jun 30"
            ),
            new InboxItem(
                    "Complimentary breakfast upgrade",
                    "Add breakfast for two to your weekend getaway – it's on us when you reserve a spa treatment in the same booking.",
                    "Limited availability"
            ),
            new InboxItem(
                    "Return guest welcome gift",
                    "We're holding a welcome basket with locally sourced treats the next time you check in. Just show this message at the desk.",
                    ""
            )
    );

    private static final List<InboxItem> NOTIFICATION_ITEMS = Arrays.asList(
            new InboxItem(
                    "Spa appointment confirmed",
                    "Your aromatherapy massage for 4:00 PM on 18 Jun is all set. Arrive 15 minutes early to enjoy complimentary tea.",
                    "Today"
            ),
            new InboxItem(
                    "Seaside villa booking received",
                    "Thanks for reserving the Seaside Villa from 12–15 Jul. Your confirmation number is RS-483920.",
                    "Yesterday"
            ),
            new InboxItem(
                    "Profile updated",
                    "We noticed you changed your contact email. If this wasn't you, reach out to our concierge team anytime.",
                    "2 days ago"
            )
    );

    public InboxFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_inbox, container, false);

        LinearLayout promoContainer = root.findViewById(R.id.inboxPromosContainer);
        LinearLayout notificationContainer = root.findViewById(R.id.inboxNotificationsContainer);

        populateSection(inflater, promoContainer, PROMO_ITEMS);
        populateSection(inflater, notificationContainer, NOTIFICATION_ITEMS);

        return root;
    }

    private void populateSection(@NonNull LayoutInflater inflater,
                                 @NonNull LinearLayout parent,
                                 @NonNull List<InboxItem> items) {
        parent.removeAllViews();

        for (InboxItem item : items) {
            View card = inflater.inflate(R.layout.item_inbox_entry, parent, false);

            TextView titleView = card.findViewById(R.id.inboxItemTitle);
            TextView messageView = card.findViewById(R.id.inboxItemMessage);
            TextView metaView = card.findViewById(R.id.inboxItemMeta);

            titleView.setText(item.title);
            messageView.setText(item.message);

            if (item.meta == null || item.meta.trim().isEmpty()) {
                metaView.setVisibility(View.GONE);
            } else {
                metaView.setText(item.meta);
                metaView.setVisibility(View.VISIBLE);
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
    }
}
