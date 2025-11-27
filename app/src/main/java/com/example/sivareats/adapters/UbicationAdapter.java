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

        void onFavoriteClick(Ubicacion u);
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
        
        // Actualizar icono de estrella según si es preferida o no
        if (u.isPreferida()) {
            holder.btnFavorite.setImageResource(R.drawable.ic_star);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_border);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(u);
        });
        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClick(u);
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
        ImageButton btnFavorite, btnEdit, btnDelete;

        public Holder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
