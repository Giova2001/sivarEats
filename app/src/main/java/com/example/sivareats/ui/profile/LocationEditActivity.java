package com.example.sivareats.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.sivareats.R;
import com.example.sivareats.data.Ubicacion;
import com.example.sivareats.utils.UbicacionViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class LocationEditActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextInputEditText etNombreLugar, etDireccion, etTipo, etDescripcion, etDepto;
    private MaterialButton btnGuardar;
    private UbicacionViewModel viewModel;
    private Ubicacion ubicacionEditing;
    private boolean isEditing = false;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_edit);

        viewModel = new ViewModelProvider(this).get(UbicacionViewModel.class);
        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etNombreLugar = findViewById(R.id.etNombreLugar);
        etDireccion = findViewById(R.id.etDireccion);
        etTipo = findViewById(R.id.etTipo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etDepto = findViewById(R.id.etDepto);
        btnGuardar = findViewById(R.id.btnGuardar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.map), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (getIntent() != null && getIntent().hasExtra("ubicacion_obj")) {
            Object obj = getIntent().getSerializableExtra("ubicacion_obj");
            if (obj instanceof Ubicacion) {
                ubicacionEditing = (Ubicacion) obj;
                isEditing = true;
                cargarDatosExistentes();
            }
        }

        btnGuardar.setOnClickListener(v -> guardarYSalir());
    }

    private void cargarDatosExistentes() {
        etNombreLugar.setText(ubicacionEditing.getNombreLugar());
        etDireccion.setText(ubicacionEditing.getDireccion());
        etTipo.setText(ubicacionEditing.getTipo());
        etDescripcion.setText(ubicacionEditing.getDescripcion());
        etDepto.setText(ubicacionEditing.getDepto());
    }

    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();

                if (mMap != null) {
                    LatLng posicion = new LatLng(currentLat, currentLng);
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(posicion).title("Mi ubicación actual"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 17));
                }
            } else {
                Toast.makeText(LocationEditActivity.this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarYSalir() {
        String nombre = etNombreLugar.getText() != null ? etNombreLugar.getText().toString().trim() : "";
        String direccion = etDireccion.getText() != null ? etDireccion.getText().toString().trim() : "";
        String tipo = etTipo.getText() != null ? etTipo.getText().toString().trim() : "";
        String descripcion = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
        String depto = etDepto.getText() != null ? etDepto.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(direccion)) {
            Toast.makeText(this, "Nombre y dirección son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint geoPoint = new GeoPoint(currentLat, currentLng);
        String geoText = geoPointToString(geoPoint);

        if (isEditing && ubicacionEditing != null) {
            ubicacionEditing.setNombreLugar(nombre);
            ubicacionEditing.setDireccion(direccion);
            ubicacionEditing.setTipo(tipo);
            ubicacionEditing.setDescripcion(descripcion);
            ubicacionEditing.setDepto(depto);
            ubicacionEditing.setCoordenadas(geoText);

            viewModel.actualizar(ubicacionEditing);
            Toast.makeText(this, "Ubicación actualizada correctamente", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();

        } else {
            Ubicacion nueva = new Ubicacion(nombre, direccion, tipo, depto, descripcion, false);
            nueva.setCoordenadas(geoText);

            viewModel.insertar(nueva);
            Toast.makeText(this, "Ubicación guardada correctamente", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (isEditing && ubicacionEditing != null && ubicacionEditing.getCoordenadas() != null && !ubicacionEditing.getCoordenadas().isEmpty()) {
            try {
                String[] parts = ubicacionEditing.getCoordenadas().split(",");
                if (parts.length == 2) {
                    double lat = Double.parseDouble(parts[0]);
                    double lng = Double.parseDouble(parts[1]);
                    currentLat = lat;
                    currentLng = lng;
                    LatLng posicion = new LatLng(lat, lng);
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(posicion).title(ubicacionEditing.getNombreLugar()));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 17));
                } else {
                    obtenerUbicacionActual();
                }
            } catch (NumberFormatException e) {
                obtenerUbicacionActual();
            }
        } else {
            obtenerUbicacionActual();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual();
            } else {
                Toast.makeText(this, "Se necesita permiso de ubicación para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String geoPointToString(GeoPoint geoPoint) {
        if (geoPoint == null) return "";
        return geoPoint.getLatitude() + "," + geoPoint.getLongitude();
    }
}
