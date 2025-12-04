package com.example.sivareats.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.sivareats.R;
import com.example.sivareats.data.Ubicacion;
import com.example.sivareats.ui.NavegacionActivity;
import com.example.sivareats.ui.checkout.SeleccionarMetodoPagoActivity;
import com.example.sivareats.ui.profile.LocationEditActivity;
import com.example.sivareats.viewmodel.UbicacionViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class EnvioFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker selectedMarker;
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView btnBack;
    private ImageView btnMiUbicacion;
    private Button btnConfirmar;
    private LinearLayout layoutDirecciones;
    private View opAgregar;
    private UbicacionViewModel ubicacionViewModel;
    private Ubicacion ubicacionSeleccionada;
    private View direccionSeleccionadaView;
    private String coordenadasUltimaGuardada = null; // Para seleccionar automáticamente después de guardar

    private static final int REQUEST_LOCATION = 1001;
    private static final String TAG = "EnvioFragment";

    public EnvioFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_envio, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        ubicacionViewModel = new ViewModelProvider(this).get(UbicacionViewModel.class);

        btnBack = view.findViewById(R.id.btnBackEnvio);
        btnConfirmar = view.findViewById(R.id.btnConfirmarEnvio);
        btnMiUbicacion = view.findViewById(R.id.btnMiUbicacion);
        layoutDirecciones = view.findViewById(R.id.layoutDirecciones);
        opAgregar = view.findViewById(R.id.opAgregar);

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

        // Botón Mi Ubicación (YA dentro del mapa)
        btnMiUbicacion.setOnClickListener(v -> centrarEnMiUbicacion());

        // Botón agregar ubicación - navegar a la actividad de agregar ubicación
        opAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.sivareats.ui.profile.LocationEditActivity.class);
            startActivityForResult(intent, 1002); // Usar código para detectar cuando regrese
        });

        // Cargar direcciones guardadas
        cargarDirecciones();

        // Confirmar ubicacion y proceder al pago
        btnConfirmar.setOnClickListener(v -> {
            if (selectedMarker == null && ubicacionSeleccionada == null) {
                Toast.makeText(requireContext(),
                        "Selecciona una ubicación en el mapa o una dirección guardada",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Abrir activity de selección de método de pago
            Intent intent = new Intent(requireContext(), SeleccionarMetodoPagoActivity.class);

            // Pasar la ubicación seleccionada con dirección completa
            if (ubicacionSeleccionada != null) {
                // Usar dirección guardada
                if (ubicacionSeleccionada.getCoordenadas() != null) {
                    String[] coords = ubicacionSeleccionada.getCoordenadas().split(",");
                    if (coords.length == 2) {
                        intent.putExtra("latitud", Double.parseDouble(coords[0]));
                        intent.putExtra("longitud", Double.parseDouble(coords[1]));
                    }
                }
                // Pasar dirección completa
                String direccionCompleta = ubicacionSeleccionada.getDireccion() != null
                        ? ubicacionSeleccionada.getDireccion()
                        : "";
                if (ubicacionSeleccionada.getNombreLugar() != null && !ubicacionSeleccionada.getNombreLugar().isEmpty()) {
                    direccionCompleta = ubicacionSeleccionada.getNombreLugar() + " - " + direccionCompleta;
                }
                intent.putExtra("direccion", direccionCompleta);
            } else if (selectedMarker != null) {
                // Usar marcador del mapa (ubicación temporal)
                intent.putExtra("latitud", selectedMarker.getPosition().latitude);
                intent.putExtra("longitud", selectedMarker.getPosition().longitude);
                intent.putExtra("direccion", "Ubicación seleccionada en el mapa");
            }

            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002 && resultCode == android.app.Activity.RESULT_OK) {
            // Recargar direcciones cuando el usuario regrese de agregar una ubicación
            cargarDirecciones();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar el mapa después de que la vista esté completamente creada
        initMap();
    }

    private void initMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapEnvio);
        if (mapFragment != null) {
            Log.d(TAG, "MapFragment encontrado, inicializando mapa...");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "ERROR: MapFragment no encontrado con ID mapEnvio");
        }
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

    private void cargarDirecciones() {
        ubicacionViewModel.obtenerTodas().observe(getViewLifecycleOwner(), ubicaciones -> {
            if (ubicaciones == null) return;

            // Guardar referencia a la última ubicación seleccionada antes de limpiar
            Ubicacion ultimaSeleccionada = ubicacionSeleccionada;
            int idUltimaSeleccionada = ultimaSeleccionada != null ? ultimaSeleccionada.getId() : -1;

            // Remover todas las vistas de direcciones
            layoutDirecciones.removeAllViews();

            // Agregar direcciones guardadas
            Ubicacion ubicacionASeleccionar = null;
            View vistaASeleccionar = null;

            for (Ubicacion ubicacion : ubicaciones) {
                View direccionView = crearVistaDireccion(ubicacion);
                layoutDirecciones.addView(direccionView);

                // Si esta era la última seleccionada, marcar para seleccionarla
                if (idUltimaSeleccionada == ubicacion.getId()) {
                    ubicacionASeleccionar = ubicacion;
                    vistaASeleccionar = direccionView;
                }

                // Si esta es la última guardada (por coordenadas), seleccionarla
                if (coordenadasUltimaGuardada != null && ubicacion.getCoordenadas() != null
                        && ubicacion.getCoordenadas().equals(coordenadasUltimaGuardada)) {
                    ubicacionASeleccionar = ubicacion;
                    vistaASeleccionar = direccionView;
                    coordenadasUltimaGuardada = null; // Limpiar para no repetir
                }
            }

            // Seleccionar la ubicación determinada
            if (ubicacionASeleccionar != null && vistaASeleccionar != null) {
                seleccionarDireccion(ubicacionASeleccionar, vistaASeleccionar);
            }

            // Agregar el botón de agregar al final
            layoutDirecciones.addView(opAgregar);
        });
    }

    private View crearVistaDireccion(Ubicacion ubicacion) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        LinearLayout direccionView = (LinearLayout) inflater.inflate(
                R.layout.item_direccion_envio, layoutDirecciones, false);

        ImageView icono = direccionView.findViewById(R.id.icono_direccion);
        TextView textoDireccion = direccionView.findViewById(R.id.texto_direccion);

        // Determinar icono según tipo
        String tipo = ubicacion.getTipo() != null ? ubicacion.getTipo().toLowerCase() : "";
        if (tipo.contains("casa")) {
            icono.setImageResource(R.drawable.ic_home);
        } else if (tipo.contains("oficina")) {
            icono.setImageResource(R.drawable.ic_star);
        } else {
            icono.setImageResource(R.drawable.ic_location_24);
        }

        // Mostrar nombre del lugar o dirección
        String texto = ubicacion.getNombreLugar() != null && !ubicacion.getNombreLugar().isEmpty()
                ? ubicacion.getNombreLugar()
                : (ubicacion.getDireccion() != null ? ubicacion.getDireccion() : "Ubicación");

        if (ubicacion.getTipo() != null && !ubicacion.getTipo().isEmpty()) {
            texto += " (" + ubicacion.getTipo() + ")";
        }

        textoDireccion.setText(texto);

        // Guardar el ID de la ubicación como tag para poder identificarla después
        direccionView.setTag(ubicacion.getId());

        // Click listener para seleccionar esta dirección
        direccionView.setOnClickListener(v -> {
            seleccionarDireccion(ubicacion, direccionView);
        });

        return direccionView;
    }

    private void seleccionarDireccion(Ubicacion ubicacion, View direccionView) {
        // Deseleccionar vista anterior
        if (direccionSeleccionadaView != null) {
            direccionSeleccionadaView.setBackgroundResource(R.drawable.caja_direccion);
        }

        // Seleccionar nueva vista
        direccionSeleccionadaView = direccionView;
        direccionSeleccionadaView.setBackgroundResource(R.drawable.caja_direccion_seleccionada);

        ubicacionSeleccionada = ubicacion;

        // Actualizar mapa con la ubicación seleccionada
        if (ubicacion.getCoordenadas() != null && !ubicacion.getCoordenadas().isEmpty() && mMap != null) {
            try {
                String[] coords = ubicacion.getCoordenadas().split(",");
                if (coords.length == 2) {
                    double lat = Double.parseDouble(coords[0]);
                    double lng = Double.parseDouble(coords[1]);
                    LatLng posicion = new LatLng(lat, lng);

                    // Limpiar marcador anterior
                    if (selectedMarker != null) {
                        selectedMarker.remove();
                    }

                    // Agregar nuevo marcador
                    selectedMarker = mMap.addMarker(new MarkerOptions()
                            .position(posicion)
                            .title(ubicacion.getNombreLugar() != null ? ubicacion.getNombreLugar() : "Ubicación seleccionada"));

                    // Centrar mapa en la ubicación
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16));
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error al parsear coordenadas: " + e.getMessage());
                Toast.makeText(requireContext(), "Error al cargar la ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void mostrarDialogoTipoUbicacion() {
        // Primero verificar que hay una ubicación seleccionada en el mapa
        if (selectedMarker == null) {
            Toast.makeText(requireContext(),
                    "Primero selecciona una ubicación en el mapa",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Seleccionar tipo de ubicación");

        String[] tipos = {"Casa", "Oficina", "Otra"};
        builder.setItems(tipos, (dialog, which) -> {
            String tipoSeleccionado = tipos[which];
            if (tipoSeleccionado.equals("Otra")) {
                mostrarDialogoNombreUbicacion(tipoSeleccionado, selectedMarker.getPosition());
            } else {
                mostrarDialogoGuardarUbicacion(tipoSeleccionado, selectedMarker.getPosition(), null);
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoNombreUbicacion(String tipo, LatLng posicion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nombre de la ubicación");

        // Crear EditText
        TextInputEditText editText = new TextInputEditText(requireContext());
        editText.setHint("Ej: Casa de mi abuela, Trabajo, etc.");
        editText.setPadding(50, 20, 50, 20);

        builder.setView(editText);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombre = editText.getText() != null ? editText.getText().toString().trim() : "";
            if (TextUtils.isEmpty(nombre)) {
                Toast.makeText(requireContext(), "Debes ingresar un nombre", Toast.LENGTH_SHORT).show();
                return;
            }
            mostrarDialogoGuardarUbicacion(nombre, posicion, tipo);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoGuardarUbicacion(String nombreTipo, LatLng posicion, String tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Guardar ubicación");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_guardar_ubicacion, null);
        TextInputEditText etDireccion = dialogView.findViewById(R.id.et_direccion);
        TextInputEditText etDescripcion = dialogView.findViewById(R.id.et_descripcion);
        TextInputEditText etDepto = dialogView.findViewById(R.id.et_depto);

        builder.setView(dialogView);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String direccion = etDireccion.getText() != null ? etDireccion.getText().toString().trim() : "";
            String descripcion = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
            String depto = etDepto.getText() != null ? etDepto.getText().toString().trim() : "";

            if (TextUtils.isEmpty(direccion)) {
                Toast.makeText(requireContext(), "La dirección es obligatoria", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determinar si es tipo "Otra" o tipo específico
            String tipoFinal = tipo != null ? tipo : nombreTipo;
            String nombreLugar = tipo != null ? nombreTipo : tipoFinal;

            // Crear nueva ubicación
            Ubicacion nuevaUbicacion = new Ubicacion(
                    nombreLugar,
                    direccion,
                    tipoFinal,
                    depto,
                    descripcion,
                    false
            );

            // Guardar coordenadas
            String coordenadasMarcador = posicion.latitude + "," + posicion.longitude;
            nuevaUbicacion.setCoordenadas(coordenadasMarcador);

            // Guardar en la base de datos
            ubicacionViewModel.insertar(nuevaUbicacion);
            Toast.makeText(requireContext(), "Ubicación guardada correctamente", Toast.LENGTH_SHORT).show();

            // Marcar para seleccionar automáticamente cuando se recargue la lista
            coordenadasUltimaGuardada = coordenadasMarcador;
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}