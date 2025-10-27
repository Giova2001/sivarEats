package com.example.sivareats.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.sivareats.R;
import com.example.sivareats.data.Ubicacion;
import com.example.sivareats.viewmodel.UbicacionViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LocationEditActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextInputEditText etNombreLugar, etDireccion, etTipo, etDescripcion, etDepto;
    private MaterialButton btnGuardar;
    private UbicacionViewModel viewModel;
    private Ubicacion ubicacionEditing;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_edit);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(UbicacionViewModel.class);

        // Views
        etNombreLugar = findViewById(R.id.etNombreLugar);
        etDireccion = findViewById(R.id.etDireccion);
        etTipo = findViewById(R.id.etTipo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etDepto = findViewById(R.id.etDepto);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.map), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Si viene una Ubicacion (editar)
        if (getIntent() != null && getIntent().hasExtra("ubicacion_obj")) {
            Object obj = getIntent().getSerializableExtra("ubicacion_obj");
            if (obj instanceof Ubicacion) {
                ubicacionEditing = (Ubicacion) obj;
                isEditing = true;
                // llenar campos
                etNombreLugar.setText(ubicacionEditing.getNombreLugar());
                etDireccion.setText(ubicacionEditing.getDireccion());
                etTipo.setText(ubicacionEditing.getTipo());
                etDescripcion.setText(ubicacionEditing.getDescripcion());
                etDepto.setText(ubicacionEditing.getDepto());
            }
        }

        btnGuardar.setOnClickListener(v -> {
            guardarYSalir();
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

        if (isEditing && ubicacionEditing != null) {
            ubicacionEditing.setNombreLugar(nombre);
            ubicacionEditing.setDireccion(direccion);
            ubicacionEditing.setTipo(tipo);
            ubicacionEditing.setDescripcion(descripcion);
            ubicacionEditing.setDepto(depto);
            viewModel.actualizar(ubicacionEditing);
        } else {
            Ubicacion nueva = new Ubicacion(nombre, direccion, tipo, depto, descripcion, false);
            viewModel.insertar(nueva);
        }

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Coordenadas por defecto
        double lat = 13.71622;
        double lng = -89.20323;
        LatLng posicion = new LatLng(lat, lng);

        // Mueve la cámara
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15));

        // Agrega el marcador
        mMap.addMarker(new MarkerOptions()
                .position(posicion)
                .title(etNombreLugar.getText().toString().isEmpty() ? "Ubicación" : etNombreLugar.getText().toString()));

        // Controles del mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }
}
