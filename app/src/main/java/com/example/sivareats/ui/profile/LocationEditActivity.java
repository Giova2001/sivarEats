package com.example.sivareats.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.sivareats.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LocationEditActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextInputEditText etDepto, etDescripcion, etNombreLugar;
    private Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_edit);

        // Configurar toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etDepto = findViewById(R.id.etDepto);
        etDescripcion = findViewById(R.id.etDescripcion);
        etNombreLugar = findViewById(R.id.etNombreLugar);
        btnGuardar = findViewById(R.id.btnGuardar);


        // Configurar el mapa
        setupMap();

        // Configurar otros elementos...
        setupForm();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        FirebaseApp.initializeApp(this);

        // Llamar a la función que obtiene la ubicación actual
        getCurrentLocation();

        btnGuardar.setOnClickListener(v -> {
            String nombreLugar = etNombreLugar.getText().toString().trim();
            String descripcionLugar = etDescripcion.getText().toString().trim();
            String tipoLugar = etDepto.getText().toString().trim();

            if (nombreLugar.isEmpty() || descripcionLugar.isEmpty() || tipoLugar.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mMap != null) {
                LatLng ubicacionActual = mMap.getCameraPosition().target; // centro del mapa
                saveLocationToFirestore(nombreLugar, descripcionLugar, tipoLugar,
                        ubicacionActual.latitude, ubicacionActual.longitude);
            }
        });
    }


    private void getCurrentLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verificar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Pedir permisos si no están concedidos
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && mMap != null) {
                        // Crear coordenadas
                        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Mover cámara a la ubicación actual
                        mMap.clear(); // limpia marcadores previos
                        mMap.addMarker(new MarkerOptions()
                                .position(myLocation)
                                .title("Mi ubicación actual"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17f));

                        // Habilitar el botón de ubicación en el mapa
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mMap.setMyLocationEnabled(true);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveLocationToFirestore(String nombreLugar, String descripcionLugar, String tipoLugar, double latitud, double longitud) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Crear mapa de datos
        Map<String, Object> datosUbicacion = new HashMap<>();
        datosUbicacion.put("Descripcion_Lugar", descripcionLugar);
        datosUbicacion.put("Tipo_Lugar", tipoLugar);
        datosUbicacion.put("latitud_longitud", Arrays.asList(latitud, longitud));

        // Guardar en colección "DIRECCIONES_GUARDADAS" con nombre del lugar como documento
        db.collection("DIRECCIONES_GUARDADAS")
                .document(nombreLugar) // nombre del documento = nombre del lugar
                .set(datosUbicacion)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Ubicación guardada correctamente", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar la ubicación: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    private void setupForm() {
        // Configurar tus otros elementos del formulario aquí
        // ...
    }
}