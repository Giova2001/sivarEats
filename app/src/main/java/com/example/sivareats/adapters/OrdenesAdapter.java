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
    private boolean isRestaurante;

    public interface OnPedidoClickListener {
        void onRastrearClick(Pedido pedido);
        void onVerDetallesClick(Pedido pedido);
    }

    public OrdenesAdapter(List<Pedido> lista) {
        this.listaPedidos = lista;
        this.isRestaurante = false;
    }
    
    public OrdenesAdapter(List<Pedido> lista, boolean isRestaurante) {
        this.listaPedidos = lista;
        this.isRestaurante = isRestaurante;
    }

    public void setOnPedidoClickListener(OnPedidoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Si es restaurante, usar layout especial
        int layoutId = isRestaurante ? R.layout.item_orden_restaurante : R.layout.item_orden;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(view, isRestaurante);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pedido pedido = listaPedidos.get(position);

        // Estado
        String estadoTexto;
        if (isRestaurante) {
            if ("pendiente".equals(pedido.getEstado())) {
                estadoTexto = "Pendiente";
            } else if ("preparacion".equals(pedido.getEstado())) {
                estadoTexto = "En preparación";
            } else {
                estadoTexto = "Completado";
            }
        } else {
            estadoTexto = pedido.getEstado().equals("activo") || pedido.getEstado().equals("preparacion") ? "En curso" : "Completado";
        }
        holder.tvEstado.setText(estadoTexto);
        
        // Fecha
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvFecha.setText(sdf.format(pedido.getFecha()));

        // Restaurante o Cliente (dependiendo del tipo de usuario)
        if (isRestaurante && holder.tvClienteNombre != null) {
            // Para restaurantes, mostrar información del cliente
            String clienteEmail = pedido.getClienteEmail();
            if (clienteEmail != null && !clienteEmail.isEmpty() && clienteEmail.contains("@")) {
                holder.tvClienteNombre.setText(clienteEmail.split("@")[0]);
                if (holder.tvClienteEmail != null) {
                    holder.tvClienteEmail.setText(clienteEmail);
                }
            } else {
                holder.tvClienteNombre.setText("Cliente");
                if (holder.tvClienteEmail != null) {
                    holder.tvClienteEmail.setText("");
                }
            }
        } else {
            // Para usuarios normales, mostrar restaurante
            if (holder.tvRestaurante != null) {
                holder.tvRestaurante.setText(pedido.getRestaurante());
            }
        }

        // Productos
        StringBuilder productosStr = new StringBuilder();
        int totalUnidades = 0;
        if (pedido.getProductos() != null && !pedido.getProductos().isEmpty()) {
            for (int i = 0; i < pedido.getProductos().size(); i++) {
                productosStr.append(pedido.getProductos().get(i).getNombre());
                totalUnidades += pedido.getProductos().get(i).getCantidad();
                if (i < pedido.getProductos().size() - 1) {
                    productosStr.append(", ");
                }
            }
        }
        holder.tvProductos.setText(productosStr.toString());
        
        // Mostrar cantidad total si es restaurante
        if (isRestaurante && holder.tvCantidad != null) {
            holder.tvCantidad.setText(totalUnidades + " unidades");
        }

        // Dirección
        holder.tvDireccion.setText(pedido.getDireccion());

        // Total
        holder.tvTotal.setText("$" + String.format(Locale.getDefault(), "%.2f", pedido.getTotal()));

        // Configurar botones según el estado y tipo de usuario
        if (isRestaurante) {
            // Para restaurantes
            if ("pendiente".equals(pedido.getEstado())) {
                // Mostrar botón "Aceptar pedido"
                if (holder.btnAceptarPedido != null) {
                    holder.btnAceptarPedido.setVisibility(View.VISIBLE);
                    holder.btnAceptarPedido.setText("Aceptar pedido");
                }
                if (holder.btnDetalles != null) {
                    holder.btnDetalles.setVisibility(View.VISIBLE);
                }
                if (holder.btnRastrear != null) {
                    holder.btnRastrear.setVisibility(View.GONE);
                }
            } else if ("preparacion".equals(pedido.getEstado())) {
                // Mostrar "En preparación"
                if (holder.btnAceptarPedido != null) {
                    holder.btnAceptarPedido.setVisibility(View.GONE);
                }
                if (holder.btnRastrear != null) {
                    holder.btnRastrear.setVisibility(View.VISIBLE);
                    holder.btnRastrear.setText("Ver detalles");
                }
            }
        } else {
            // Para usuarios normales
            if (pedido.getEstado().equals("activo") || pedido.getEstado().equals("preparacion")) {
                if (holder.btnRastrear != null) {
                    holder.btnRastrear.setVisibility(View.VISIBLE);
                }
                if (holder.btnVerDetalles != null) {
                    holder.btnVerDetalles.setVisibility(View.GONE);
                }
                if (holder.tvTiempoEstimado != null) {
                    holder.tvTiempoEstimado.setVisibility(View.VISIBLE);
                    if (pedido.getTiempoEstimado() > 0) {
                        holder.tvTiempoEstimado.setText("Llegando en " + pedido.getTiempoEstimado() + " minutos");
                    }
                }
            } else {
                if (holder.btnRastrear != null) {
                    holder.btnRastrear.setVisibility(View.GONE);
                }
                if (holder.btnVerDetalles != null) {
                    holder.btnVerDetalles.setVisibility(View.VISIBLE);
                }
                if (holder.tvTiempoEstimado != null) {
                    holder.tvTiempoEstimado.setVisibility(View.GONE);
                }
            }
        }

        // Click en botones
        if (holder.btnAceptarPedido != null) {
            holder.btnAceptarPedido.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRastrearClick(pedido); // Reutilizar para abrir RevisarPedidoActivity
                }
            });
        }
        
        if (holder.btnDetalles != null) {
            holder.btnDetalles.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRastrearClick(pedido); // Reutilizar para abrir RevisarPedidoActivity
                }
            });
        }
        
        if (holder.btnRastrear != null) {
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
        }

        // Click en ver detalles
        if (holder.btnVerDetalles != null) {
            holder.btnVerDetalles.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVerDetallesClick(pedido);
                }
            });
        }
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
        TextView tvClienteNombre, tvClienteEmail, tvCantidad; // Para restaurantes
        MaterialButton btnRastrear, btnVerDetalles, btnAceptarPedido, btnDetalles;

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
            
            // Views para restaurantes (pueden ser null si no es restaurante)
            tvClienteNombre = itemView.findViewById(R.id.tvClienteNombre);
            tvClienteEmail = itemView.findViewById(R.id.tvClienteEmail);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            btnAceptarPedido = itemView.findViewById(R.id.btnAceptarPedido);
            btnDetalles = itemView.findViewById(R.id.btnDetalles);
        }
        
        public ViewHolder(@NonNull View itemView, boolean isRestaurante) {
            this(itemView);
        }
    }
}




