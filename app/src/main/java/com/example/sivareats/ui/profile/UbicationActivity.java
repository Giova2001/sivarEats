package com.example.sivareats.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        rvPreferred = findViewById(R.id.rvPreferred);
        rvMine = findViewById(R.id.rvMine);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAddLocation = findViewById(R.id.fabAddLocation);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        viewModel = new ViewModelProvider(this).get(UbicacionViewModel.class);

        rvPreferred.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMine.setLayoutManager(new LinearLayoutManager(this));

        UbicationAdapter.OnItemClickListener listener = new UbicationAdapter.OnItemClickListener() {
            @Override
            public void onEdit(Ubicacion u) {
                Intent i = new Intent(UbicationActivity.this, LocationEditActivity.class);
                i.putExtra("ubicacion_obj", u);
                locationLauncher.launch(i);
            }

            @Override
            public void onDelete(Ubicacion u) {
                viewModel.eliminar(u);
                if (u.getIdRemoto() != null && !u.getIdRemoto().isEmpty()) {
                    FirebaseFirestore.getInstance().collection("ubicaciones")
                            .document(u.getIdRemoto())
                            .delete()
                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "Ubicación eliminada de Firestore"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error al eliminar en Firestore", e));
                }
            }

            @Override
            public void onClick(Ubicacion u) {
                // Not used for now
            }

            @Override
            public void onFavoriteClick(Ubicacion u) {
                u.setPreferida(!u.isPreferida());
                viewModel.actualizar(u);
                String message = u.isPreferida() ? "Marcada como favorita" : "Quitada de favoritas";
                Toast.makeText(UbicationActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        };

        adapterPreferred = new UbicationAdapter(new ArrayList<>(), this, listener);
        adapterMine = new UbicationAdapter(new ArrayList<>(), this, listener);

        rvPreferred.setAdapter(adapterPreferred);
        rvMine.setAdapter(adapterMine);

        locationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Ubicaciones actualizadas", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        viewModel.obtenerTodas().observe(this, ubicaciones -> {
            if (ubicaciones == null || ubicaciones.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvPreferred.setVisibility(View.GONE);
                rvMine.setVisibility(View.GONE);
                adapterMine.setUbicaciones(new ArrayList<>());
                adapterPreferred.setUbicaciones(new ArrayList<>());
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvMine.setVisibility(View.VISIBLE);

                List<Ubicacion> preferred = new ArrayList<>();
                for (Ubicacion u : ubicaciones) {
                    if (u.isPreferida()) {
                        preferred.add(u);
                    }
                }

                if (preferred.isEmpty()) {
                    rvPreferred.setVisibility(View.GONE);
                } else {
                    rvPreferred.setVisibility(View.VISIBLE);
                }

                adapterPreferred.setUbicaciones(preferred);
                adapterMine.setUbicaciones(ubicaciones);
            }
        });

        fabAddLocation.setOnClickListener(v -> {
            Intent i = new Intent(UbicationActivity.this, LocationEditActivity.class);
            locationLauncher.launch(i);
        });
    }
}
