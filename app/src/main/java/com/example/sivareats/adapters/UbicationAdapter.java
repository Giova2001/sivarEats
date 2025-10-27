package com.example.sivareats.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sivareats.R;
import com.example.sivareats.data.Ubicacion;

import java.util.ArrayList;
import java.util.List;

public class UbicationAdapter extends RecyclerView.Adapter<UbicationAdapter.Holder> {

    public interface OnItemClickListener {
        void onEdit(Ubicacion u);
        void onDelete(Ubicacion u);
        void onClick(Ubicacion u);
    }

    private List<Ubicacion> lista = new ArrayList<>();
    private final Context context;
    private final OnItemClickListener listener;

    public UbicationAdapter(List<Ubicacion> inicial, Context context, OnItemClickListener listener) {
        if (inicial != null) this.lista = inicial;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_location_item, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Ubicacion u = lista.get(position);
        holder.tvTitle.setText(u.getNombreLugar());
        holder.tvAddress.setText(u.getDireccion());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(u);
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(u);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(u);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void setUbicaciones(List<Ubicacion> nuevas) {
        this.lista = nuevas != null ? nuevas : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAddress;
        ImageButton btnEdit, btnDelete;

        public Holder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
