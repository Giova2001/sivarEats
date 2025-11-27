package com.example.sivareats.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sivareats.R;
import com.example.sivareats.model.Pedido;
import com.example.sivareats.ui.ordenes.RastreandoPedidoActivity;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrdenesAdapter extends RecyclerView.Adapter<OrdenesAdapter.ViewHolder> {

    private List<Pedido> listaPedidos;
    private OnPedidoClickListener listener;

    public interface OnPedidoClickListener {
        void onRastrearClick(Pedido pedido);
        void onVerDetallesClick(Pedido pedido);
    }

    public OrdenesAdapter(List<Pedido> lista) {
        this.listaPedidos = lista;
    }

    public void setOnPedidoClickListener(OnPedidoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_orden, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pedido pedido = listaPedidos.get(position);

        // Estado
        holder.tvEstado.setText(pedido.getEstado().equals("activo") ? "En curso" : "Completado");
        
        // Fecha
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvFecha.setText(sdf.format(pedido.getFecha()));

        // Restaurante
        holder.tvRestaurante.setText(pedido.getRestaurante());

        // Productos
        StringBuilder productosStr = new StringBuilder();
        if (pedido.getProductos() != null && !pedido.getProductos().isEmpty()) {
            for (int i = 0; i < pedido.getProductos().size(); i++) {
                productosStr.append(pedido.getProductos().get(i).getNombre());
                if (i < pedido.getProductos().size() - 1) {
                    productosStr.append(", ");
                }
            }
        }
        holder.tvProductos.setText(productosStr.toString());

        // Dirección
        holder.tvDireccion.setText(pedido.getDireccion());

        // Total
        holder.tvTotal.setText("$" + String.format(Locale.getDefault(), "%.2f", pedido.getTotal()));

        // Configurar botones según el estado
        if (pedido.getEstado().equals("activo")) {
            holder.btnRastrear.setVisibility(View.VISIBLE);
            holder.btnVerDetalles.setVisibility(View.GONE);
            holder.tvTiempoEstimado.setVisibility(View.VISIBLE);
            if (pedido.getTiempoEstimado() > 0) {
                holder.tvTiempoEstimado.setText("Llegando en " + pedido.getTiempoEstimado() + " minutos");
            }
        } else {
            holder.btnRastrear.setVisibility(View.GONE);
            holder.btnVerDetalles.setVisibility(View.VISIBLE);
            holder.tvTiempoEstimado.setVisibility(View.GONE);
        }

        // Click en rastrear
        holder.btnRastrear.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRastrearClick(pedido);
            } else {
                // Fallback: abrir directamente la actividad
                Intent intent = new Intent(v.getContext(), RastreandoPedidoActivity.class);
                intent.putExtra("pedido_id", pedido.getId());
                v.getContext().startActivity(intent);
            }
        });

        // Click en ver detalles
        holder.btnVerDetalles.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVerDetallesClick(pedido);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaPedidos != null ? listaPedidos.size() : 0;
    }

    public void updateList(List<Pedido> nuevaLista) {
        this.listaPedidos = nuevaLista;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEstado, tvFecha, tvRestaurante, tvProductos, tvDireccion, tvTotal, tvTiempoEstimado;
        MaterialButton btnRastrear, btnVerDetalles;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvRestaurante = itemView.findViewById(R.id.tvRestaurante);
            tvProductos = itemView.findViewById(R.id.tvProductos);
            tvDireccion = itemView.findViewById(R.id.tvDireccion);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvTiempoEstimado = itemView.findViewById(R.id.tvTiempoEstimado);
            btnRastrear = itemView.findViewById(R.id.btnRastrear);
            btnVerDetalles = itemView.findViewById(R.id.btnVerDetalles);
        }
    }
}




