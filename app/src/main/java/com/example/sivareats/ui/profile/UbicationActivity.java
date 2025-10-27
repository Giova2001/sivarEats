package com.example.sivareats.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sivareats.R;
import com.example.sivareats.adapters.UbicationAdapter;
import com.example.sivareats.data.Ubicacion;
import com.example.sivareats.viewmodel.UbicacionViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class UbicationActivity extends AppCompatActivity {

    private UbicacionViewModel viewModel;
    private RecyclerView rvPreferred, rvMine;
    private TextView tvEmpty;
    private UbicationAdapter adapterPreferred, adapterMine;
    private FloatingActionButton fabAddLocation;
    private ActivityResultLauncher<Intent> locationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ubication);

        // Views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        rvPreferred = findViewById(R.id.rvPreferred);
        rvMine = findViewById(R.id.rvMine);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAddLocation = findViewById(R.id.fabAddLocation);

        // Toolbar back
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // ViewModel
        viewModel = new ViewModelProvider(this).get(UbicacionViewModel.class);

        // RecyclerViews setup
        rvPreferred.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMine.setLayoutManager(new LinearLayoutManager(this));

        // Adapter listener
        UbicationAdapter.OnItemClickListener listenerForMine = new UbicationAdapter.OnItemClickListener() {
            @Override
            public void onEdit(Ubicacion u) {
                // Lanzar editor con objeto
                Intent i = new Intent(UbicationActivity.this, LocationEditActivity.class);
                i.putExtra("ubicacion_obj", u); // serializable
                locationLauncher.launch(i);
            }

            @Override
            public void onDelete(Ubicacion u) {
                viewModel.eliminar(u); // borra local; si quieres borrar en Firestore necesitas id remoto
            }

            @Override
            public void onClick(Ubicacion u) {
                // Evento click item (si quieres marcar favorita o seleccionar)
            }
        };

        // Adapters
        adapterPreferred = new UbicationAdapter(new ArrayList<>(), this, listenerForMine);
        adapterMine = new UbicationAdapter(new ArrayList<>(), this, listenerForMine);

        rvPreferred.setAdapter(adapterPreferred);
        rvMine.setAdapter(adapterMine);

        // Launcher para recibir resultado de creación/edición
        locationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (ActivityResult result) -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // LiveData actualizará automáticamente; puedes mostrar toast o scroll si deseas
                    }
                }
        );

        // Observa LiveData
        viewModel.obtenerTodas().observe(this, ubicaciones -> {
            if (ubicaciones != null && !ubicaciones.isEmpty()) {
                tvEmpty.setVisibility(TextView.GONE);
                adapterMine.setUbicaciones(ubicaciones);

                if (ubicaciones.size() >= 2) {
                    adapterPreferred.setUbicaciones(ubicaciones.subList(0, 2));
                } else {
                    adapterPreferred.setUbicaciones(ubicaciones);
                }
            } else {
                tvEmpty.setVisibility(TextView.VISIBLE);
                adapterMine.setUbicaciones(new ArrayList<>());
                adapterPreferred.setUbicaciones(new ArrayList<>());
            }
        });

        // FAB: crear nueva ubicación
        fabAddLocation.setOnClickListener(v -> {
            Intent i = new Intent(UbicationActivity.this, LocationEditActivity.class);
            locationLauncher.launch(i);
        });
    }
}
