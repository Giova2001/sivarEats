package com.example.sivareats.ui.ordenes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sivareats.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RastreandoPedidoActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "RastreandoPedido";
    private GoogleMap mMap;
    private MaterialCardView bottomSheetTracking;
    private TextView tvLlegadaTiempo;
    private TextView tvLlegadaTitulo;
    private TextView tvPedidoItem;
    private TextView tvPedidoRestaurante;
    private TextView tvRepartidorNombre;
    private TextView tvRepartidorTelefono;
    private ImageButton btnBack;
    private ImageButton btnChatear;
    private ImageButton btnLlamar;
    
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean isInfoVisible = true;
    
    private String pedidoId;
    private String restaurante;
    private String direccion;
    private String clienteEmail;
    private boolean isRepartidor;
    private String repartidorNombre;
    private String repartidorTelefono;
    private int tiempoEstimado;
    
    private LatLng ubicacionRepartidor;
    private LatLng ubicacionRestaurante;
    private LatLng ubicacionCliente;
    
    private Polyline polylineRestaurante;
    private Polyline polylineCliente;
    private Marker markerRepartidor;
    private Marker markerRestaurante;
    private Marker markerCliente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rastreando_pedido);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Obtener datos del intent
        Intent intent = getIntent();
        pedidoId = intent.getStringExtra("pedido_id");
        restaurante = intent.getStringExtra("restaurante");
        direccion = intent.getStringExtra("direccion");
        clienteEmail = intent.getStringExtra("cliente_email");
        isRepartidor = intent.getBooleanExtra("is_repartidor", false);
        repartidorNombre = intent.getStringExtra("repartidor_nombre");
        repartidorTelefono = intent.getStringExtra("repartidor_telefono");
        tiempoEstimado = intent.getIntExtra("tiempo_estimado", 30);

        // Inicializar vistas
        bottomSheetTracking = findViewById(R.id.bottom_sheet_tracking);
        tvLlegadaTiempo = findViewById(R.id.tv_llegada_tiempo);
        tvLlegadaTitulo = findViewById(R.id.tv_llegada_titulo);
        tvPedidoItem = findViewById(R.id.tv_pedido_item);
        tvPedidoRestaurante = findViewById(R.id.tv_pedido_restaurante);
        tvRepartidorNombre = findViewById(R.id.tv_repartidor_nombre);
        tvRepartidorTelefono = findViewById(R.id.tv_repartidor_telefono);
        btnBack = findViewById(R.id.btn_back);
        btnChatear = findViewById(R.id.btn_chatear);
        btnLlamar = findViewById(R.id.btn_llamar);

        // Configurar datos en las vistas
        if (restaurante != null) {
            tvPedidoRestaurante.setText(restaurante);
        }
        if (repartidorNombre != null) {
            tvRepartidorNombre.setText(repartidorNombre);
        }
        if (repartidorTelefono != null) {
            tvRepartidorTelefono.setText("Delivery " + repartidorTelefono);
        }
        if (tiempoEstimado > 0) {
            tvLlegadaTiempo.setText("Llegando en " + tiempoEstimado + " minutos");
        }
        
        if (isRepartidor) {
            tvLlegadaTitulo.setText("Rastrear pedido");
        }

        // Botón volver
        btnBack.setOnClickListener(v -> finish());
        
        // Botones de contacto
        btnLlamar.setOnClickListener(v -> {
            if (repartidorTelefono != null && !repartidorTelefono.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + repartidorTelefono));
                startActivity(callIntent);
            }
        });
        
        btnChatear.setOnClickListener(v -> {
            // Implementar chat si es necesario
            Toast.makeText(this, "Función de chat próximamente", Toast.LENGTH_SHORT).show();
        });

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtener el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Solicitar permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            
            // Obtener ubicación actual
            obtenerUbicacionActual();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Configurar listener para detectar cuando se mueve el mapa
        mMap.setOnCameraMoveStartedListener(reason -> {
            // Ocultar información cuando el usuario mueve el mapa manualmente
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                ocultarInformacion();
            }
        });

        // Mostrar información cuando el mapa deja de moverse
        mMap.setOnCameraIdleListener(() -> {
            // Mostrar información después de un breve delay
            bottomSheetTracking.postDelayed(() -> mostrarInformacion(), 500);
        });

        // Si es repartidor, cargar datos del pedido y trazar rutas
        if (isRepartidor && pedidoId != null) {
            cargarDatosPedidoYTrazarRutas();
        } else {
            // Ubicación de ejemplo (San Salvador, El Salvador)
            LatLng sanSalvador = new LatLng(13.6929, -89.2182);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sanSalvador, 15f));
            
            // Agregar marcador de ejemplo para el restaurante
            mMap.addMarker(new MarkerOptions()
                    .position(sanSalvador)
                    .title("Restaurante"));
        }
    }

    private void cargarDatosPedidoYTrazarRutas() {
        // Obtener ubicación actual del repartidor
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        ubicacionRepartidor = new LatLng(location.getLatitude(), location.getLongitude());
                        
                        // Buscar el pedido en Firebase
                        // Primero buscar en la colección del repartidor
                        String repartidorEmail = getSharedPreferences("user_prefs", MODE_PRIVATE)
                                .getString("user_email", "");
                        
                        if (repartidorEmail != null && !repartidorEmail.isEmpty()) {
                            db.collection("users").document(repartidorEmail)
                                    .collection("pedidos").document(pedidoId)
                                    .get()
                                    .addOnSuccessListener(document -> {
                                        if (document.exists()) {
                                            cargarCoordenadasYTrazarRutas(document);
                                        } else {
                                            // Si no está en la colección del repartidor, buscar en restaurantes
                                            buscarPedidoEnRestaurantes();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error al cargar pedido: " + e.getMessage());
                                        buscarPedidoEnRestaurantes();
                                    });
                        } else {
                            buscarPedidoEnRestaurantes();
                        }
                    }
                });
    }
    
    private void buscarPedidoEnRestaurantes() {
        if (restaurante == null || restaurante.isEmpty()) {
            Toast.makeText(this, "No se pudo encontrar el restaurante", Toast.LENGTH_SHORT).show();
            return;
        }
        
        db.collection("restaurantes").document(restaurante)
                .collection("pedidos_pendientes").document(pedidoId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        cargarCoordenadasYTrazarRutas(document);
                    } else {
                        Toast.makeText(this, "No se pudo encontrar el pedido", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al buscar pedido en restaurantes: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar el pedido", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void cargarCoordenadasYTrazarRutas(DocumentSnapshot document) {
        // Obtener coordenadas del restaurante
        Double restauranteLat = document.getDouble("restauranteLat");
        Double restauranteLng = document.getDouble("restauranteLng");
        
        if (restauranteLat != null && restauranteLng != null) {
            ubicacionRestaurante = new LatLng(restauranteLat, restauranteLng);
        } else {
            // Si no hay coordenadas, usar coordenadas por defecto
            ubicacionRestaurante = new LatLng(13.6929, -89.2182);
        }
        
        // Obtener coordenadas del cliente
        if (clienteEmail != null && !clienteEmail.isEmpty()) {
            db.collection("users").document(clienteEmail)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            Double clienteLat = userDoc.getDouble("latitud");
                            Double clienteLng = userDoc.getDouble("longitud");
                            
                            if (clienteLat != null && clienteLng != null) {
                                ubicacionCliente = new LatLng(clienteLat, clienteLng);
                            } else {
                                // Si no hay coordenadas del cliente, usar la dirección para geocodificar
                                geocodificarDireccion(direccion, ubicacionRestaurante);
                            }
                            
                            // Trazar rutas
                            trazarRutas();
                        } else {
                            geocodificarDireccion(direccion, ubicacionRestaurante);
                            trazarRutas();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al cargar datos del cliente: " + e.getMessage());
                        geocodificarDireccion(direccion, ubicacionRestaurante);
                        trazarRutas();
                    });
        } else {
            geocodificarDireccion(direccion, ubicacionRestaurante);
            trazarRutas();
        }
    }
    
    private void geocodificarDireccion(String direccion, LatLng fallback) {
        // Por ahora usar coordenadas por defecto o fallback
        // En producción, usarías Google Geocoding API
        ubicacionCliente = fallback != null ? fallback : new LatLng(13.6929, -89.2182);
    }
    
    private void trazarRutas() {
        if (mMap == null || ubicacionRepartidor == null || ubicacionRestaurante == null || ubicacionCliente == null) {
            return;
        }
        
        // Limpiar marcadores y polilíneas anteriores
        if (markerRepartidor != null) markerRepartidor.remove();
        if (markerRestaurante != null) markerRestaurante.remove();
        if (markerCliente != null) markerCliente.remove();
        if (polylineRestaurante != null) polylineRestaurante.remove();
        if (polylineCliente != null) polylineCliente.remove();
        
        // Agregar marcadores
        markerRepartidor = mMap.addMarker(new MarkerOptions()
                .position(ubicacionRepartidor)
                .title("Mi ubicación"));
        
        markerRestaurante = mMap.addMarker(new MarkerOptions()
                .position(ubicacionRestaurante)
                .title("Restaurante: " + restaurante));
        
        markerCliente = mMap.addMarker(new MarkerOptions()
                .position(ubicacionCliente)
                .title("Cliente"));
        
        // Trazar ruta al restaurante
        trazarRuta(ubicacionRepartidor, ubicacionRestaurante, Color.BLUE, true);
        
        // Trazar ruta del restaurante al cliente
        trazarRuta(ubicacionRestaurante, ubicacionCliente, Color.GREEN, false);
        
        // Ajustar cámara para mostrar todas las ubicaciones
        ajustarCamara();
    }
    
    private void trazarRuta(LatLng origen, LatLng destino, int color, boolean esPrimeraRuta) {
        new Thread(() -> {
            try {
                // Obtener API key del manifest
                String apiKey = null;
                try {
                    android.content.pm.ApplicationInfo appInfo = getPackageManager()
                            .getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
                    Bundle bundle = appInfo.metaData;
                    if (bundle != null) {
                        apiKey = bundle.getString("com.google.android.geo.API_KEY");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al obtener API key: " + e.getMessage());
                }
                
                if (apiKey == null) {
                    apiKey = "AIzaSyDBf9_VgURZetYqAqf_V-CuB8r8qOB5bvI"; // Fallback
                }
                
                String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=" + origen.latitude + "," + origen.longitude +
                        "&destination=" + destino.latitude + "," + destino.longitude +
                        "&key=" + apiKey;
                
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray routes = jsonResponse.getJSONArray("routes");
                
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPolyline = overviewPolyline.getString("points");
                    
                    List<LatLng> decodedPath = decodePolyline(encodedPolyline);
                    
                    runOnUiThread(() -> {
                        PolylineOptions polylineOptions = new PolylineOptions()
                                .addAll(decodedPath)
                                .color(color)
                                .width(8);
                        
                        if (esPrimeraRuta) {
                            polylineRestaurante = mMap.addPolyline(polylineOptions);
                        } else {
                            polylineCliente = mMap.addPolyline(polylineOptions);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al trazar ruta: " + e.getMessage());
                // Si falla la API, trazar línea recta
                runOnUiThread(() -> {
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .add(origen, destino)
                            .color(color)
                            .width(8);
                    
                    if (esPrimeraRuta) {
                        polylineRestaurante = mMap.addPolyline(polylineOptions);
                    } else {
                        polylineCliente = mMap.addPolyline(polylineOptions);
                    }
                });
            }
        }).start();
    }
    
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;
        
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            
            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        
        return poly;
    }
    
    private void ajustarCamara() {
        if (ubicacionRepartidor == null || ubicacionRestaurante == null || ubicacionCliente == null) {
            return;
        }
        
        // Calcular bounds para incluir todos los puntos
        double minLat = Math.min(Math.min(ubicacionRepartidor.latitude, ubicacionRestaurante.latitude), ubicacionCliente.latitude);
        double maxLat = Math.max(Math.max(ubicacionRepartidor.latitude, ubicacionRestaurante.latitude), ubicacionCliente.latitude);
        double minLng = Math.min(Math.min(ubicacionRepartidor.longitude, ubicacionRestaurante.longitude), ubicacionCliente.longitude);
        double maxLng = Math.max(Math.max(ubicacionRepartidor.longitude, ubicacionRestaurante.longitude), ubicacionCliente.longitude);
        
        double centerLat = (minLat + maxLat) / 2;
        double centerLng = (minLng + maxLng) / 2;
        
        double latDelta = maxLat - minLat;
        double lngDelta = maxLng - minLng;
        double maxDelta = Math.max(latDelta, lngDelta);
        
        float zoom = 12f;
        if (maxDelta > 0.1) {
            zoom = 11f;
        } else if (maxDelta > 0.05) {
            zoom = 13f;
        } else {
            zoom = 15f;
        }
        
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(centerLat, centerLng), zoom));
    }

    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && mMap != null) {
                        ubicacionRepartidor = new LatLng(location.getLatitude(), location.getLongitude());
                        if (!isRepartidor) {
                            // Solo para usuarios normales, mostrar marcador
                            mMap.addMarker(new MarkerOptions()
                                    .position(ubicacionRepartidor)
                                    .title("Mi ubicación"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionRepartidor, 15f));
                        }
                    }
                });
    }

    private void ocultarInformacion() {
        if (isInfoVisible) {
            isInfoVisible = false;
            bottomSheetTracking.animate()
                    .translationY(bottomSheetTracking.getHeight())
                    .setDuration(300)
                    .start();
        }
    }

    private void mostrarInformacion() {
        if (!isInfoVisible) {
            isInfoVisible = true;
            bottomSheetTracking.animate()
                    .translationY(0)
                    .setDuration(300)
                    .start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        obtenerUbicacionActual();
                        if (isRepartidor) {
                            cargarDatosPedidoYTrazarRutas();
                        }
                    }
                }
            }
        }
    }
}
