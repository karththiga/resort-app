package com.example.resortapp;


import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class BookingsFragment extends Fragment {

    private RecyclerView rv;
    private BookingAdapter adapter;
    private ListenerRegistration reg;
    // fields
    private String currentKind = "ROOM"; // default

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = i.inflate(R.layout.fragment_bookings, c, false);
        rv = v.findViewById(R.id.rvBookings);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingAdapter();
        rv.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getUid();
        Query q = FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        reg = q.addSnapshotListener((qs, e) -> {
            if (e != null || qs == null) return;
            List<BookingItem> items = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) {
                items.add(BookingItem.from(d));
            }
            adapter.submit(items);
        });

        // in onCreateView after setting adapter & rv:
        ChipGroup chips = v.findViewById(R.id.chipGroup);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            currentKind = (id == R.id.chipActivities) ? "ACTIVITY" : "ROOM";
            subscribe();
        });

// initial:
        subscribe();

        return v;
    }

    private void subscribe() {
        if (reg != null) { reg.remove(); reg = null; }
        String uid = FirebaseAuth.getInstance().getUid();
        Query q = FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("userId", uid)
                .whereEqualTo("kind", currentKind)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        reg = q.addSnapshotListener((qs, e) -> {
            if (e != null || qs == null) return;
            List<BookingItem> items = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) {
                items.add(BookingItem.from(d));
            }
            adapter.submit(items);
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (rv != null) rv.setAdapter(null);
        adapter = null;
        if (reg != null) { reg.remove(); reg = null; }
    }

    // ---- Data + adapter
    static class BookingItem {
        String id, roomName, roomImageUrl, status;
        Date checkIn, checkOut;
        Double totalAmount;

//        static BookingItem from(DocumentSnapshot d) {
//            BookingItem i = new BookingItem();
//            i.id = d.getId();
//            i.roomName = d.getString("roomName");
//            i.roomImageUrl = d.getString("roomImageUrl");
//            i.status = d.getString("status");
//            Timestamp ci = d.getTimestamp("checkIn");
//            Timestamp co = d.getTimestamp("checkOut");
//            i.checkIn = ci != null ? ci.toDate() : null;
//            i.checkOut = co != null ? co.toDate() : null;
//            i.totalAmount = d.getDouble("totalAmount");
//            return i;
//        }

        static BookingItem from(DocumentSnapshot d) {
            BookingItem i = new BookingItem();
            i.id = d.getId();
            String kind = d.getString("kind");
            if ("ACTIVITY".equals(kind)) {
                i.roomName = d.getString("activityName"); // reuse field for display title
                i.roomImageUrl = d.getString("activityImageUrl");
                i.totalAmount = d.getDouble("totalAmount");
                Timestamp start = d.getTimestamp("scheduleStart");
                i.checkIn = start != null ? start.toDate() : null;
                i.checkOut = null;
                i.status = d.getString("status");
            } else {
                // ROOM
                i.roomName = d.getString("roomName");
                i.roomImageUrl = d.getString("roomImageUrl");
                i.totalAmount = d.getDouble("totalAmount");
                Timestamp ci = d.getTimestamp("checkIn");
                Timestamp co = d.getTimestamp("checkOut");
                i.checkIn = ci != null ? ci.toDate() : null;
                i.checkOut = co != null ? co.toDate() : null;
                i.status = d.getString("status");
            }
            return i;
        }
    }

    static class BookingAdapter extends RecyclerView.Adapter<VH> {
        private final List<BookingItem> data = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
        void submit(List<BookingItem> items) {
            data.clear(); data.addAll(items); notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_booking, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            BookingItem b = data.get(pos);
            h.name.setText(b.roomName);
            String dates = (b.checkIn != null && b.checkOut != null)
                    ? fmt.format(b.checkIn) + " → " + fmt.format(b.checkOut) : "";
            h.dates.setText(dates);
            h.total.setText(String.format(Locale.getDefault(),"Total: LKR %.0f", b.totalAmount == null ? 0.0 : b.totalAmount));
            h.status.setText(b.status != null ? b.status : "");
            Glide.with(h.img.getContext()).load(b.roomImageUrl).into(h.img);
        }
        @Override public int getItemCount() { return data.size(); }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img; TextView name, dates, total, status;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            name = v.findViewById(R.id.tvName);
            dates = v.findViewById(R.id.tvDates);
            total = v.findViewById(R.id.tvTotal);
            status = v.findViewById(R.id.tvStatus);
        }
    }
}
