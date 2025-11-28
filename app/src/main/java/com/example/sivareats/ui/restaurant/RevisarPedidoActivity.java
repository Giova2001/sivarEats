package com.example.sivareats.ui.restaurant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sivareats.R;
import com.example.sivareats.model.Producto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RevisarPedidoActivity extends AppCompatActivity {

    private static final String TAG = "RevisarPedidoActivity";
    
    private FirebaseFirestore db;
    private String pedidoId;
    private String restauranteName;
    private String clienteEmail;
    private String restauranteEmail; // Email del restaurante para guardar copia del pedido
    
    private MaterialToolbar toolbar;
    private ImageView imgCliente;
    private TextView tvClienteNombre;
    private TextView tvRating;
    private MaterialButton btnRechazar;
    private MaterialButton btnAceptar;
    private TextView tvTiempoEstimado;
    private RecyclerView recyclerProductos;
    private TextView tvSubtotal;
    private TextView tvDelivery;
    private TextView tvTotal;
    private ImageView imgClienteContacto;
    private TextView tvClienteContactoNombre;
    private TextView tvClienteContactoInfo;
    private ImageView btnLlamar;
    private ImageView btnMensaje;
    
    private ProductosPedidoAdapter adapter;
    private List<Producto> productosList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revisar_pedido);

        // Obtener datos del Intent
        pedidoId = getIntent().getStringExtra("pedido_id");
        restauranteName = getIntent().getStringExtra("restaurante_name");
        
        if (pedidoId == null || restauranteName == null) {
            Toast.makeText(this, "Error: Información del pedido no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        
        // Obtener email del restaurante desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        restauranteEmail = prefs.getString("CURRENT_USER_EMAIL", null);
        
        initViews();
        setupToolbar();
        loadPedidoData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        imgCliente = findViewById(R.id.imgCliente);
        tvClienteNombre = findViewById(R.id.tvClienteNombre);
        tvRating = findViewById(R.id.tvRating);
        btnRechazar = findViewById(R.id.btnRechazar);
        btnAceptar = findViewById(R.id.btnAceptar);
        tvTiempoEstimado = findViewById(R.id.tvTiempoEstimado);
        recyclerProductos = findViewById(R.id.recyclerProductos);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDelivery = findViewById(R.id.tvDelivery);
        tvTotal = findViewById(R.id.tvTotal);
        imgClienteContacto = findViewById(R.id.imgClienteContacto);
        tvClienteContactoNombre = findViewById(R.id.tvClienteContactoNombre);
        tvClienteContactoInfo = findViewById(R.id.tvClienteContactoInfo);
        btnLlamar = findViewById(R.id.btnLlamar);
        btnMensaje = findViewById(R.id.btnMensaje);
        
        // Configurar RecyclerView
        adapter = new ProductosPedidoAdapter(productosList);
        recyclerProductos.setLayoutManager(new LinearLayoutManager(this));
        recyclerProductos.setAdapter(adapter);
        
        // Configurar botones
        btnAceptar.setOnClickListener(v -> aceptarPedido());
        btnRechazar.setOnClickListener(v -> rechazarPedido());
        
        // Configurar botones de contacto
        btnLlamar.setOnClickListener(v -> llamarCliente());
        btnMensaje.setOnClickListener(v -> mensajearCliente());
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadPedidoData() {
        db.collection("restaurantes").document(restauranteName)
                .collection("pedidos_pendientes").document(pedidoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mostrarDatosPedido(documentSnapshot);
                    } else {
                        Toast.makeText(this, "Pedido no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar pedido: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar el pedido", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void mostrarDatosPedido(DocumentSnapshot document) {
        // Obtener email del cliente
        clienteEmail = document.getString("clienteEmail");
        
        // Cargar información del cliente
        if (clienteEmail != null && !clienteEmail.isEmpty()) {
            cargarInfoCliente(clienteEmail);
        }
        
        // Obtener productos
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> productosData = (List<Map<String, Object>>) document.get("productos");
        if (productosData != null) {
            productosList.clear();
            for (Map<String, Object> prodData : productosData) {
                String nombre = (String) prodData.get("nombre");
                String descripcion = (String) prodData.get("descripcion");
                Double precio = ((Number) prodData.get("precio")).doubleValue();
                Long cantidadLong = ((Number) prodData.get("cantidad")).longValue();
                int cantidad = cantidadLong != null ? cantidadLong.intValue() : 1;
                Long imagenResIdLong = prodData.get("imagenResId") != null ? ((Number) prodData.get("imagenResId")).longValue() : null;
                int imagenResId = imagenResIdLong != null ? imagenResIdLong.intValue() : R.drawable.ic_profile;
                
                if (nombre != null && precio != null) {
                    Producto producto = new Producto(nombre, descripcion != null ? descripcion : "", imagenResId, precio, cantidad);
                    productosList.add(producto);
                }
            }
            adapter.notifyDataSetChanged();
        }
        
        // Obtener precios
        Double subtotalDouble = document.getDouble("subtotal");
        Double deliveryDouble = document.getDouble("delivery");
        Double totalDouble = document.getDouble("total");
        
        if (subtotalDouble != null) {
            tvSubtotal.setText("$" + String.format(Locale.getDefault(), "%.2f", subtotalDouble));
        }
        if (deliveryDouble != null) {
            tvDelivery.setText("$" + String.format(Locale.getDefault(), "%.2f", deliveryDouble));
        }
        if (totalDouble != null) {
            tvTotal.setText("$" + String.format(Locale.getDefault(), "%.2f", totalDouble));
        }
        
        // Tiempo estimado
        Long tiempoEstimadoLong = document.getLong("tiempoEstimado");
        if (tiempoEstimadoLong != null) {
            tvTiempoEstimado.setText("Llegando en " + tiempoEstimadoLong + " minutos");
        }
    }

    private void cargarInfoCliente(String email) {
        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("name");
                        if (nombre != null && !nombre.isEmpty()) {
                            tvClienteNombre.setText(nombre);
                            tvClienteContactoNombre.setText(nombre);
                        } else {
                            // Usar parte del email como nombre
                            String nombreEmail = email.split("@")[0];
                            tvClienteNombre.setText(nombreEmail);
                            tvClienteContactoNombre.setText(nombreEmail);
                        }
                        
                        // Cargar imagen de perfil
                        String imageUrl = documentSnapshot.getString("profile_image_url");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .into(imgCliente);
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .into(imgClienteContacto);
                        }
                        
                        // Obtener teléfono si está disponible
                        String telefono = documentSnapshot.getString("phone");
                        if (telefono != null && !telefono.isEmpty()) {
                            tvClienteContactoInfo.setText("Cliente • " + telefono);
                        } else {
                            tvClienteContactoInfo.setText("Cliente • " + email);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar info del cliente: " + e.getMessage());
                    // Usar email como nombre por defecto
                    String nombreEmail = email.split("@")[0];
                    tvClienteNombre.setText(nombreEmail);
                    tvClienteContactoNombre.setText(nombreEmail);
                    tvClienteContactoInfo.setText("Cliente • " + email);
                });
    }

    private void aceptarPedido() {
        // Primero obtener los datos completos del pedido para guardar copia
        db.collection("restaurantes").document(restauranteName)
                .collection("pedidos_pendientes").document(pedidoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Pedido no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Obtener todos los datos del pedido
                    Map<String, Object> pedidoData = new HashMap<>(documentSnapshot.getData());
                    pedidoData.put("estado", "preparacion"); // Actualizar estado
                    
                    // Actualizar estado del pedido en pedidos_pendientes
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("estado", "preparacion");
                    
                    db.collection("restaurantes").document(restauranteName)
                            .collection("pedidos_pendientes").document(pedidoId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Actualizar en la colección del cliente
                                if (clienteEmail != null && !clienteEmail.isEmpty()) {
                                    db.collection("users").document(clienteEmail)
                                            .collection("pedidos").document(pedidoId)
                                            .update("estado", "preparacion")
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "Estado actualizado en usuario cliente");
                                            });
                                }
                                
                                // Guardar copia en la colección del restaurante para seguimiento
                                if (restauranteEmail != null && !restauranteEmail.isEmpty()) {
                                    // Asegurar que el pedido tenga el email del cliente
                                    pedidoData.put("clienteEmail", clienteEmail);
                                    
                                    db.collection("users").document(restauranteEmail)
                                            .collection("pedidos").document(pedidoId)
                                            .set(pedidoData)
                                            .addOnSuccessListener(aVoid3 -> {
                                                Log.d(TAG, "Copia del pedido guardada en restaurante: " + restauranteEmail);
                                                Toast.makeText(this, "Pedido aceptado", Toast.LENGTH_SHORT).show();
                                                setResult(RESULT_OK);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error al guardar copia en restaurante: " + e.getMessage());
                                                // Aún así mostrar éxito ya que el pedido fue aceptado
                                                Toast.makeText(this, "Pedido aceptado", Toast.LENGTH_SHORT).show();
                                                setResult(RESULT_OK);
                                                finish();
                                            });
                                } else {
                                    // Si no hay email del restaurante, solo mostrar éxito
                                    Toast.makeText(this, "Pedido aceptado", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al aceptar pedido: " + e.getMessage());
                                Toast.makeText(this, "Error al aceptar el pedido", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener datos del pedido: " + e.getMessage());
                    Toast.makeText(this, "Error al obtener datos del pedido", Toast.LENGTH_SHORT).show();
                });
    }

    private void rechazarPedido() {
        // Eliminar el pedido de pedidos_pendientes
        db.collection("restaurantes").document(restauranteName)
                .collection("pedidos_pendientes").document(pedidoId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Actualizar estado en la colección del usuario a "rechazado"
                    if (clienteEmail != null && !clienteEmail.isEmpty()) {
                        db.collection("users").document(clienteEmail)
                                .collection("pedidos").document(pedidoId)
                                .update("estado", "rechazado")
                                .addOnSuccessListener(aVoid2 -> {
                                    Log.d(TAG, "Estado actualizado a rechazado en usuario");
                                });
                    }
                    
                    Toast.makeText(this, "Pedido rechazado", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al rechazar pedido: " + e.getMessage());
                    Toast.makeText(this, "Error al rechazar el pedido", Toast.LENGTH_SHORT).show();
                });
    }

    private void llamarCliente() {
        // Obtener teléfono del cliente desde Firebase
        if (clienteEmail != null && !clienteEmail.isEmpty()) {
            db.collection("users").document(clienteEmail)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String telefono = documentSnapshot.getString("phone");
                            if (telefono != null && !telefono.isEmpty()) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + telefono));
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Teléfono no disponible", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void mensajearCliente() {
        // Abrir aplicación de mensajería
        if (clienteEmail != null && !clienteEmail.isEmpty()) {
            db.collection("users").document(clienteEmail)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String telefono = documentSnapshot.getString("phone");
                            if (telefono != null && !telefono.isEmpty()) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("sms:" + telefono));
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Teléfono no disponible", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    // Adapter para productos del pedido
    private static class ProductosPedidoAdapter extends RecyclerView.Adapter<ProductosPedidoAdapter.ViewHolder> {
        private List<Producto> productos;

        public ProductosPedidoAdapter(List<Producto> productos) {
            this.productos = productos;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_producto_pedido, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Producto producto = productos.get(position);
            holder.tvNombre.setText(producto.getNombre());
            holder.tvCantidad.setText("x" + producto.getCantidad());
            holder.tvPrecio.setText("$" + String.format(Locale.getDefault(), "%.2f", producto.getPrecio()));
            
            // Cargar imagen
            if (producto.getImagenUrl() != null && !producto.getImagenUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(producto.getImagenUrl())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(holder.imgProducto);
            } else {
                holder.imgProducto.setImageResource(producto.getImagenResId());
            }
        }

        @Override
        public int getItemCount() {
            return productos != null ? productos.size() : 0;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgProducto;
            TextView tvNombre;
            TextView tvCantidad;
            TextView tvPrecio;

            public ViewHolder(View itemView) {
                super(itemView);
                imgProducto = itemView.findViewById(R.id.imgProducto);
                tvNombre = itemView.findViewById(R.id.tvNombre);
                tvCantidad = itemView.findViewById(R.id.tvCantidad);
                tvPrecio = itemView.findViewById(R.id.tvPrecio);
            }
        }
    }
}

