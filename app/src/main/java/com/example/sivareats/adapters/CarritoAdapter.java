package com.example.sivareats.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.cart.CartDao;
import com.example.sivareats.data.cart.CartItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.ViewHolder> {

    private List<CartItem> listaCart;
    private OnCantidadChangeListener listener;

    private CartDao cartDao;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public CarritoAdapter(Context context, List<CartItem> lista) {
        this.listaCart = lista;
        this.cartDao = AppDatabase.getInstance(context).cartDao();
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
        CartItem item = listaCart.get(position);

        holder.tvNombre.setText(item.getNombre());
        holder.tvDescripcion.setText(item.getDescripcion());
        holder.tvPrecio.setText("$" + String.format("%.2f", item.getPrecio()));
        holder.tvCantidad.setText(String.valueOf(item.getCantidad()));

        holder.imgProducto.setImageResource(item.getImageResId());

        // SUMAR cantidad
        holder.btnSumar.setOnClickListener(v -> {
            int nuevaCantidad = item.getCantidad() + 1;
            item.setCantidad(nuevaCantidad);
            holder.tvCantidad.setText(String.valueOf(nuevaCantidad));

            executor.execute(() -> cartDao.update(item));

            if (listener != null) listener.onCantidadChanged();
        });

        // RESTAR cantidad
        holder.btnRestar.setOnClickListener(v -> {
            int cantidadActual = item.getCantidad();

            if (cantidadActual > 1) {
                int nuevaCantidad = cantidadActual - 1;
                item.setCantidad(nuevaCantidad);
                holder.tvCantidad.setText(String.valueOf(nuevaCantidad));

                executor.execute(() -> cartDao.update(item));

                if (listener != null) listener.onCantidadChanged();

            } else {
                int index = holder.getAdapterPosition();

                executor.execute(() -> cartDao.delete(item));

                listaCart.remove(index);
                notifyItemRemoved(index);

                if (listener != null) listener.onCantidadChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaCart.size();
    }

    // ← Necesario para CartFragment
    public List<CartItem> getLista() {
        return listaCart;
    }

    public void updateList(List<CartItem> nuevaLista) {
        this.listaCart = nuevaLista;
        notifyDataSetChanged();
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

    public void setOnCantidadChangeListener(OnCantidadChangeListener listener) {
        this.listener = listener;
    }
}
