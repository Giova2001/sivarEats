package com.example.sivareats.ui.restaurant;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sivareats.R;
import com.example.sivareats.model.Producto;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
    private FusedLocationProviderClient fusedLocationClient;
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
    private MaterialAutoCompleteTextView spinnerEstado;
    
    private ProductosPedidoAdapter adapter;
    private List<Producto> productosList = new ArrayList<>();
    private String estadoActual = "pendiente"; // Estado actual del pedido

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
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
        spinnerEstado = findViewById(R.id.spinnerEstado);
        
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
        
        // El spinner se configurará después de cargar el estado del pedido
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadPedidoData() {
        // Primero intentar cargar desde pedidos_pendientes
        db.collection("restaurantes").document(restauranteName)
                .collection("pedidos_pendientes").document(pedidoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mostrarDatosPedido(documentSnapshot);
                    } else {
                        // Si no está en pendientes, buscar en la colección del restaurante
                        if (restauranteEmail != null && !restauranteEmail.isEmpty()) {
                            db.collection("users").document(restauranteEmail)
                                    .collection("pedidos").document(pedidoId)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot2 -> {
                                        if (documentSnapshot2.exists()) {
                                            mostrarDatosPedido(documentSnapshot2);
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
                        } else {
                            Toast.makeText(this, "Pedido no encontrado", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar pedido: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar el pedido", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void mostrarDatosPedido(DocumentSnapshot document) {
        // Obtener estado actual del pedido
        estadoActual = document.getString("estado");
        if (estadoActual == null || estadoActual.isEmpty()) {
            estadoActual = "pendiente";
        }
        
        // Configurar spinner de estados después de obtener el estado
        setupEstadoSpinner();
        
        // Actualizar UI según el estado
        actualizarUIEstado();
        
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
                                                // Actualizar estado local y UI
                                                estadoActual = "preparacion";
                                                actualizarUIEstado();
                                                Toast.makeText(this, "Pedido aceptado", Toast.LENGTH_SHORT).show();
                                                // No cerrar la actividad, permitir cambiar estados
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error al guardar copia en restaurante: " + e.getMessage());
                                                // Aún así mostrar éxito ya que el pedido fue aceptado
                                                Toast.makeText(this, "Pedido aceptado", Toast.LENGTH_SHORT).show();
                                                setResult(RESULT_OK);
                                                finish();
                                            });
                                } else {
                                    // Si no hay email del restaurante, actualizar estado local y UI
                                    estadoActual = "preparacion";
                                    actualizarUIEstado();
                                    Toast.makeText(this, "Pedido aceptado", Toast.LENGTH_SHORT).show();
                                    // No cerrar la actividad, permitir cambiar estados
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
    
    /**
     * Configura el spinner de estados con los estados disponibles según el estado actual
     */
    private void setupEstadoSpinner() {
        // Definir estados posibles
        String[] estadosDisponibles = obtenerEstadosDisponibles(estadoActual);
        
        // Crear adapter para el spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, estadosDisponibles);
        spinnerEstado.setAdapter(adapter);
        
        Log.d(TAG, "Spinner configurado con " + estadosDisponibles.length + " estados disponibles");
        
        // Establecer estado actual
        String nombreEstadoActual = obtenerNombreEstado(estadoActual);
        spinnerEstado.setText(nombreEstadoActual, false);
        Log.d(TAG, "Estado actual establecido: " + nombreEstadoActual);
        
        // Listener para cambios de estado
        spinnerEstado.setOnItemClickListener((parent, view, position, id) -> {
            try {
                String nuevoEstado = (String) parent.getItemAtPosition(position);
                String estadoCodigo = obtenerCodigoEstado(nuevoEstado);
                Log.d(TAG, "=== CLICK EN SPINNER ===");
                Log.d(TAG, "Estado seleccionado: " + nuevoEstado);
                Log.d(TAG, "Código de estado: " + estadoCodigo);
                Log.d(TAG, "Estado actual: " + estadoActual);
                
                if (estadoCodigo != null && !estadoCodigo.equals(estadoActual)) {
                    Log.d(TAG, "Cambiando estado de " + estadoActual + " a " + estadoCodigo);
                    cambiarEstadoPedido(estadoCodigo);
                } else if (estadoCodigo != null && estadoCodigo.equals(estadoActual)) {
                    Log.d(TAG, "Estado seleccionado es el mismo que el actual, no se cambia");
                } else {
                    Log.w(TAG, "Código de estado es null o inválido");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en listener del spinner: " + e.getMessage(), e);
            }
        });
        
        // También agregar listener para cuando se cierra el dropdown
        spinnerEstado.setOnDismissListener(() -> {
            Log.d(TAG, "Dropdown cerrado");
        });
    }
    
    /**
     * Obtiene los estados disponibles según el estado actual
     * No se puede retroceder, solo avanzar
     */
    private String[] obtenerEstadosDisponibles(String estadoActual) {
        List<String> estados = new ArrayList<>();
        
        // Siempre mostrar el estado actual
        estados.add(obtenerNombreEstado(estadoActual));
        
        // Agregar estados siguientes según el estado actual
        switch (estadoActual) {
            case "pendiente":
                // Desde pendiente se puede ir a preparacion
                estados.add(obtenerNombreEstado("preparacion"));
                break;
            case "preparacion":
                // Desde preparacion se puede ir a entregado a repartidor
                estados.add(obtenerNombreEstado("entregado_repartidor"));
                break;
            case "entregado_repartidor":
                // Ya está en el último estado, no hay más estados
                break;
            default:
                // Si el estado no es reconocido, permitir avanzar a preparacion
                if (!estados.contains(obtenerNombreEstado("preparacion"))) {
                    estados.add(obtenerNombreEstado("preparacion"));
                }
                break;
        }
        
        return estados.toArray(new String[0]);
    }
    
    /**
     * Convierte código de estado a nombre legible
     */
    private String obtenerNombreEstado(String codigo) {
        switch (codigo) {
            case "pendiente":
                return "Validación del pedido";
            case "preparacion":
                return "En preparación";
            case "entregado_repartidor":
                return "Entregado a repartidor";
            default:
                return codigo;
        }
    }
    
    /**
     * Convierte nombre de estado a código
     */
    private String obtenerCodigoEstado(String nombre) {
        switch (nombre) {
            case "Validación del pedido":
                return "pendiente";
            case "En preparación":
                return "preparacion";
            case "Entregado a repartidor":
                return "entregado_repartidor";
            default:
                return nombre;
        }
    }
    
    /**
     * Actualiza la UI según el estado actual del pedido
     */
    private void actualizarUIEstado() {
        // Si el estado no es "pendiente", ocultar botones de aceptar/rechazar
        if (!"pendiente".equals(estadoActual)) {
            btnAceptar.setVisibility(View.GONE);
            btnRechazar.setVisibility(View.GONE);
        } else {
            btnAceptar.setVisibility(View.VISIBLE);
            btnRechazar.setVisibility(View.VISIBLE);
        }
        
        // Actualizar spinner con estados disponibles
        String[] estadosDisponibles = obtenerEstadosDisponibles(estadoActual);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, estadosDisponibles);
        spinnerEstado.setAdapter(adapter);
        spinnerEstado.setText(obtenerNombreEstado(estadoActual), false);
    }
    
    /**
     * Cambia el estado del pedido en Firebase
     */
    private void cambiarEstadoPedido(String nuevoEstado) {
        if (nuevoEstado == null || nuevoEstado.equals(estadoActual)) {
            return;
        }
        
        // Validar que no se esté retrocediendo
        if (!esProgresionValida(estadoActual, nuevoEstado)) {
            Toast.makeText(this, "No se puede retroceder el estado del pedido", Toast.LENGTH_SHORT).show();
            // Restaurar estado anterior en el spinner
            spinnerEstado.setText(obtenerNombreEstado(estadoActual), false);
            return;
        }
        
        // Si el nuevo estado es "entregado_repartidor", obtener ubicación actual
        if ("entregado_repartidor".equals(nuevoEstado)) {
            obtenerUbicacionYActualizarEstado(nuevoEstado);
        } else {
            actualizarEstadoEnFirebase(nuevoEstado, null, null);
        }
    }
    
    private void obtenerUbicacionYActualizarEstado(String nuevoEstado) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            // Si no hay permiso, actualizar sin ubicación
            actualizarEstadoEnFirebase(nuevoEstado, null, null);
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        actualizarEstadoEnFirebase(nuevoEstado, lat, lng);
                    } else {
                        // Si no se puede obtener ubicación, actualizar sin ella
                        actualizarEstadoEnFirebase(nuevoEstado, null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener ubicación: " + e.getMessage());
                    // Actualizar sin ubicación
                    actualizarEstadoEnFirebase(nuevoEstado, null, null);
                });
    }
    
    private void actualizarEstadoEnFirebase(String nuevoEstado, Double lat, Double lng) {
        // Actualizar estado en Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("estado", nuevoEstado);
        
        // Si hay coordenadas, agregarlas
        if (lat != null && lng != null) {
            updates.put("restauranteLat", lat);
            updates.put("restauranteLng", lng);
        }
        
        // Actualizar en pedidos_pendientes si existe
        db.collection("restaurantes").document(restauranteName)
                .collection("pedidos_pendientes").document(pedidoId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Estado actualizado en pedidos_pendientes");
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Pedido no está en pedidos_pendientes, continuando...");
                });
        
        // Actualizar en la colección del cliente
        if (clienteEmail != null && !clienteEmail.isEmpty()) {
            db.collection("users").document(clienteEmail)
                    .collection("pedidos").document(pedidoId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Estado actualizado en usuario cliente");
                    });
        }
        
        // Actualizar en la colección del restaurante
        if (restauranteEmail != null && !restauranteEmail.isEmpty()) {
            db.collection("users").document(restauranteEmail)
                    .collection("pedidos").document(pedidoId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Estado actualizado en restaurante");
                        estadoActual = nuevoEstado;
                        actualizarUIEstado();
                        Toast.makeText(this, "Estado actualizado: " + obtenerNombreEstado(nuevoEstado), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al actualizar estado en restaurante: " + e.getMessage());
                        Toast.makeText(this, "Error al actualizar el estado", Toast.LENGTH_SHORT).show();
                        // Restaurar estado anterior en el spinner
                        spinnerEstado.setText(obtenerNombreEstado(estadoActual), false);
                    });
        } else {
            // Si no hay email del restaurante, actualizar estado local
            estadoActual = nuevoEstado;
            actualizarUIEstado();
            Toast.makeText(this, "Estado actualizado: " + obtenerNombreEstado(nuevoEstado), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Valida que el cambio de estado sea una progresión válida (no retroceder)
     */
    private boolean esProgresionValida(String estadoActual, String nuevoEstado) {
        // Definir orden de estados
        int ordenActual = obtenerOrdenEstado(estadoActual);
        int ordenNuevo = obtenerOrdenEstado(nuevoEstado);
        
        // Solo permitir avanzar (ordenNuevo > ordenActual)
        return ordenNuevo > ordenActual;
    }
    
    /**
     * Obtiene el orden numérico de un estado (para validar progresión)
     */
    private int obtenerOrdenEstado(String estado) {
        switch (estado) {
            case "pendiente":
                return 1;
            case "preparacion":
                return 2;
            case "entregado_repartidor":
                return 3;
            default:
                return 0;
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

