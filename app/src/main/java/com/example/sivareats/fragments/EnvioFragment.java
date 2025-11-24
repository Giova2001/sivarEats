package com.example.sivareats.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.sivareats.R;
import com.example.sivareats.ui.NavegacionActivity;
import com.example.sivareats.ui.checkout.SeleccionarMetodoPagoActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class EnvioFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker selectedMarker;
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView btnBack;
    private ImageView btnMiUbicacion;
    private Button btnConfirmar;

    private static final int REQUEST_LOCATION = 1001;

    public EnvioFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_envio, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        btnBack = view.findViewById(R.id.btnBackEnvio);
        btnConfirmar = view.findViewById(R.id.btnConfirmarEnvio);
        btnMiUbicacion = view.findViewById(R.id.btnMiUbicacion);

        // Regresar al carrito
        btnBack.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

            // Intentar usar popBackStack primero
            if (fragmentManager.getBackStackEntryCount() > 0) {
                fragmentManager.popBackStack();
            } else {
                // Si no hay back stack, buscar y mostrar el CartFragment
                Fragment cartFragment = fragmentManager.findFragmentByTag("CartFragment");
                if (cartFragment != null && !cartFragment.isVisible()) {
                    fragmentManager.beginTransaction()
                            .hide(this)
                            .show(cartFragment)
                            .commit();
                } else {
                    // Si no existe o no se puede mostrar, usar popBackStack o navegar
                    // Esto debería funcionar con la nueva implementación de loadFragment
                    fragmentManager.popBackStackImmediate();
                }
            }
        });

        // Accion del mapa
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapEnvio);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Botón Mi Ubicación (YA dentro del mapa)
        btnMiUbicacion.setOnClickListener(v -> centrarEnMiUbicacion());

        // Confirmar ubicacion y proceder al pago
        btnConfirmar.setOnClickListener(v -> {
            if (selectedMarker == null) {
                Toast.makeText(requireContext(),
                        "Selecciona una ubicación en el mapa",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Abrir activity de selección de método de pago
            Intent intent = new Intent(requireContext(), SeleccionarMetodoPagoActivity.class);
            // Opcional: pasar la ubicación seleccionada como extra
            intent.putExtra("latitud", selectedMarker.getPosition().latitude);
            intent.putExtra("longitud", selectedMarker.getPosition().longitude);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
            return;
        }

        mMap.setMyLocationEnabled(true);

        // Seleccionar punto tocando el mapa
        mMap.setOnMapClickListener(latLng -> {
            if (selectedMarker != null) selectedMarker.remove();

            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Ubicación seleccionada"));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        });

        // Centrar en ubicación actual al iniciar
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng actual = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(actual, 15));
                    }
                });
    }

    private void centrarEnMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(requireContext(),
                                "No se pudo obtener tu ubicación",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LatLng actual = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(actual, 16));
                });
    }
}