package com.example.resortapp;

import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.resortapp.model.EcoInfo;

import java.util.*;

public class EcoInfoCardAdapter extends RecyclerView.Adapter<EcoInfoCardAdapter.VH> {

    public interface OnEcoClick { void onClick(EcoInfo info); }
    private final List<EcoInfo> items = new ArrayList<>();
    private OnEcoClick onClick;

    public void setOnEcoClick(OnEcoClick cb) { onClick = cb; }
    public void submit(List<EcoInfo> list) { items.clear(); items.addAll(list); notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_eco_info_card, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        EcoInfo e = items.get(pos);
        h.title.setText(e.getTitle());
        h.subtitle.setText(e.getSubtitle());
        Glide.with(h.img.getContext()).load(e.getImageUrl()).into(h.img);
        h.itemView.setOnClickListener(v -> { if (onClick != null) onClick.onClick(e); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img; TextView title, subtitle;
        VH(@NonNull View v) { super(v);
            img = v.findViewById(R.id.img);
            title = v.findViewById(R.id.tvTitle);
            subtitle = v.findViewById(R.id.tvSubtitle);
        }
    }
}
