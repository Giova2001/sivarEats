package com.example.sivareats.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.example.sivareats.viewmodel.UbicacionViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
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

        // ViewModel y Firestore
        viewModel = new ViewModelProvider(this).get(UbicacionViewModel.class);
        db = FirebaseFirestore.getInstance();

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Views
        etNombreLugar = findViewById(R.id.etNombreLugar);
        etDireccion = findViewById(R.id.etDireccion);
        etTipo = findViewById(R.id.etTipo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etDepto = findViewById(R.id.etDepto);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.map), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Revisar si estamos editando
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

    // ---------------------------------------------------------------------------------------------
    // OBTENER UBICACIÓN ACTUAL
    // ---------------------------------------------------------------------------------------------
    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
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
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // GUARDAR / ACTUALIZAR EN FIREBASE + ROOM
    // ---------------------------------------------------------------------------------------------
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
            // 🔹 ACTUALIZAR
            ubicacionEditing.setNombreLugar(nombre);
            ubicacionEditing.setDireccion(direccion);
            ubicacionEditing.setTipo(tipo);
            ubicacionEditing.setDescripcion(descripcion);
            ubicacionEditing.setDepto(depto);
            ubicacionEditing.setCoordenadas(geoText);

            if (ubicacionEditing.getIdRemoto() != null) {
                db.collection("ubicaciones")
                        .document(ubicacionEditing.getIdRemoto())
                        .set(ubicacionEditing)
                        .addOnSuccessListener(aVoid -> {
                            viewModel.actualizar(ubicacionEditing);
                            Toast.makeText(this, "Ubicación actualizada correctamente", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Error al actualizar", e);
                            Toast.makeText(this, "Error al actualizar en Firestore", Toast.LENGTH_SHORT).show();
                        });
            }

        } else {
            // 🔹 CREAR NUEVA
            Ubicacion nueva = new Ubicacion(nombre, direccion, tipo, depto, descripcion, false);
            nueva.setCoordenadas(geoText);

            DocumentReference ref = db.collection("ubicaciones").document();
            nueva.setIdRemoto(ref.getId());

            ref.set(nueva)
                    .addOnSuccessListener(aVoid -> {
                        viewModel.insertar(nueva);
                        Toast.makeText(this, "Ubicación guardada correctamente", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error al guardar", e);
                        Toast.makeText(this, "Error al guardar en Firestore", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // ---------------------------------------------------------------------------------------------
    // CALLBACK DEL MAPA
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        obtenerUbicacionActual();
    }

    // ---------------------------------------------------------------------------------------------
    // MANEJO DE PERMISOS
    // ---------------------------------------------------------------------------------------------
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

    // ---------------------------------------------------------------------------------------------
    // CONVERTIR GEOPOINT A TEXTO
    // ---------------------------------------------------------------------------------------------
    private String geoPointToString(GeoPoint geoPoint) {
        if (geoPoint == null) return "";
        return geoPoint.getLatitude() + "," + geoPoint.getLongitude();
    }
}
