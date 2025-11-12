package com.example.sivareats.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sivareats.R;
import com.example.sivareats.model.Producto;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.ViewHolder> {

    private List<Producto> listaProductos;

    public CarritoAdapter(List<Producto> lista) {
        this.listaProductos = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carrito, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Producto producto = listaProductos.get(position);

        holder.tvNombre.setText(producto.getNombre());
        holder.tvDescripcion.setText(producto.getDescripcion());
        holder.tvPrecio.setText("$" + producto.getPrecio());
        holder.tvCantidad.setText(String.valueOf(producto.getCantidad()));
        holder.imgProducto.setImageResource(producto.getImagenResId());

        // Cargar imagen local
        holder.imgProducto.setImageResource(producto.getImagenResId());

        // Botón sumar
        holder.btnSumar.setOnClickListener(v -> {
            producto.setCantidad(producto.getCantidad() + 1);
            holder.tvCantidad.setText(String.valueOf(producto.getCantidad()));
            if (listener != null) listener.onCantidadChanged();
        });

        // Botón restar
        holder.btnRestar.setOnClickListener(v -> {
            int cantidad = producto.getCantidad();
            if (cantidad > 1) {
                producto.setCantidad(cantidad - 1);
                holder.tvCantidad.setText(String.valueOf(producto.getCantidad()));
                if (listener != null) listener.onCantidadChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaProductos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProducto;
        TextView tvNombre, tvDescripcion, tvPrecio, tvCantidad;
        MaterialButton btnSumar, btnRestar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProducto = itemView.findViewById(R.id.imgProducto);
            tvNombre = itemView.findViewById(R.id.tvNombreProducto);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionProducto);
            tvPrecio = itemView.findViewById(R.id.tvPrecioProducto);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            btnSumar = itemView.findViewById(R.id.btnSumar);
            btnRestar = itemView.findViewById(R.id.btnRestar);
        }

    }
    public interface OnCantidadChangeListener {
        void onCantidadChanged();
    }

    private OnCantidadChangeListener listener;

    public void setOnCantidadChangeListener(OnCantidadChangeListener listener) {
        this.listener = listener;
    }
}
