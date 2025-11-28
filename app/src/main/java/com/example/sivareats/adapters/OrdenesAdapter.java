package com.example.sivareats.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sivareats.R;
import com.example.sivareats.model.Pedido;
import com.example.sivareats.ui.ordenes.RastreandoPedidoActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrdenesAdapter extends RecyclerView.Adapter<OrdenesAdapter.ViewHolder> {

    private List<Pedido> listaPedidos;
    private OnPedidoClickListener listener;
    private boolean isRestaurante;
    private boolean isRepartidor;

    public interface OnPedidoClickListener {
        void onRastrearClick(Pedido pedido);
        void onVerDetallesClick(Pedido pedido);
    }

    public OrdenesAdapter(List<Pedido> lista) {
        this.listaPedidos = lista;
        this.isRestaurante = false;
        this.isRepartidor = false;
    }
    
    public OrdenesAdapter(List<Pedido> lista, boolean isRestaurante) {
        this.listaPedidos = lista;
        this.isRestaurante = isRestaurante;
        this.isRepartidor = false;
    }
    
    public OrdenesAdapter(List<Pedido> lista, boolean isRestaurante, boolean isRepartidor) {
        this.listaPedidos = lista;
        this.isRestaurante = isRestaurante;
        this.isRepartidor = isRepartidor;
    }

    public void setOnPedidoClickListener(OnPedidoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Determinar layout según tipo de usuario
        int layoutId;
        if (isRestaurante) {
            layoutId = R.layout.item_orden_restaurante;
        } else if (isRepartidor) {
            layoutId = R.layout.item_orden_repartidor;
        } else {
            layoutId = R.layout.item_orden;
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(view, isRestaurante, isRepartidor);
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
        } else if (isRepartidor) {
            if ("en_camino".equals(pedido.getEstado())) {
                estadoTexto = "En camino";
            } else if ("entregado".equals(pedido.getEstado())) {
                estadoTexto = "Entregado";
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
            // Para usuarios normales y repartidores, mostrar restaurante
            if (holder.tvRestaurante != null) {
                holder.tvRestaurante.setText(pedido.getRestaurante());
            }
        }
        
        // Configurar spinner de estados para repartidores
        if (isRepartidor && holder.spinnerEstado != null) {
            configurarSpinnerRepartidor(holder, pedido);
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
        } else if (isRepartidor) {
            // Para repartidores, siempre mostrar botón Rastrear si el pedido está en camino o entregado
            if ("en_camino".equals(pedido.getEstado()) || "entregado_repartidor".equals(pedido.getEstado())) {
                if (holder.btnRastrear != null) {
                    holder.btnRastrear.setVisibility(View.VISIBLE);
                    holder.btnRastrear.setText("Rastrear");
                }
            } else {
                if (holder.btnRastrear != null) {
                    holder.btnRastrear.setVisibility(View.GONE);
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
        
        MaterialAutoCompleteTextView spinnerEstado; // Para repartidores
        
        public ViewHolder(@NonNull View itemView, boolean isRestaurante) {
            this(itemView);
        }
        
        public ViewHolder(@NonNull View itemView, boolean isRestaurante, boolean isRepartidor) {
            this(itemView);
            if (isRepartidor) {
                spinnerEstado = itemView.findViewById(R.id.spinnerEstado);
            }
        }
    }
    
    private void configurarSpinnerRepartidor(ViewHolder holder, Pedido pedido) {
        if (holder.spinnerEstado == null) return;
        
        String estadoActual = pedido.getEstado();
        List<String> estadosDisponibles = new ArrayList<>();
        
        // Agregar estado actual
        estadosDisponibles.add(obtenerNombreEstadoRepartidor(estadoActual));
        
        // Agregar estados siguientes según el estado actual
        if ("en_camino".equals(estadoActual)) {
            estadosDisponibles.add(obtenerNombreEstadoRepartidor("entregado"));
        }
        // Si está en "entregado", no hay más estados
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(holder.itemView.getContext(),
                android.R.layout.simple_dropdown_item_1line, estadosDisponibles);
        holder.spinnerEstado.setAdapter(adapter);
        holder.spinnerEstado.setText(obtenerNombreEstadoRepartidor(estadoActual), false);
        
        // Listener para cambios de estado
        holder.spinnerEstado.setOnItemClickListener((parent, view, position, id) -> {
            String nuevoEstadoNombre = (String) parent.getItemAtPosition(position);
            String nuevoEstadoCodigo = obtenerCodigoEstadoRepartidor(nuevoEstadoNombre);
            
            if (nuevoEstadoCodigo != null && !nuevoEstadoCodigo.equals(estadoActual)) {
                cambiarEstadoPedidoRepartidor(pedido, nuevoEstadoCodigo, holder);
            }
        });
    }
    
    private String obtenerNombreEstadoRepartidor(String codigo) {
        switch (codigo) {
            case "en_camino":
                return "Pedido en camino";
            case "entregado":
                return "Pedido entregado";
            default:
                return codigo;
        }
    }
    
    private String obtenerCodigoEstadoRepartidor(String nombre) {
        switch (nombre) {
            case "Pedido en camino":
                return "en_camino";
            case "Pedido entregado":
                return "entregado";
            default:
                return nombre;
        }
    }
    
    private void cambiarEstadoPedidoRepartidor(Pedido pedido, String nuevoEstado, ViewHolder holder) {
        Context context = holder.itemView.getContext();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Validar progresión (solo puede avanzar de "en_camino" a "entregado")
        String estadoActual = pedido.getEstado();
        if (!"en_camino".equals(estadoActual) || !"entregado".equals(nuevoEstado)) {
            Toast.makeText(context, "Solo se puede cambiar de 'En camino' a 'Entregado'", Toast.LENGTH_SHORT).show();
            // Restaurar estado anterior
            holder.spinnerEstado.setText(obtenerNombreEstadoRepartidor(estadoActual), false);
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("estado", nuevoEstado);
        
        String pedidoId = pedido.getId();
        String restauranteName = pedido.getRestaurante();
        String clienteEmail = pedido.getClienteEmail();
        
        // Obtener email del repartidor desde SharedPreferences
        android.content.SharedPreferences prefs = context.getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        String repartidorEmail = prefs.getString("CURRENT_USER_EMAIL", null);
        
        if (repartidorEmail == null) {
            Toast.makeText(context, "Error: No se pudo identificar al repartidor", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Actualizar en todas las colecciones relevantes
        // 1. En la colección del repartidor
        db.collection("users").document(repartidorEmail)
                .collection("pedidos").document(pedidoId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("OrdenesAdapter", "Estado actualizado en repartidor");
                });
        
        // 2. En la colección del cliente
        if (clienteEmail != null && !clienteEmail.isEmpty()) {
            db.collection("users").document(clienteEmail)
                    .collection("pedidos").document(pedidoId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("OrdenesAdapter", "Estado actualizado en cliente");
                    });
        }
        
        // 3. En pedidos_pendientes del restaurante
        if (restauranteName != null && !restauranteName.isEmpty()) {
            db.collection("restaurantes").document(restauranteName)
                    .collection("pedidos_pendientes").document(pedidoId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("OrdenesAdapter", "Estado actualizado en restaurante");
                        pedido.setEstado(nuevoEstado);
                        Toast.makeText(context, "Estado actualizado: " + obtenerNombreEstadoRepartidor(nuevoEstado), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("OrdenesAdapter", "Error al actualizar estado: " + e.getMessage());
                        Toast.makeText(context, "Error al actualizar el estado", Toast.LENGTH_SHORT).show();
                        // Restaurar estado anterior
                        holder.spinnerEstado.setText(obtenerNombreEstadoRepartidor(estadoActual), false);
                    });
        }
    }
}




