package com.example.sivareats.fragments;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.sivareats.R;
import com.example.sivareats.model.Pedido;
import com.example.sivareats.model.Producto;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AssignOrderFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "AssignOrderFragment";
    private static final int REQUEST_LOCATION = 1001;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private String repartidorEmail;

    // Views
    private MaterialCardView cardDetallesPedido;
    private ImageView imgRestauranteLogo;
    private TextView tvRestauranteNombre;
    private TextView tvDistancia;
    private TextView tvPedidoId;
    private TextView tvTotalPedido;
    private ImageButton btnCerrarDetalles;
    private com.google.android.material.button.MaterialButton btnAceptarPedido;

    // Datos
    private Map<String, Pedido> pedidosDisponibles = new HashMap<>();
    private Map<Marker, String> markerToPedidoId = new HashMap<>();
    private Pedido pedidoSeleccionado;
    private Marker markerSeleccionado;
    private Location ubicacionRepartidor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assign_order, container, false);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Obtener email del repartidor
        SharedPreferences prefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        repartidorEmail = prefs.getString("CURRENT_USER_EMAIL", null);

        // Inicializar vistas
        initViews(view);

        // Inicializar mapa
        initMap();

        // Cargar pedidos disponibles
        cargarPedidosDisponibles();

        return view;
    }

    private void initViews(View view) {
        cardDetallesPedido = view.findViewById(R.id.cardDetallesPedido);
        imgRestauranteLogo = view.findViewById(R.id.imgRestauranteLogo);
        tvRestauranteNombre = view.findViewById(R.id.tvRestauranteNombre);
        tvDistancia = view.findViewById(R.id.tvDistancia);
        tvPedidoId = view.findViewById(R.id.tvPedidoId);
        tvTotalPedido = view.findViewById(R.id.tvTotalPedido);
        btnCerrarDetalles = view.findViewById(R.id.btnCerrarDetalles);
        btnAceptarPedido = view.findViewById(R.id.btnAceptarPedido);

        // Listeners
        btnCerrarDetalles.setOnClickListener(v -> ocultarDetalles());
        btnAceptarPedido.setOnClickListener(v -> aceptarPedido());
    }

    private void initMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapAssignOrder);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            obtenerUbicacionActual();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        ubicacionRepartidor = location;
                        LatLng actual = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(actual, 12));
                    }
                });
    }

    private void cargarPedidosDisponibles() {
        // Buscar pedidos con estado "entregado_repartidor" en todas las colecciones de restaurantes
        db.collectionGroup("pedidos_pendientes")
                .whereEqualTo("estado", "entregado_repartidor")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pedidosDisponibles.clear();
                    markerToPedidoId.clear();
                    if (mMap != null) {
                        mMap.clear();
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Pedido pedido = convertirDocumentoAPedido(document);
                            if (pedido != null) {
                                String pedidoId = pedido.getId();
                                pedidosDisponibles.put(pedidoId, pedido);

                                // Obtener ubicación del restaurante desde el pedido
                                String restauranteName = pedido.getRestaurante();
                                if (restauranteName != null && !restauranteName.isEmpty()) {
                                    cargarUbicacionRestaurante(restauranteName, pedido);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al convertir documento a pedido: " + e.getMessage());
                        }
                    }

                    Log.d(TAG, "Pedidos disponibles cargados: " + pedidosDisponibles.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar pedidos disponibles: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error al cargar pedidos", Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarUbicacionRestaurante(String restauranteName, Pedido pedido) {
        // Buscar la ubicación del restaurante en la colección de usuarios
        db.collection("users")
                .whereEqualTo("name", restauranteName)
                .whereEqualTo("rol", "RESTAURANTE")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        
                        // Intentar obtener coordenadas del pedido primero
                        Map<String, Object> pedidoData = doc.getData();
                        Double lat = null;
                        Double lng = null;
                        
                        // Buscar coordenadas en el pedido
                        if (pedidoData != null) {
                            Object latObj = pedidoData.get("restauranteLat");
                            Object lngObj = pedidoData.get("restauranteLng");
                            
                            if (latObj instanceof Number) lat = ((Number) latObj).doubleValue();
                            if (lngObj instanceof Number) lng = ((Number) lngObj).doubleValue();
                        }
                        
                        // Si no hay coordenadas en el pedido, usar coordenadas por defecto o del restaurante
                        if (lat == null || lng == null) {
                            // Usar coordenadas por defecto (San Salvador)
                            lat = 13.6929;
                            lng = -89.2182;
                        }
                        
                        LatLng restauranteLocation = new LatLng(lat, lng);
                        agregarMarcadorRestaurante(restauranteLocation, restauranteName, pedido, doc.getString("profile_image_url"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar ubicación del restaurante: " + e.getMessage());
                });
    }

    private void agregarMarcadorRestaurante(LatLng location, String restauranteName, Pedido pedido, String logoUrl) {
        if (mMap == null) return;

        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title(restauranteName)
                .snippet("Pedido disponible")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

        Marker marker = mMap.addMarker(markerOptions);
        markerToPedidoId.put(marker, pedido.getId());

        // Listener para clic en marcador
        mMap.setOnMarkerClickListener(marker1 -> {
            String pedidoId = markerToPedidoId.get(marker1);
            if (pedidoId != null) {
                mostrarDetallesPedido(pedidosDisponibles.get(pedidoId), marker1);
            }
            return true;
        });
    }

    private void mostrarDetallesPedido(Pedido pedido, Marker marker) {
        if (pedido == null) return;

        pedidoSeleccionado = pedido;
        markerSeleccionado = marker;

        // Mostrar información del restaurante
        tvRestauranteNombre.setText(pedido.getRestaurante());
        tvPedidoId.setText("ID: " + pedido.getId());
        tvTotalPedido.setText("Total: $" + String.format(Locale.getDefault(), "%.2f", pedido.getTotal()));

        // Calcular distancia
        if (ubicacionRepartidor != null && marker != null) {
            Location locationRestaurante = new Location("restaurante");
            locationRestaurante.setLatitude(marker.getPosition().latitude);
            locationRestaurante.setLongitude(marker.getPosition().longitude);
            
            float distancia = ubicacionRepartidor.distanceTo(locationRestaurante) / 1000; // en km
            tvDistancia.setText(String.format(Locale.getDefault(), "%.1f km", distancia));
        } else {
            tvDistancia.setText("Distancia no disponible");
        }

        // Cargar logo del restaurante
        cargarLogoRestaurante(pedido.getRestaurante());

        // Mostrar card
        cardDetallesPedido.setVisibility(View.VISIBLE);
    }

    private void cargarLogoRestaurante(String restauranteName) {
        db.collection("users")
                .whereEqualTo("name", restauranteName)
                .whereEqualTo("rol", "RESTAURANTE")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String logoUrl = queryDocumentSnapshots.getDocuments().get(0).getString("profile_image_url");
                        if (logoUrl != null && !logoUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(logoUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .into(imgRestauranteLogo);
                        }
                    }
                });
    }

    private void ocultarDetalles() {
        cardDetallesPedido.setVisibility(View.GONE);
        pedidoSeleccionado = null;
        markerSeleccionado = null;
    }

    private void aceptarPedido() {
        if (pedidoSeleccionado == null || repartidorEmail == null) {
            Toast.makeText(requireContext(), "Error: No se puede aceptar el pedido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener información del repartidor
        db.collection("users").document(repartidorEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String repartidorNombre = documentSnapshot.getString("name");
                        String repartidorTelefono = documentSnapshot.getString("telefono");

                        // Actualizar estado del pedido a "en_camino"
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("estado", "en_camino");
                        updates.put("repartidorEmail", repartidorEmail);
                        if (repartidorNombre != null) updates.put("repartidorNombre", repartidorNombre);
                        if (repartidorTelefono != null) updates.put("repartidorTelefono", repartidorTelefono);

                        // Actualizar en todas las colecciones relevantes
                        String restauranteName = pedidoSeleccionado.getRestaurante();
                        String pedidoId = pedidoSeleccionado.getId();
                        String clienteEmail = pedidoSeleccionado.getClienteEmail();

                        // Actualizar en pedidos_pendientes del restaurante
                        db.collection("restaurantes").document(restauranteName)
                                .collection("pedidos_pendientes").document(pedidoId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Estado actualizado en pedidos_pendientes");
                                });

                        // Actualizar en colección del cliente
                        if (clienteEmail != null && !clienteEmail.isEmpty()) {
                            db.collection("users").document(clienteEmail)
                                    .collection("pedidos").document(pedidoId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Estado actualizado en cliente");
                                    });
                        }

                        // Guardar pedido en colección del repartidor
                        Map<String, Object> pedidoData = new HashMap<>();
                        pedidoData.put("id", pedidoId);
                        pedidoData.put("restaurante", restauranteName);
                        pedidoData.put("direccion", pedidoSeleccionado.getDireccion());
                        pedidoData.put("total", pedidoSeleccionado.getTotal());
                        pedidoData.put("estado", "en_camino");
                        pedidoData.put("fecha", pedidoSeleccionado.getFecha());
                        pedidoData.put("productos", convertirProductosAMap(pedidoSeleccionado.getProductos()));
                        pedidoData.put("clienteEmail", clienteEmail);
                        pedidoData.put("repartidorEmail", repartidorEmail);
                        pedidoData.put("repartidorNombre", repartidorNombre);
                        pedidoData.put("repartidorTelefono", repartidorTelefono);

                        db.collection("users").document(repartidorEmail)
                                .collection("pedidos").document(pedidoId)
                                .set(pedidoData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Pedido guardado en repartidor");
                                    
                                    // Remover marcador del mapa
                                    if (markerSeleccionado != null) {
                                        markerSeleccionado.remove();
                                        markerToPedidoId.remove(markerSeleccionado);
                                    }
                                    
                                    // Remover de la lista
                                    pedidosDisponibles.remove(pedidoId);
                                    
                                    // Ocultar detalles
                                    ocultarDetalles();
                                    
                                    Toast.makeText(requireContext(), "Pedido aceptado", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error al guardar pedido en repartidor: " + e.getMessage());
                                    Toast.makeText(requireContext(), "Error al aceptar el pedido", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener información del repartidor: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error al obtener información", Toast.LENGTH_SHORT).show();
                });
    }

    private Pedido convertirDocumentoAPedido(QueryDocumentSnapshot document) {
        try {
            String id = document.getString("id");
            if (id == null) id = document.getId();

            String restaurante = document.getString("restaurante");
            if (restaurante == null) restaurante = "Restaurante";

            String direccion = document.getString("direccion");
            if (direccion == null) direccion = "Dirección no especificada";

            Double totalDouble = document.getDouble("total");
            double total = totalDouble != null ? totalDouble : 0.0;

            String estado = document.getString("estado");
            if (estado == null) estado = "entregado_repartidor";

            // Obtener fecha
            java.util.Date fecha;
            if (document.getTimestamp("fecha") != null) {
                fecha = document.getTimestamp("fecha").toDate();
            } else {
                fecha = new java.util.Date();
            }

            // Obtener productos
            List<Producto> productos = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productosData = (List<Map<String, Object>>) document.get("productos");
            if (productosData != null) {
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
                        productos.add(producto);
                    }
                }
            }

            Pedido pedido = new Pedido(id, restaurante, direccion, total, estado, fecha, productos);

            // Obtener email del cliente
            String clienteEmail = document.getString("clienteEmail");
            if (clienteEmail != null) pedido.setClienteEmail(clienteEmail);

            return pedido;
        } catch (Exception e) {
            Log.e(TAG, "Error al convertir documento: " + e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> convertirProductosAMap(List<Producto> productos) {
        List<Map<String, Object>> productosMap = new ArrayList<>();
        for (Producto producto : productos) {
            Map<String, Object> prodMap = new HashMap<>();
            prodMap.put("nombre", producto.getNombre());
            prodMap.put("descripcion", producto.getDescripcion());
            prodMap.put("precio", producto.getPrecio());
            prodMap.put("cantidad", producto.getCantidad());
            prodMap.put("imagenResId", producto.getImagenResId());
            productosMap.add(prodMap);
        }
        return productosMap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                obtenerUbicacionActual();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar pedidos cuando el fragment se vuelve visible
        if (db != null) {
            cargarPedidosDisponibles();
        }
    }
}
