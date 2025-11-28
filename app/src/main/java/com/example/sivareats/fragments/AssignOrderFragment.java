package com.example.sivareats.fragments;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
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
    private com.google.android.material.button.MaterialButton btnCrearPedidosPrueba;

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
        btnCrearPedidosPrueba = view.findViewById(R.id.btnCrearPedidosPrueba);

        // Listeners
        btnCerrarDetalles.setOnClickListener(v -> ocultarDetalles());
        btnAceptarPedido.setOnClickListener(v -> aceptarPedido());
        btnCrearPedidosPrueba.setOnClickListener(v -> crearPedidosPrueba());
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
        
        // Configurar listener para clic en marcadores
        mMap.setOnMarkerClickListener(marker -> {
            String pedidoId = markerToPedidoId.get(marker);
            if (pedidoId != null) {
                mostrarDetallesPedido(pedidosDisponibles.get(pedidoId), marker);
            }
            return true;
        });

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
        // Primero obtener ubicación del repartidor
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Se necesita permiso de ubicación para ver pedidos cercanos", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Iniciando carga de pedidos disponibles...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Log.w(TAG, "Ubicación es null, intentando obtener ubicación actualizada...");
                        // Intentar obtener ubicación actualizada
                        if (ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.getCurrentLocation(
                                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                                    null
                            ).addOnSuccessListener(location2 -> {
                                if (location2 != null) {
                                    procesarUbicacionYCargarPedidos(location2);
                                } else {
                                    Log.e(TAG, "No se pudo obtener ubicación actualizada");
                                    Toast.makeText(requireContext(), "No se pudo obtener tu ubicación. Asegúrate de tener GPS activado.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        return;
                    }

                    procesarUbicacionYCargarPedidos(location);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener ubicación: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Error al obtener ubicación: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    
    private void procesarUbicacionYCargarPedidos(Location location) {
        ubicacionRepartidor = location;
        double repartidorLat = location.getLatitude();
        double repartidorLng = location.getLongitude();
        
        Log.d(TAG, "Ubicación obtenida: " + repartidorLat + ", " + repartidorLng);

        // Limpiar datos anteriores
        pedidosDisponibles.clear();
        markerToPedidoId.clear();
        if (mMap != null) {
            mMap.clear();
        }

        // Cargar todos los restaurantes y luego buscar pedidos en cada uno
        db.collection("restaurantes")
                .get()
                .addOnSuccessListener(restaurantesSnapshot -> {
                    Log.d(TAG, "Restaurantes encontrados: " + restaurantesSnapshot.size());
                    
                    if (restaurantesSnapshot.isEmpty()) {
                        Toast.makeText(requireContext(), "No hay restaurantes disponibles", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    final int[] restaurantesProcesados = {0};
                    final int totalRestaurantes = restaurantesSnapshot.size();
                    
                    if (totalRestaurantes == 0) {
                        Toast.makeText(requireContext(), "No hay restaurantes disponibles", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    for (com.google.firebase.firestore.QueryDocumentSnapshot restauranteDoc : restaurantesSnapshot) {
                        final String restauranteName = restauranteDoc.getId();
                        
                        // Buscar pedidos con estado "entregado_repartidor" en cada restaurante
                        db.collection("restaurantes").document(restauranteName)
                                .collection("pedidos_pendientes")
                                .whereEqualTo("estado", "entregado_repartidor")
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    restaurantesProcesados[0]++;
                                    Log.d(TAG, "Restaurante " + restauranteName + ": " + queryDocumentSnapshots.size() + " pedidos con estado entregado_repartidor");
                                    
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        try {
                                            Pedido pedido = convertirDocumentoAPedido(document);
                                            if (pedido != null) {
                                                // Obtener coordenadas del restaurante desde el pedido
                                                Double restauranteLat = document.getDouble("restauranteLat");
                                                Double restauranteLng = document.getDouble("restauranteLng");

                                                if (restauranteLat != null && restauranteLng != null) {
                                                    // Calcular distancia
                                                    Location locationRestaurante = new Location("restaurante");
                                                    locationRestaurante.setLatitude(restauranteLat);
                                                    locationRestaurante.setLongitude(restauranteLng);

                                                    float distanciaMetros = ubicacionRepartidor.distanceTo(locationRestaurante);
                                                    float distanciaKm = distanciaMetros / 1000;

                                                    // Solo agregar si está dentro de 1km
                                                    if (distanciaKm <= 1.0) {
                                                        String pedidoId = pedido.getId();
                                                        if (pedidoId != null && !pedidosDisponibles.containsKey(pedidoId)) {
                                                            pedidosDisponibles.put(pedidoId, pedido);

                                                            // Agregar marcador en el mapa
                                                            LatLng restauranteLocation = new LatLng(restauranteLat, restauranteLng);
                                                            String restauranteName2 = pedido.getRestaurante();
                                                            cargarLogoYAgregarMarcador(restauranteLocation, restauranteName2, pedido);
                                                        }
                                                    }
                                                } else {
                                                    // Si no hay coordenadas, intentar obtenerlas del restaurante
                                                    String restauranteName2 = pedido.getRestaurante();
                                                    if (restauranteName2 != null && !restauranteName2.isEmpty()) {
                                                        cargarUbicacionRestaurante(restauranteName2, pedido, repartidorLat, repartidorLng);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error al convertir documento a pedido: " + e.getMessage(), e);
                                        }
                                    }
                                    
                                    // Cuando se procesen todos los restaurantes, mostrar resultado
                                    if (restaurantesProcesados[0] >= totalRestaurantes) {
                                        Log.d(TAG, "Pedidos disponibles cargados: " + pedidosDisponibles.size() + " dentro de 1km");
                                        if (pedidosDisponibles.isEmpty()) {
                                            Toast.makeText(requireContext(), "No hay pedidos disponibles en un radio de 1km", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(), "Se encontraron " + pedidosDisponibles.size() + " pedidos cercanos", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    restaurantesProcesados[0]++;
                                    Log.e(TAG, "Error al cargar pedidos del restaurante " + restauranteName + ": " + e.getMessage(), e);
                                    
                                    // Si es el último restaurante, mostrar resultado
                                    if (restaurantesProcesados[0] >= totalRestaurantes) {
                                        if (pedidosDisponibles.isEmpty()) {
                                            Toast.makeText(requireContext(), "No se encontraron pedidos disponibles", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(), "Se encontraron " + pedidosDisponibles.size() + " pedidos cercanos", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar restaurantes: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Error al cargar restaurantes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void cargarUbicacionRestaurante(String restauranteName, Pedido pedido, double repartidorLat, double repartidorLng) {
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
                        
                        // Verificar distancia antes de agregar
                        if (ubicacionRepartidor != null) {
                            Location locationRestaurante = new Location("restaurante");
                            locationRestaurante.setLatitude(lat);
                            locationRestaurante.setLongitude(lng);
                            
                            float distanciaMetros = ubicacionRepartidor.distanceTo(locationRestaurante);
                            float distanciaKm = distanciaMetros / 1000;
                            
                            // Solo agregar si está dentro de 1km
                            if (distanciaKm <= 1.0) {
                                LatLng restauranteLocation = new LatLng(lat, lng);
                                cargarLogoYAgregarMarcador(restauranteLocation, restauranteName, pedido);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar ubicación del restaurante: " + e.getMessage());
                });
    }
    
    private void cargarLogoYAgregarMarcador(LatLng location, String restauranteName, Pedido pedido) {
        // Buscar logo del restaurante
        db.collection("users")
                .whereEqualTo("name", restauranteName)
                .whereEqualTo("rol", "RESTAURANTE")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String logoUrl = null;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        logoUrl = queryDocumentSnapshots.getDocuments().get(0).getString("profile_image_url");
                    }
                    agregarMarcadorRestaurante(location, restauranteName, pedido, logoUrl);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar logo: " + e.getMessage());
                    agregarMarcadorRestaurante(location, restauranteName, pedido, null);
                });
    }

    private void agregarMarcadorRestaurante(LatLng location, String restauranteName, Pedido pedido, String logoUrl) {
        if (mMap == null) return;

        // Crear marcador personalizado con imagen del restaurante
        if (logoUrl != null && !logoUrl.isEmpty()) {
            // Cargar imagen y crear marcador personalizado
            Glide.with(requireContext())
                    .asBitmap()
                    .load(logoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                            BitmapDescriptor icon = crearMarcadorPersonalizado(resource);
                            agregarMarcadorAlMapa(location, restauranteName, pedido, icon);
                        }

                        @Override
                        public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                            // Usar marcador por defecto si falla la carga
                            BitmapDescriptor icon = crearMarcadorPersonalizado(null);
                            agregarMarcadorAlMapa(location, restauranteName, pedido, icon);
                        }
                    });
        } else {
            // Si no hay logo, usar marcador personalizado con imagen por defecto
            BitmapDescriptor icon = crearMarcadorPersonalizado(null);
            agregarMarcadorAlMapa(location, restauranteName, pedido, icon);
        }
    }
    
    private void agregarMarcadorAlMapa(LatLng location, String restauranteName, Pedido pedido, BitmapDescriptor icon) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title(restauranteName)
                .snippet("Pedido disponible")
                .icon(icon);

        Marker marker = mMap.addMarker(markerOptions);
        if (marker != null) {
            markerToPedidoId.put(marker, pedido.getId());
            Log.d(TAG, "Marcador agregado para pedido: " + pedido.getId() + " en " + restauranteName);
        }
    }
    
    /**
     * Crea un marcador personalizado con la imagen del restaurante dentro de un pin de mapa
     */
    private BitmapDescriptor crearMarcadorPersonalizado(Bitmap imagenRestaurante) {
        int width = 120;
        int height = 120;
        
        // Crear bitmap para el marcador
        Bitmap markerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);
        
        // Dibujar el pin de mapa (círculo en la parte superior, triángulo en la inferior)
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        // Color del pin (naranja/rojo)
        paint.setColor(0xFFFF6B35);
        
        // Dibujar círculo superior (cabeza del pin)
        float circleRadius = width * 0.35f;
        float circleCenterX = width / 2f;
        float circleCenterY = circleRadius + 5;
        canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, paint);
        
        // Dibujar triángulo inferior (punta del pin)
        android.graphics.Path trianglePath = new android.graphics.Path();
        trianglePath.moveTo(circleCenterX, circleCenterY + circleRadius);
        trianglePath.lineTo(circleCenterX - circleRadius * 0.6f, height - 5);
        trianglePath.lineTo(circleCenterX + circleRadius * 0.6f, height - 5);
        trianglePath.close();
        canvas.drawPath(trianglePath, paint);
        
        // Dibujar borde blanco alrededor del círculo
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFFFFFFFF);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        canvas.drawCircle(circleCenterX, circleCenterY, circleRadius - 2, borderPaint);
        
        // Si hay imagen del restaurante, dibujarla dentro del círculo
        if (imagenRestaurante != null) {
            // Crear un bitmap circular para la imagen
            int imageSize = (int) (circleRadius * 1.5f);
            Bitmap circularImage = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
            Canvas imageCanvas = new Canvas(circularImage);
            
            // Dibujar círculo de fondo blanco
            Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            imagePaint.setColor(0xFFFFFFFF);
            imageCanvas.drawCircle(imageSize / 2f, imageSize / 2f, imageSize / 2f - 2, imagePaint);
            
            // Crear máscara circular
            Bitmap scaledImage = Bitmap.createScaledBitmap(imagenRestaurante, imageSize - 8, imageSize - 8, true);
            Bitmap circularBitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
            Canvas circularCanvas = new Canvas(circularBitmap);
            circularCanvas.drawCircle(imageSize / 2f, imageSize / 2f, imageSize / 2f - 4, new Paint());
            
            Paint imagePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            imagePaint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            circularCanvas.drawBitmap(scaledImage, 4, 4, imagePaint2);
            
            // Dibujar la imagen circular en el marcador
            float imageX = circleCenterX - imageSize / 2f;
            float imageY = circleCenterY - imageSize / 2f;
            canvas.drawBitmap(circularBitmap, imageX, imageY, null);
        } else {
            // Si no hay imagen, dibujar un icono de restaurante por defecto
            Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconPaint.setColor(0xFF333333);
            iconPaint.setTextSize(circleRadius * 0.8f);
            iconPaint.setTextAlign(Paint.Align.CENTER);
            
            // Dibujar un símbolo simple (puedes cambiarlo por un icono)
            canvas.drawText("R", circleCenterX, circleCenterY + circleRadius * 0.3f, iconPaint);
        }
        
        return BitmapDescriptorFactory.fromBitmap(markerBitmap);
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
    
    /**
     * Crea 3 pedidos ficticios de prueba con estado "entregado_repartidor"
     * Estos pedidos se crean en restaurantes existentes o se crean restaurantes ficticios con coordenadas aleatorias dentro de 1km
     */
    private void crearPedidosPrueba() {
        if (ubicacionRepartidor == null) {
            Toast.makeText(requireContext(), "Espera a que se cargue tu ubicación", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double repartidorLat = ubicacionRepartidor.getLatitude();
        double repartidorLng = ubicacionRepartidor.getLongitude();
        
        Toast.makeText(requireContext(), "Creando pedidos de prueba...", Toast.LENGTH_SHORT).show();
        
        // Primero obtener los restaurantes existentes
        db.collection("restaurantes")
                .get()
                .addOnSuccessListener(restaurantesSnapshot -> {
                    List<String> restaurantesExistentes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : restaurantesSnapshot) {
                        restaurantesExistentes.add(doc.getId());
                    }
                    
                    // Si hay menos de 3 restaurantes, crear restaurantes ficticios
                    String[] restaurantesPrueba = new String[3];
                    double[] lats = new double[3];
                    double[] lngs = new double[3];
                    
                    java.util.Random random = new java.util.Random();
                    
                    // Generar coordenadas aleatorias dentro de un radio de 1km
                    for (int i = 0; i < 3; i++) {
                        // Generar ángulo aleatorio (0 a 2π)
                        double angulo = random.nextDouble() * 2 * Math.PI;
                        
                        // Generar distancia aleatoria (0 a 1000 metros, pero preferir entre 200m y 800m para mejor distribución)
                        double distanciaMetros = 200 + random.nextDouble() * 600; // Entre 200m y 800m
                        
                        // Convertir distancia a grados (aproximadamente)
                        // 1 grado de latitud ≈ 111 km
                        // 1 grado de longitud ≈ 111 km * cos(latitud)
                        double latOffset = (distanciaMetros / 111000.0) * Math.cos(angulo);
                        double lngOffset = (distanciaMetros / (111000.0 * Math.cos(Math.toRadians(repartidorLat)))) * Math.sin(angulo);
                        
                        lats[i] = repartidorLat + latOffset;
                        lngs[i] = repartidorLng + lngOffset;
                        
                        // Usar restaurante existente si hay, sino crear uno ficticio
                        if (i < restaurantesExistentes.size()) {
                            restaurantesPrueba[i] = restaurantesExistentes.get(i);
                        } else {
                            restaurantesPrueba[i] = "Restaurante Prueba " + (i + 1);
                        }
                        
                        Log.d(TAG, "Pedido prueba " + (i + 1) + " - Restaurante: " + restaurantesPrueba[i] + ", Distancia: " + String.format(Locale.getDefault(), "%.0f", distanciaMetros) + "m, Ángulo: " + String.format(Locale.getDefault(), "%.1f", Math.toDegrees(angulo)) + "°");
                    }
                    
                    // Crear los pedidos
                    final int[] pedidosCreados = {0};
                    for (int i = 0; i < 3; i++) {
                        final int index = i;
                        String restauranteName = restaurantesPrueba[i];
                        String pedidoId = "PRUEBA" + System.currentTimeMillis() + i;
                        
                        // Crear datos del pedido
                        Map<String, Object> pedidoData = new HashMap<>();
                        pedidoData.put("id", pedidoId);
                        pedidoData.put("restaurante", restauranteName);
                        pedidoData.put("direccion", "Dirección de prueba " + (i + 1));
                        pedidoData.put("total", 15.50 + (i * 5.0));
                        pedidoData.put("subtotal", 14.00 + (i * 5.0));
                        pedidoData.put("delivery", 1.50);
                        pedidoData.put("estado", "entregado_repartidor");
                        pedidoData.put("fecha", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        pedidoData.put("tiempoEstimado", 30);
                        pedidoData.put("restauranteLat", lats[i]);
                        pedidoData.put("restauranteLng", lngs[i]);
                        pedidoData.put("clienteEmail", "cliente.prueba" + i + "@test.com");
                        
                        // Crear productos ficticios
                        List<Map<String, Object>> productos = new ArrayList<>();
                        for (int j = 0; j < 2; j++) {
                            Map<String, Object> producto = new HashMap<>();
                            producto.put("nombre", "Producto Prueba " + (i + 1) + "-" + (j + 1));
                            producto.put("descripcion", "Descripción de prueba");
                            producto.put("precio", 7.00 + (j * 2.0));
                            producto.put("cantidad", 1);
                            producto.put("imagenResId", R.drawable.ic_profile);
                            productos.add(producto);
                        }
                        pedidoData.put("productos", productos);
                        
                        // Primero asegurar que el documento del restaurante existe
                        Map<String, Object> restauranteData = new HashMap<>();
                        restauranteData.put("nombre", restauranteName);
                        restauranteData.put("activo", true);
                        
                        db.collection("restaurantes").document(restauranteName)
                                .set(restauranteData, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    // Ahora guardar el pedido
                                    db.collection("restaurantes").document(restauranteName)
                                            .collection("pedidos_pendientes").document(pedidoId)
                                            .set(pedidoData)
                                            .addOnSuccessListener(aVoid2 -> {
                                                pedidosCreados[0]++;
                                                Log.d(TAG, "Pedido de prueba creado: " + pedidoId + " (" + pedidosCreados[0] + "/3)");
                                                if (pedidosCreados[0] == 3) {
                                                    // Cuando se crean todos los pedidos, recargar la lista
                                                    Toast.makeText(requireContext(), "3 pedidos de prueba creados correctamente", Toast.LENGTH_SHORT).show();
                                                    cargarPedidosDisponibles();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                pedidosCreados[0]++;
                                                Log.e(TAG, "Error al crear pedido de prueba " + (index + 1) + ": " + e.getMessage());
                                                if (pedidosCreados[0] == 3) {
                                                    Toast.makeText(requireContext(), "Algunos pedidos de prueba no se pudieron crear", Toast.LENGTH_SHORT).show();
                                                    cargarPedidosDisponibles();
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    pedidosCreados[0]++;
                                    Log.e(TAG, "Error al crear/actualizar restaurante " + restauranteName + ": " + e.getMessage());
                                    if (pedidosCreados[0] == 3) {
                                        Toast.makeText(requireContext(), "Algunos pedidos de prueba no se pudieron crear", Toast.LENGTH_SHORT).show();
                                        cargarPedidosDisponibles();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener restaurantes: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error al obtener restaurantes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
