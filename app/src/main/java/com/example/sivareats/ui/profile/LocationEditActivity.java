package com.example.sivareats.ui.profile;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sivareats.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationEditActivity extends AppCompatActivity {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_edit);

        // Inicializar el fragmento del mapa
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.main);
        mapFragment.getMapAsync((OnMapReadyCallback) this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


    }


    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Coordenadas de UES, San Salvador
        LatLng ues = new LatLng(13.71622, -89.20323);
        // Agregar marcador en esa ubicación
        mMap.addMarker(new MarkerOptions()
                .position(ues)
                .title("Universidad de El Salvador - San Salvador"));
        // Mover la cámara al marcador con zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ues, 15));

        // Mover la cámara al marcador con zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ues, 15));
        // -----------------------------
        // HABILITAR CONTROLES DE MAPA
        // -----------------------------
        mMap.getUiSettings().setZoomControlsEnabled(true); // Botones de zoom (+/-)
        mMap.getUiSettings().setCompassEnabled(true); // Brújula (aparece al rotar el mapa)
        mMap.getUiSettings().setMapToolbarEnabled(true); // Barra de herramientas (cuando se toca un marcador)
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Botón "mi ubicación"
        mMap.getUiSettings().setAllGesturesEnabled(true); // Permitir gestos: zoom, mover, rotar, inclinar
        // Cambiar el tipo de mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // NORMAL, SATELLITE, HYBRID, TERRAIN
    }
}