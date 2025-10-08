package com.example.resortapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.resortapp.model.Room;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.resortapp.util.NotificationRepository;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RoomDetailActivity extends AppCompatActivity {

    private ImageView img;
    private TextView tvName, tvPrice, tvMeta, tvDesc, tvDates;
    private Button btnPickDates, btnBook;

    private Room room;
    private Long startUtc = null, endUtc = null; // UTC millis

    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        if (bar != null) {
            setSupportActionBar(bar);
            bar.setNavigationOnClickListener(v -> onBackPressed());
        }

        img = findViewById(R.id.img);
        tvName = findViewById(R.id.tvName);
        tvPrice = findViewById(R.id.tvPrice);
        tvMeta = findViewById(R.id.tvMeta);
        tvDesc = findViewById(R.id.tvDesc);
        tvDates = findViewById(R.id.tvDates);
        btnPickDates = findViewById(R.id.btnPickDates);
        btnBook = findViewById(R.id.btnBook);

        String roomId = getIntent().getStringExtra("roomId");
        if (roomId == null) { finish(); return; }

        FirebaseFirestore.getInstance().collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(snap -> {
                    room = snap.toObject(Room.class);
                    if (room == null) { finish(); return; }
                    // ensure id field
                    try {
                        java.lang.reflect.Field f = Room.class.getDeclaredField("id");
                        f.setAccessible(true); f.set(room, snap.getId());
                    } catch (Exception ignored) {}

                    bindRoom();
                })
                .addOnFailureListener(e -> { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); finish(); });

        btnPickDates.setOnClickListener(v -> pickDates());
        btnBook.setOnClickListener(v -> onBookClicked());
    }

    private void bindRoom() {
        String displayName = room.getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = room.getType();
        }
        tvName.setText(displayName);

        double price = room.getBasePrice() == null ? 0.0 : room.getBasePrice();
        tvPrice.setText(String.format(Locale.getDefault(), "LKR %,.0f / night", price));

        StringBuilder metaBuilder = new StringBuilder();
        if (room.getType() != null && !room.getType().trim().isEmpty()) {
            metaBuilder.append(room.getType().trim());
        }
        if (room.getCapacity() != null && room.getCapacity() > 0) {
            if (metaBuilder.length() > 0) metaBuilder.append(" • ");
            metaBuilder.append("Sleeps ").append(room.getCapacity());
        }
        if (metaBuilder.length() > 0) {
            tvMeta.setText(metaBuilder.toString());
            tvMeta.setVisibility(View.VISIBLE);
        } else {
            tvMeta.setVisibility(View.GONE);
        }

        String description = room.getDescription();
        tvDesc.setText(description != null && !description.trim().isEmpty()
                ? description
                : getString(R.string.room_detail_no_description));

        Glide.with(this).load(room.getImageUrl()).placeholder(R.drawable.placeholder_room).into(img);
    }

    private void pickDates() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select dates");
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> dp = builder.build();
        dp.addOnPositiveButtonClickListener(sel -> {
            if (sel == null) return;
            startUtc = sel.first;
            endUtc   = sel.second;
            tvDates.setText(fmt.format(new Date(startUtc)) + " → " + fmt.format(new Date(endUtc)));
        });
        dp.show(getSupportFragmentManager(), "range");
    }

    private void onBookClicked() {
        if (room == null) return;
        if (startUtc == null || endUtc == null) {
            Toast.makeText(this, "Please select dates", Toast.LENGTH_SHORT).show(); return;
        }

        long nights = TimeUnit.MILLISECONDS.toDays(endUtc - startUtc);
        if (nights <= 0) {
            Toast.makeText(this, "Invalid date range", Toast.LENGTH_SHORT).show(); return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show(); return; }

        double price = room.getBasePrice() == null ? 0.0 : room.getBasePrice();
        double total = nights * price;

        showPaymentDialog(uid, nights, price, total);
    }

    private void showPaymentDialog(String uid, long nights, double pricePerNight, double total) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_method, null);
        if (dialogView == null) return;

        RadioGroup paymentGroup = dialogView.findViewById(R.id.paymentMethodGroup);
        View cardDetailsGroup = dialogView.findViewById(R.id.cardDetailsGroup);
        TextView tvSummary = dialogView.findViewById(R.id.tvPaymentSummary);
        TextView tvBaseAmount = dialogView.findViewById(R.id.tvBaseAmount);
        TextView tvDiscountAmount = dialogView.findViewById(R.id.tvDiscountAmount);
        TextView tvTotalDue = dialogView.findViewById(R.id.tvTotalDue);
        TextView tvGreenContribution = dialogView.findViewById(R.id.tvGreenContribution);
        TextInputLayout tilCardName = dialogView.findViewById(R.id.tilCardName);
        TextInputLayout tilCardNumber = dialogView.findViewById(R.id.tilCardNumber);
        TextInputLayout tilExpiry = dialogView.findViewById(R.id.tilExpiry);
        TextInputLayout tilCvv = dialogView.findViewById(R.id.tilCvv);
        TextInputEditText etCardName = dialogView.findViewById(R.id.etCardName);
        TextInputEditText etCardNumber = dialogView.findViewById(R.id.etCardNumber);
        TextInputEditText etExpiry = dialogView.findViewById(R.id.etExpiry);
        TextInputEditText etCvv = dialogView.findViewById(R.id.etCvv);
        TextInputLayout tilPromoCode = dialogView.findViewById(R.id.tilPromoCode);
        TextInputEditText etPromoCode = dialogView.findViewById(R.id.etPromoCode);
        MaterialButton btnApplyPromo = dialogView.findViewById(R.id.btnApplyPromo);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        CircularProgressIndicator progressBooking = dialogView.findViewById(R.id.progressBooking);
        View btnClose = dialogView.findViewById(R.id.btnClose);
        SwitchMaterial switchGreenStay = dialogView.findViewById(R.id.switchGreenStay);

        if (paymentGroup == null || cardDetailsGroup == null || tvSummary == null ||
                tvBaseAmount == null || tvDiscountAmount == null || tvTotalDue == null ||
                tvGreenContribution == null ||
                tilCardName == null || tilCardNumber == null || tilExpiry == null || tilCvv == null ||
                etCardName == null || etCardNumber == null || etExpiry == null || etCvv == null ||
                tilPromoCode == null || etPromoCode == null || btnApplyPromo == null ||
                btnConfirm == null || btnClose == null || switchGreenStay == null ||
                progressBooking == null) {
            return;
        }

        String summaryText = String.format(Locale.getDefault(),
                getString(R.string.payment_dialog_summary_format),
                nights,
                nights == 1 ? "" : "s");
        tvSummary.setText(summaryText);

        final double baseTotal = total;
        final double discountRate = 0.10;
        final boolean[] promoApplied = {false};
        final double[] amountDue = {baseTotal};
        final boolean[] contributeGreen = {false};
        final double greenContributionAmount = 500.0;

        Runnable updateAmounts = () -> {
            double discount = promoApplied[0] ? baseTotal * discountRate : 0.0;
            double greenContribution = contributeGreen[0] ? greenContributionAmount : 0.0;
            amountDue[0] = Math.max(0.0, baseTotal - discount) + greenContribution;
            tvBaseAmount.setText(String.format(Locale.getDefault(),
                    getString(R.string.payment_dialog_base_amount), baseTotal));
            tvDiscountAmount.setText(String.format(Locale.getDefault(),
                    getString(R.string.payment_dialog_discount_amount), discount));
            if (greenContribution > 0) {
                tvGreenContribution.setVisibility(View.VISIBLE);
                tvGreenContribution.setText(String.format(Locale.getDefault(),
                        getString(R.string.payment_dialog_green_amount), greenContribution));
            } else {
                tvGreenContribution.setVisibility(View.GONE);
            }
            tvTotalDue.setText(String.format(Locale.getDefault(),
                    getString(R.string.payment_dialog_total_due), amountDue[0]));
        };
        updateAmounts.run();

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setDismissWithAnimation(true);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        RadioGroup.OnCheckedChangeListener listener = (group, checkedId) -> {
            boolean payingByCard = checkedId == R.id.optionPayByCard;
            cardDetailsGroup.setVisibility(payingByCard ? View.VISIBLE : View.GONE);
            btnConfirm.setText(payingByCard
                    ? getString(R.string.payment_dialog_positive_card)
                    : getString(R.string.payment_dialog_positive_cash));
        };
        paymentGroup.setOnCheckedChangeListener(listener);

        if (paymentGroup.getCheckedRadioButtonId() == -1) {
            paymentGroup.check(R.id.optionPayAtHotel);
        } else {
            listener.onCheckedChanged(paymentGroup, paymentGroup.getCheckedRadioButtonId());
        }

        switchGreenStay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            contributeGreen[0] = isChecked;
            updateAmounts.run();
        });

        btnApplyPromo.setOnClickListener(v -> {
            String code = getTextFromField(etPromoCode);
            if (TextUtils.isEmpty(code)) {
                tilPromoCode.setError(getString(R.string.payment_dialog_apply_promo_error));
                return;
            }

            tilPromoCode.setError(null);
            promoApplied[0] = true;
            updateAmounts.run();
            btnApplyPromo.setText(getString(R.string.payment_dialog_apply_promo_applied));
            btnApplyPromo.setEnabled(false);
            etPromoCode.setEnabled(false);
            Toast.makeText(this, R.string.payment_dialog_apply_promo_success, Toast.LENGTH_SHORT).show();
        });

        btnConfirm.setOnClickListener(v -> {
            int selectedId = paymentGroup.getCheckedRadioButtonId();
            boolean payingByCard = selectedId == R.id.optionPayByCard;

            tilCardName.setError(null);
            tilCardNumber.setError(null);
            tilExpiry.setError(null);
            tilCvv.setError(null);

            if (payingByCard) {
                String nameValue = getTextFromField(etCardName);
                String cardNumberValue = getTextFromField(etCardNumber);
                String expiryValue = getTextFromField(etExpiry);
                String cvvValue = getTextFromField(etCvv);

                boolean valid = true;

                if (!isValidCardHolderName(nameValue)) {
                    tilCardName.setError(getString(R.string.error_card_name_invalid));
                    valid = false;
                }
                if (!isValidCardNumber(cardNumberValue)) {
                    tilCardNumber.setError(getString(R.string.error_card_number_invalid));
                    valid = false;
                }
                if (!isValidExpiry(expiryValue)) {
                    tilExpiry.setError(getString(R.string.error_card_expiry_invalid));
                    valid = false;
                }
                if (!isValidCvv(cvvValue)) {
                    tilCvv.setError(getString(R.string.error_card_cvv_invalid));
                    valid = false;
                }

                if (!valid) {
                    return;
                }

                attemptBooking(uid, nights, pricePerNight, amountDue[0],
                        "CARD", "PAID", btnConfirm, progressBooking, dialog);
            } else {
                attemptBooking(uid, nights, pricePerNight, amountDue[0],
                        "PAY_AT_HOTEL", "PENDING", btnConfirm, progressBooking, dialog);
            }
        });

        dialog.show();
    }

    private void attemptBooking(String uid,
                                long nights,
                                double pricePerNight,
                                double total,
                                String paymentMethod,
                                String paymentStatus,
                                MaterialButton btnConfirm,
                                CircularProgressIndicator progressBooking,
                                BottomSheetDialog dialog) {
        btnConfirm.setEnabled(false);
        progressBooking.setVisibility(View.VISIBLE);
        progressBooking.setIndeterminate(true);

        Timestamp checkInTs = new Timestamp(new Date(startUtc));
        Timestamp checkOutTs = new Timestamp(new Date(endUtc));

        final int maxCapacity = resolveRoomInventoryCapacity();
        if (maxCapacity <= 0) {
            progressBooking.setVisibility(View.GONE);
            btnConfirm.setEnabled(true);
            showNoAvailabilityDialog();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference newBookingRef = db.collection("bookings").document();
        db.runTransaction(transaction -> {
                    Query query = db.collection("bookings")
                            .whereEqualTo("status", "CONFIRMED")
                            .whereEqualTo("roomId", room.getId())
                            .whereLessThan("checkIn", checkOutTs);

                    QuerySnapshot prefetch;
                    try {
                        prefetch = Tasks.await(query.get());
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    int overlapping = 0;
                    for (DocumentSnapshot doc : prefetch.getDocuments()) {
                        DocumentSnapshot existing = transaction.get(doc.getReference());
                        Timestamp existingCheckIn = existing.getTimestamp("checkIn");
                        Timestamp existingCheckOut = existing.getTimestamp("checkOut");
                        if (existingCheckIn == null || existingCheckOut == null) continue;

                        long existingStart = existingCheckIn.toDate().getTime();
                        long existingEnd = existingCheckOut.toDate().getTime();
                        if (existingStart < endUtc && existingEnd > startUtc) {
                            overlapping++;
                        }
                    }

                    if (overlapping >= maxCapacity) {
                        throw new FirebaseFirestoreException(
                                "NO_AVAILABILITY",
                                FirebaseFirestoreException.Code.ABORTED);
                    }

                    transaction.set(newBookingRef,
                            buildBookingPayload(uid, nights, pricePerNight, total, paymentMethod,
                                    paymentStatus, checkInTs, checkOutTs));
                    return null;
                })
                .addOnSuccessListener(ignored -> {
                    progressBooking.setVisibility(View.GONE);
                    Toast.makeText(this, "Booked! See in My Bookings.", Toast.LENGTH_LONG).show();
                    NotificationRepository.getInstance().recordRoomEvent(
                            uid,
                            newBookingRef.getId(),
                            room.getId(),
                            room.getName() != null ? room.getName() : room.getType(),
                            checkInTs,
                            checkOutTs,
                            "CONFIRMED",
                            "BOOKED");
                    dialog.dismiss();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    progressBooking.setVisibility(View.GONE);
                    if (e instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
                        if (ffe.getCode() == FirebaseFirestoreException.Code.ABORTED &&
                                "NO_AVAILABILITY".equals(ffe.getMessage())) {
                            showNoAvailabilityDialog();
                            return;
                        }
                    }
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private int resolveRoomInventoryCapacity() {
        if (room == null) {
            return 0;
        }
        Integer capacity = room.getCapacity();
        if (capacity == null) {
            return 0;
        }
        return Math.max(0, capacity);
    }

    private void showNoAvailabilityDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.room_detail_no_availability_dialog_title)
                .setMessage(R.string.room_detail_no_availability_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String getTextFromField(TextInputEditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private boolean isValidCardHolderName(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() < 3) {
            return false;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 2) {
            return false;
        }
        for (String part : parts) {
            if (!part.matches("[\\p{L}.'-]{1,}") ) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidCardNumber(String cardNumber) {
        if (TextUtils.isEmpty(cardNumber)) {
            return false;
        }
        String digitsOnly = cardNumber.replaceAll("[\\s-]", "");
        if (!digitsOnly.matches("\\d{13,19}")) {
            return false;
        }
        return passesLuhnCheck(digitsOnly);
    }

    private boolean passesLuhnCheck(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private boolean isValidExpiry(String expiry) {
        if (TextUtils.isEmpty(expiry)) {
            return false;
        }
        String trimmed = expiry.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(0[1-9]|1[0-2])\\s*/\\s*(\\d{2})$")
                .matcher(trimmed);
        if (!matcher.matches()) {
            return false;
        }
        int month = Integer.parseInt(matcher.group(1));
        int year = Integer.parseInt(matcher.group(2)) + 2000;

        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_MONTH, 1);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        Calendar expiryCal = Calendar.getInstance();
        expiryCal.set(Calendar.DAY_OF_MONTH, 1);
        expiryCal.set(Calendar.HOUR_OF_DAY, 0);
        expiryCal.set(Calendar.MINUTE, 0);
        expiryCal.set(Calendar.SECOND, 0);
        expiryCal.set(Calendar.MILLISECOND, 0);
        expiryCal.set(Calendar.YEAR, year);
        expiryCal.set(Calendar.MONTH, month - 1);
        expiryCal.add(Calendar.MONTH, 1);

        return expiryCal.after(now);
    }

    private boolean isValidCvv(String cvv) {
        if (TextUtils.isEmpty(cvv)) {
            return false;
        }
        String digitsOnly = cvv.trim();
        return digitsOnly.matches("\\d{3,4}");
    }

    private Map<String, Object> buildBookingPayload(String uid,
                                                    long nights,
                                                    double pricePerNight,
                                                    double total,
                                                    String paymentMethod,
                                                    String paymentStatus,
                                                    Timestamp checkInTs,
                                                    Timestamp checkOutTs) {
        Map<String, Object> b = new HashMap<>();
        b.put("kind", "ROOM");
        b.put("userId", uid);
        b.put("roomId", room.getId());
        b.put("roomName", room.getName() != null ? room.getName() : room.getType());
        b.put("roomImageUrl", room.getImageUrl());
        b.put("priceAtBooking", pricePerNight);
        b.put("checkIn", checkInTs);
        b.put("checkOut", checkOutTs);
        b.put("nights", nights);
        b.put("totalAmount", total);
        b.put("status", "CONFIRMED");
        b.put("paymentMethod", paymentMethod);
        b.put("paymentStatus", paymentStatus);
        b.put("createdAt", FieldValue.serverTimestamp());
        return b;
    }
}
