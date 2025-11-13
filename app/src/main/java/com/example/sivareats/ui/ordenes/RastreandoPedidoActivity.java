package com.example.sivareats.ui.ordenes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.gms.tasks.OnSuccessListener;

public class RastreandoPedidoActivity extends AppCompatActivity implements OnMapReadyCallback {

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
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean isInfoVisible = true;
    
    private String pedidoId;
    private String restaurante;
    private String repartidorNombre;
    private String repartidorTelefono;
    private int tiempoEstimado;

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

        // Obtener datos del intent
        Intent intent = getIntent();
        pedidoId = intent.getStringExtra("pedido_id");
        restaurante = intent.getStringExtra("restaurante");
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

        // Botón volver
        btnBack.setOnClickListener(v -> finish());

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
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                // Ocultar información cuando el usuario mueve el mapa manualmente
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    ocultarInformacion();
                }
            }
        });

        // Mostrar información cuando el mapa deja de moverse
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                // Mostrar información después de un breve delay
                bottomSheetTracking.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mostrarInformacion();
                    }
                }, 500);
            }
        });

        // Ubicación de ejemplo (San Salvador, El Salvador)
        LatLng sanSalvador = new LatLng(13.6929, -89.2182);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sanSalvador, 15f));
        
        // Agregar marcador de ejemplo para el restaurante
        mMap.addMarker(new MarkerOptions()
                .position(sanSalvador)
                .title("Restaurante"));
    }

    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null && mMap != null) {
                            LatLng miUbicacion = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions()
                                    .position(miUbicacion)
                                    .title("Mi ubicación"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15f));
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
                    }
                }
            }
        }
    }
}
