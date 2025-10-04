package com.example.resortapp;

import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.resortapp.model.EcoInfo;

import java.util.*;

public class EcoBulletAdapter extends RecyclerView.Adapter<EcoBulletAdapter.VH> {
    public interface OnClick { void onClick(EcoInfo info); }
    private final List<EcoInfo> data = new ArrayList<>();
    private OnClick cb;

    public void setOnClick(OnClick c) { cb = c; }
    public void submit(List<EcoInfo> list) { data.clear(); data.addAll(list); notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_eco_bullet, p, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        EcoInfo e = data.get(pos);
        h.title.setText(e.getTitle());
        h.subtitle.setText(e.getSubtitle() != null ? e.getSubtitle() : "");
        h.desc.setText(e.getDescription() != null ? e.getDescription() : "");
        Glide.with(h.img.getContext()).load(e.getImageUrl()).into(h.img);
        h.itemView.setOnClickListener(v -> { if (cb != null) cb.onClick(e); });
    }
    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img; TextView title, subtitle, desc;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            title = v.findViewById(R.id.tvTitle);
            subtitle = v.findViewById(R.id.tvSubtitle);
            desc = v.findViewById(R.id.tvDesc);
        }
    }
}
