package com.example.resortapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BookingsFragment extends Fragment {

    private static final int DEFAULT_ACTIVITY_CAPACITY = 10;

    private final SimpleDateFormat activityDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private RecyclerView rv;
    private BookingAdapter adapter;
    private ListenerRegistration reg;
    private String currentKind = "ROOM"; // default
    private TabLayout tabLayout;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptySubtitle;
    private CircularProgressIndicator loadingIndicator;
    private final List<BookingItem> cachedRooms = new ArrayList<>();
    private final List<BookingItem> cachedActivities = new ArrayList<>();
    private boolean roomsLoaded;
    private boolean activitiesLoaded;
    private final TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            String newKind = tab.getPosition() == 1 ? "ACTIVITY" : "ROOM";
            if (!TextUtils.equals(currentKind, newKind)) {
                currentKind = newKind;
                renderCurrentKind();
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            // no-op
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            // no-op
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bookings, container, false);
        rv = v.findViewById(R.id.rvBookings);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingAdapter(new BookingAdapter.OnBookingActionListener() {
            @Override
            public void onEditActivity(@NonNull BookingItem item) {
                showUpdateActivityDialog(item);
            }

            @Override
            public void onDeleteActivity(@NonNull BookingItem item) {
                confirmDeleteActivity(item);
            }
        });
        rv.setAdapter(adapter);

        emptyState = v.findViewById(R.id.layoutEmptyBookings);
        emptyTitle = v.findViewById(R.id.tvEmptyTitle);
        emptySubtitle = v.findViewById(R.id.tvEmptySubtitle);
        loadingIndicator = v.findViewById(R.id.progressBookings);

        tabLayout = v.findViewById(R.id.tabLayoutBookings);
        tabLayout.addOnTabSelectedListener(tabSelectedListener);
        TabLayout.Tab defaultTab = tabLayout.getTabAt("ACTIVITY".equals(currentKind) ? 1 : 0);
        if (defaultTab != null) {
            defaultTab.select();
        }

        subscribe();

        return v;
    }

    private void subscribe() {
        if (reg != null) {
            reg.remove();
            reg = null;
        }
        cachedRooms.clear();
        cachedActivities.clear();
        roomsLoaded = false;
        activitiesLoaded = false;
        renderCurrentKind();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            roomsLoaded = true;
            activitiesLoaded = true;
            renderCurrentKind();
            return;
        }

        Query q = FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        reg = q.addSnapshotListener((qs, e) -> {
            if (e != null || qs == null) {
                if (isAdded() && e != null) {
                    String message = e.getMessage();
                    if (TextUtils.isEmpty(message)) {
                        message = getString(R.string.bookings_error_loading);
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
                return;
            }
            cachedRooms.clear();
            cachedActivities.clear();
            for (DocumentSnapshot d : qs.getDocuments()) {
                BookingItem item = BookingItem.from(d);
                if ("ACTIVITY".equals(item.kind)) {
                    cachedActivities.add(item);
                } else {
                    cachedRooms.add(item);
                }
            }
            roomsLoaded = true;
            activitiesLoaded = true;
            renderCurrentKind();
        });
    }

    private void renderCurrentKind() {
        if (adapter == null) {
            return;
        }
        List<BookingItem> source;
        if ("ACTIVITY".equals(currentKind)) {
            source = new ArrayList<>(cachedActivities);
        } else {
            source = new ArrayList<>(cachedRooms);
        }
        adapter.submit(source);
        boolean loaded = "ACTIVITY".equals(currentKind) ? activitiesLoaded : roomsLoaded;
        updateEmptyState(currentKind, source.isEmpty(), loaded);
    }

    private void updateEmptyState(@NonNull String kind, boolean empty, boolean loaded) {
        if (!TextUtils.equals(currentKind, kind) || rv == null || emptyState == null || emptyTitle == null || emptySubtitle == null) {
            return;
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(loaded ? View.GONE : View.VISIBLE);
        }
        if (!loaded) {
            rv.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            return;
        }
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (!empty) {
            return;
        }
        if ("ACTIVITY".equals(kind)) {
            emptyTitle.setText(R.string.bookings_empty_activity_title);
            emptySubtitle.setText(R.string.bookings_empty_activity_message);
        } else {
            emptyTitle.setText(R.string.bookings_empty_room_title);
            emptySubtitle.setText(R.string.bookings_empty_room_message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rv != null) {
            rv.setAdapter(null);
        }
        adapter = null;
        emptyState = null;
        emptyTitle = null;
        emptySubtitle = null;
        loadingIndicator = null;
        if (tabLayout != null) {
            tabLayout.removeOnTabSelectedListener(tabSelectedListener);
            tabLayout = null;
        }
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }

    private void showUpdateActivityDialog(@NonNull BookingItem item) {
        if (!"ACTIVITY".equals(item.kind) || !isAdded()) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_activity_booking, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tvSelectedDate);
        MaterialButton btnPickDate = dialogView.findViewById(R.id.btnPickDate);
        TextInputLayout tilParticipants = dialogView.findViewById(R.id.tilParticipants);
        TextInputEditText etParticipants = dialogView.findViewById(R.id.etParticipants);
        CircularProgressIndicator progress = dialogView.findViewById(R.id.progressUpdate);

        tvTitle.setText(!TextUtils.isEmpty(item.roomName) ? item.roomName : getString(R.string.inbox_activity_default_title));

        final Calendar selectedDate = Calendar.getInstance();
        if (item.checkIn != null) {
            selectedDate.setTime(item.checkIn);
        } else {
            selectedDate.setTimeInMillis(MaterialDatePicker.todayInUtcMilliseconds());
        }
        normalizeCalendarToDayStart(selectedDate);
        tvSelectedDate.setText(activityDateFormat.format(selectedDate.getTime()));

        btnPickDate.setOnClickListener(v -> {
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
            builder.setTitleText(R.string.booking_activity_update_pick_date);
            builder.setSelection(selectedDate.getTimeInMillis());
            MaterialDatePicker<Long> picker = builder.build();
            picker.addOnPositiveButtonClickListener(selection -> {
                if (selection == null) {
                    return;
                }
                selectedDate.setTimeInMillis(selection);
                normalizeCalendarToDayStart(selectedDate);
                tvSelectedDate.setText(activityDateFormat.format(selectedDate.getTime()));
            });
            picker.show(getChildFragmentManager(), "activity_update_date");
        });

        if (item.participants > 0) {
            etParticipants.setText(String.valueOf(item.participants));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.booking_activity_update_positive, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                tilParticipants.setError(null);
                String participantsText = etParticipants.getText() != null ? etParticipants.getText().toString().trim() : "";
                if (TextUtils.isEmpty(participantsText)) {
                    tilParticipants.setError(getString(R.string.booking_activity_update_validation_participants));
                    return;
                }
                int participants;
                try {
                    participants = Integer.parseInt(participantsText);
                } catch (NumberFormatException ex) {
                    tilParticipants.setError(getString(R.string.booking_activity_update_validation_participants));
                    return;
                }
                if (participants <= 0) {
                    tilParticipants.setError(getString(R.string.booking_activity_update_validation_participants));
                    return;
                }
                long selectedDateMillis = selectedDate.getTimeInMillis();
                if (selectedDateMillis <= 0) {
                    Toast.makeText(requireContext(), R.string.booking_activity_update_validation_date, Toast.LENGTH_SHORT).show();
                    return;
                }
                setUpdateDialogInProgress(true, progress, positiveButton, btnPickDate, etParticipants);
                updateActivityReservation(item, selectedDateMillis, participants, new ActivityUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), R.string.booking_activity_update_success, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) {
                            return;
                        }
                        setUpdateDialogInProgress(false, progress, positiveButton, btnPickDate, etParticipants);
                        if (e instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
                            if (ffe.getCode() == FirebaseFirestoreException.Code.ABORTED && "NO_AVAILABILITY".equals(ffe.getMessage())) {
                                showNoAvailabilityDialog();
                                return;
                            }
                        }
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
        dialog.show();
    }

    private void setUpdateDialogInProgress(boolean inProgress,
                                            CircularProgressIndicator progress,
                                            Button positiveButton,
                                            MaterialButton btnPickDate,
                                            TextInputEditText etParticipants) {
        progress.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        positiveButton.setEnabled(!inProgress);
        btnPickDate.setEnabled(!inProgress);
        etParticipants.setEnabled(!inProgress);
    }

    private void updateActivityReservation(@NonNull BookingItem item,
                                           long dateUtc,
                                           int participants,
                                           @NonNull ActivityUpdateCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateUtc);
        normalizeCalendarToDayStart(cal);
        Timestamp dayStart = new Timestamp(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Timestamp nextDay = new Timestamp(cal.getTime());

        db.runTransaction(transaction -> {
            DocumentReference bookingRef = db.collection("bookings").document(item.id);
            DocumentSnapshot bookingSnapshot = transaction.get(bookingRef);
            if (!bookingSnapshot.exists()) {
                throw new FirebaseFirestoreException("NOT_FOUND", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String activityId = bookingSnapshot.getString("activityId");
            if (activityId == null) {
                activityId = item.activityId;
            }
            if (activityId == null) {
                throw new FirebaseFirestoreException("INVALID_ACTIVITY", FirebaseFirestoreException.Code.ABORTED);
            }

            DocumentReference activityRef = db.collection("activities").document(activityId);
            DocumentSnapshot activitySnapshot = transaction.get(activityRef);

            long capacity = DEFAULT_ACTIVITY_CAPACITY;
            if (activitySnapshot.exists()) {
                Long cap = activitySnapshot.getLong("capacityPerSession");
                if (cap != null && cap > 0) {
                    capacity = cap;
                }
            }

            Query query = db.collection("bookings")
                    .whereEqualTo("status", "CONFIRMED")
                    .whereEqualTo("kind", "ACTIVITY")
                    .whereEqualTo("activityId", activityId)
                    .whereGreaterThanOrEqualTo("scheduleStart", dayStart)
                    .whereLessThan("scheduleStart", nextDay);

            QuerySnapshot existing;
            try {
                existing = Tasks.await(query.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            int reserved = 0;
            for (DocumentSnapshot doc : existing.getDocuments()) {
                if (doc.getId().equals(item.id)) {
                    continue;
                }
                Long existingParticipants = doc.getLong("participants");
                if (existingParticipants != null) {
                    reserved += existingParticipants.intValue();
                }
            }

            if (reserved + participants > capacity) {
                throw new FirebaseFirestoreException("NO_AVAILABILITY", FirebaseFirestoreException.Code.ABORTED);
            }

            double price = 0.0;
            Double priceAtBooking = bookingSnapshot.getDouble("priceAtBooking");
            if (priceAtBooking != null) {
                price = priceAtBooking;
            } else if (activitySnapshot.exists()) {
                Double pricePerPerson = activitySnapshot.getDouble("pricePerPerson");
                if (pricePerPerson != null) {
                    price = pricePerPerson;
                }
            } else if (item.pricePerPerson != null) {
                price = item.pricePerPerson;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("participants", participants);
            updates.put("scheduleStart", dayStart);
            updates.put("totalAmount", price * participants);
            updates.put("priceAtBooking", price);
            transaction.update(bookingRef, updates);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    if (e instanceof RuntimeException && e.getCause() instanceof Exception) {
                        callback.onFailure((Exception) e.getCause());
                    } else {
                        callback.onFailure(e);
                    }
                });
    }

    private static void normalizeCalendarToDayStart(@NonNull Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void confirmDeleteActivity(@NonNull BookingItem item) {
        if (!isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.booking_activity_delete_confirm_title)
                .setMessage(R.string.booking_activity_delete_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.booking_action_delete, (dialog, which) -> deleteActivityBooking(item))
                .show();
    }

    private void deleteActivityBooking(@NonNull BookingItem item) {
        FirebaseFirestore.getInstance().collection("bookings")
                .document(item.id)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.booking_activity_delete_success, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showNoAvailabilityDialog() {
        if (!isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.activity_detail_no_availability_dialog_title)
                .setMessage(R.string.activity_detail_no_availability_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private interface ActivityUpdateCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    static class BookingItem {
        String id;
        String roomName;
        String roomImageUrl;
        String status;
        Date checkIn;
        Date checkOut;
        Double totalAmount;
        String kind;
        int participants;
        String activityId;
        Double pricePerPerson;

        static BookingItem from(DocumentSnapshot d) {
            BookingItem i = new BookingItem();
            i.id = d.getId();
            String kind = d.getString("kind");
            String normalizedKind = kind != null ? kind.trim() : "";
            if (!TextUtils.isEmpty(normalizedKind)) {
                normalizedKind = normalizedKind.toUpperCase(Locale.US);
            }
            if (!"ACTIVITY".equals(normalizedKind)) {
                normalizedKind = "ROOM";
            }
            i.kind = normalizedKind;
            if ("ACTIVITY".equals(i.kind)) {
                i.roomName = d.getString("activityName");
                i.roomImageUrl = d.getString("activityImageUrl");
                i.totalAmount = d.getDouble("totalAmount");
                Timestamp start = d.getTimestamp("scheduleStart");
                i.checkIn = start != null ? start.toDate() : null;
                i.checkOut = null;
                i.status = d.getString("status");
                i.activityId = d.getString("activityId");
                Long participants = d.getLong("participants");
                i.participants = participants != null ? participants.intValue() : 0;
                Double priceAtBooking = d.getDouble("priceAtBooking");
                if (priceAtBooking != null) {
                    i.pricePerPerson = priceAtBooking;
                } else if (i.totalAmount != null && i.participants > 0) {
                    i.pricePerPerson = i.totalAmount / i.participants;
                } else {
                    i.pricePerPerson = null;
                }
            } else {
                i.roomName = d.getString("roomName");
                i.roomImageUrl = d.getString("roomImageUrl");
                i.totalAmount = d.getDouble("totalAmount");
                Timestamp ci = d.getTimestamp("checkIn");
                Timestamp co = d.getTimestamp("checkOut");
                i.checkIn = ci != null ? ci.toDate() : null;
                i.checkOut = co != null ? co.toDate() : null;
                i.status = d.getString("status");
                i.activityId = null;
                i.participants = 0;
                i.pricePerPerson = null;
            }
            return i;
        }
    }

    static class BookingAdapter extends RecyclerView.Adapter<VH> {
        interface OnBookingActionListener {
            void onEditActivity(@NonNull BookingItem item);

            void onDeleteActivity(@NonNull BookingItem item);
        }

        private final List<BookingItem> data = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
        private final OnBookingActionListener listener;

        BookingAdapter(OnBookingActionListener listener) {
            this.listener = listener;
        }

        void submit(List<BookingItem> items) {
            data.clear();
            data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            BookingItem b = data.get(position);
            holder.name.setText(b.roomName);
            String dates;
            if (b.checkIn != null && b.checkOut != null) {
                dates = fmt.format(b.checkIn) + " → " + fmt.format(b.checkOut);
            } else if (b.checkIn != null) {
                dates = fmt.format(b.checkIn);
            } else {
                dates = "";
            }
            holder.dates.setText(dates);
            holder.total.setText(String.format(Locale.getDefault(), "Total: LKR %.0f", b.totalAmount == null ? 0.0 : b.totalAmount));
            holder.status.setText(b.status != null ? b.status : "");
            Glide.with(holder.img.getContext()).load(b.roomImageUrl).into(holder.img);

            if ("ACTIVITY".equals(b.kind)) {
                holder.actions.setVisibility(View.VISIBLE);
                holder.btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditActivity(b);
                    }
                });
                holder.btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteActivity(b);
                    }
                });
            } else {
                holder.actions.setVisibility(View.GONE);
                holder.btnEdit.setOnClickListener(null);
                holder.btnDelete.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name;
        TextView dates;
        TextView total;
        TextView status;
        View actions;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            name = v.findViewById(R.id.tvName);
            dates = v.findViewById(R.id.tvDates);
            total = v.findViewById(R.id.tvTotal);
            status = v.findViewById(R.id.tvStatus);
            actions = v.findViewById(R.id.containerActions);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
