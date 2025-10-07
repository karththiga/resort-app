package com.example.resortapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.resortapp.model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.VH> {

    private static final int DEFAULT_STOCK = 5;

    public enum LayoutMode {
        LIST,
        CARD
    }

    public interface OnRoomClick {
        void onClick(Room room);
    }

    private final List<Room> items = new ArrayList<>();
    private final LayoutMode layoutMode;
    private OnRoomClick onRoomClick;

    public void setOnRoomClick(OnRoomClick cb) {
        this.onRoomClick = cb;
    }

    public RoomListAdapter() {
        this(LayoutMode.LIST);
    }

    public RoomListAdapter(LayoutMode layoutMode) {
        this.layoutMode = layoutMode;
        setHasStableIds(true);
    }

    public void submit(List<Room> newItems) {
        List<Room> old = new ArrayList<>(items);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new Diff(old, newItems));
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        Room r = items.get(position);
        return r.getId() != null ? r.getId().hashCode() : RecyclerView.NO_ID;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = layoutMode == LayoutMode.CARD
                ? R.layout.item_room_card
                : R.layout.item_room_list;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Room r = items.get(position);
        h.name.setText(r.getName() != null ? r.getName() : r.getType());
        if (h.desc != null) {
            h.desc.setText(r.getDescription());
            h.desc.setVisibility(r.getDescription() != null && !r.getDescription().isEmpty()
                    ? View.VISIBLE : View.GONE);
        }
        double price = r.getBasePrice() == null ? 0.0 : r.getBasePrice();
        h.price.setText(String.format("LKR %.0f / night", price));
        Glide.with(h.img.getContext()).load(r.getImageUrl()).into(h.img);

        int availableRooms = r.getAvailableRooms() != null ? r.getAvailableRooms() : DEFAULT_STOCK;
        boolean soldOut = r.isSoldOut() || availableRooms <= 0;
        if (soldOut) availableRooms = 0;

        if (h.availability != null) {
            if (soldOut) {
                h.availability.setText(h.itemView.getContext().getString(R.string.rooms_sold_out_label));
                h.availability.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.rooms_sold_out));
            } else {
                h.availability.setText(h.itemView.getResources().getQuantityString(
                        R.plurals.rooms_availability_count,
                        availableRooms,
                        availableRooms));
                h.availability.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.green_dark));
            }
            h.availability.setVisibility(View.VISIBLE);
        }

        if (h.tapHint != null) {
            if (soldOut) {
                h.tapHint.setVisibility(View.GONE);
            } else {
                h.tapHint.setText(R.string.rooms_tap_hint);
                h.tapHint.setVisibility(View.VISIBLE);
            }
        }

        View.OnClickListener go = v -> {
            if (onRoomClick != null) onRoomClick.onClick(r);
        };
        if (soldOut) {
            h.itemView.setOnClickListener(null);
        } else {
            h.itemView.setOnClickListener(go);
        }
        h.itemView.setEnabled(!soldOut);
        h.itemView.setClickable(!soldOut);
        h.itemView.setFocusable(!soldOut);
        h.itemView.setAlpha(soldOut ? 0.5f : 1f);


//        h.name.setText(r.getName() != null ? r.getName() : r.getType());
//        double price = r.getBasePrice() == null ? 0.0 : r.getBasePrice();
//        h.price.setText(String.format("LKR %.0f / night", price));
//        Glide.with(h.img.getContext()).load(r.getImageUrl()).into(h.img);
//        h.itemView.setOnClickListener(v -> { if (onRoomClick != null) onRoomClick.onClick(r); });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, price;
        TextView desc;
        TextView availability;
        TextView tapHint;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            name = v.findViewById(R.id.tvName);
            price = v.findViewById(R.id.tvPrice);
            desc = v.findViewById(R.id.tvDesc);
            availability = v.findViewById(R.id.tvAvailability);
            tapHint = v.findViewById(R.id.tvTapHint);
        }
    }

    static class Diff extends DiffUtil.Callback {
        private final List<Room> oldL, newL;

        Diff(List<Room> oldL, List<Room> newL) {
            this.oldL = oldL;
            this.newL = newL;
        }

        @Override
        public int getOldListSize() {
            return oldL.size();
        }

        @Override
        public int getNewListSize() {
            return newL.size();
        }

        @Override
        public boolean areItemsTheSame(int o, int n) {
            String oid = oldL.get(o).getId(), nid = newL.get(n).getId();
            return oid != null && oid.equals(nid);
        }

        @Override
        public boolean areContentsTheSame(int o, int n) {
            Room a = oldL.get(o), b = newL.get(n);
            return Objects.equals(a.getName(), b.getName())
                    && Objects.equals(a.getType(), b.getType())
                    && Objects.equals(a.getImageUrl(), b.getImageUrl())
                    && Objects.equals(a.getBasePrice(), b.getBasePrice())
                    && Objects.equals(a.getStatus(), b.getStatus())
                    && Objects.equals(a.getAvailableRooms(), b.getAvailableRooms())
                    && a.isSoldOut() == b.isSoldOut();
        }
    }
}
