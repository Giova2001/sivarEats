package com.example.sivareats.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sivareats.R;
import com.example.sivareats.ui.restaurant.AgregarPlatilloActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EditPlatilloFragment extends Fragment {

    private FirebaseFirestore db;
    private String userEmail;
    private String restaurantName;
    private String restaurantImageUrl;
    
    private ImageView imgRestaurant;
    private TextView tvRestaurantName;
    private RecyclerView recyclerViewPlatillos;
    private View btnAgregarPlatillo;
    
    private PlatilloAdapter adapter;
    private List<Platillo> platillosList = new ArrayList<>();

    private static final int REQUEST_AGREGAR_PLATILLO = 1001;

    public EditPlatilloFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_platillo, container, false);

        db = FirebaseFirestore.getInstance();
        
        // Obtener email del usuario
        SharedPreferences prefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("CURRENT_USER_EMAIL", null);

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(getContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            return view;
        }

        initViews(view);
        loadRestaurantData();
        
        btnAgregarPlatillo.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AgregarPlatilloActivity.class);
            startActivityForResult(intent, REQUEST_AGREGAR_PLATILLO);
        });

        return view;
    }

    private void initViews(View view) {
        imgRestaurant = view.findViewById(R.id.img_restaurant);
        tvRestaurantName = view.findViewById(R.id.tv_restaurant_name);
        recyclerViewPlatillos = view.findViewById(R.id.recycler_platillos);
        btnAgregarPlatillo = view.findViewById(R.id.btn_agregar_platillo);
        
        adapter = new PlatilloAdapter(platillosList);
        recyclerViewPlatillos.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPlatillos.setAdapter(adapter);
    }

    private void loadRestaurantData() {
        db.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded() && documentSnapshot.exists()) {
                        restaurantName = documentSnapshot.getString("name");
                        restaurantImageUrl = documentSnapshot.getString("profile_image_url");
                        
                        if (restaurantName != null && tvRestaurantName != null) {
                            tvRestaurantName.setText(restaurantName);
                        }
                        
                        if (restaurantImageUrl != null && !restaurantImageUrl.isEmpty() && imgRestaurant != null) {
                            Glide.with(requireContext())
                                    .load(restaurantImageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .into(imgRestaurant);
                        } else if (imgRestaurant != null) {
                            imgRestaurant.setImageResource(R.drawable.ic_profile);
                        }
                        
                        // Cargar platillos después de obtener el nombre del restaurante
                        if (restaurantName != null && !restaurantName.isEmpty()) {
                            loadPlatillosFromFirebase();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EditPlatilloFragment", "Error al cargar datos del restaurante: " + e.getMessage());
                });
    }

    private void loadPlatillos() {
        if (restaurantName == null || restaurantName.isEmpty()) {
            // Si no tenemos el nombre del restaurante, cargarlo primero
            db.collection("users").document(userEmail)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (isAdded() && documentSnapshot.exists()) {
                            restaurantName = documentSnapshot.getString("name");
                            if (restaurantName != null && !restaurantName.isEmpty()) {
                                loadPlatillosFromFirebase();
                            }
                        }
                    });
            return;
        }
        loadPlatillosFromFirebase();
    }

    private void loadPlatillosFromFirebase() {
        // Cargar platillos directamente del documento del restaurante
        db.collection("restaurantes").document(restaurantName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded() && documentSnapshot.exists()) {
                        platillosList.clear();
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                // Verificar si es un platillo (tiene la estructura esperada)
                                Object value = entry.getValue();
                                if (value instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> platilloData = (Map<String, Object>) value;
                                    // Verificar que tenga los campos de un platillo
                                    if (platilloData.containsKey("nombrePlatillo")) {
                                        String platilloId = entry.getKey();
                                        String nombrePlatillo = (String) platilloData.get("nombrePlatillo");
                                        String descripcion = (String) platilloData.get("Descripcion");
                                        Double precio = (Double) platilloData.get("precio");
                                        String categoria = (String) platilloData.get("categoria");
                                        String imagenUrl = (String) platilloData.get("URL_imagen_platillo");
                                        Boolean visible = (Boolean) platilloData.get("visible");
                                        
                                        Platillo platillo = new Platillo(
                                                platilloId,
                                                nombrePlatillo != null ? nombrePlatillo : "",
                                                descripcion != null ? descripcion : "",
                                                precio != null ? precio : 0.0,
                                                categoria != null ? categoria : "",
                                                imagenUrl != null ? imagenUrl : "",
                                                visible != null ? visible : true
                                        );
                                        platillosList.add(platillo);
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EditPlatilloFragment", "Error al cargar platillos: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al cargar platillos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AGREGAR_PLATILLO && resultCode == android.app.Activity.RESULT_OK) {
            // Recargar platillos después de agregar/editar
            loadPlatillos();
        }
    }

    // Clase para representar un platillo
    private static class Platillo {
        String id;
        String nombre;
        String descripcion;
        double precio;
        String categoria;
        String imagenUrl;
        boolean visible;

        Platillo(String id, String nombre, String descripcion, double precio, String categoria, String imagenUrl, boolean visible) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.precio = precio;
            this.categoria = categoria;
            this.imagenUrl = imagenUrl;
            this.visible = visible;
        }
    }

    // Adapter para RecyclerView
    private class PlatilloAdapter extends RecyclerView.Adapter<PlatilloAdapter.PlatilloViewHolder> {
        private List<Platillo> platillos;

        PlatilloAdapter(List<Platillo> platillos) {
            this.platillos = platillos;
        }

        @NonNull
        @Override
        public PlatilloViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_platillo_restaurante, parent, false);
            return new PlatilloViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlatilloViewHolder holder, int position) {
            Platillo platillo = platillos.get(position);
            holder.bind(platillo);
        }

        @Override
        public int getItemCount() {
            return platillos.size();
        }

        class PlatilloViewHolder extends RecyclerView.ViewHolder {
            private ImageView imgPlatillo;
            private TextView tvNombre;
            private TextView tvDetalles;
            private TextView tvDescripcion;
            private ImageView btnEdit;

            PlatilloViewHolder(View itemView) {
                super(itemView);
                imgPlatillo = itemView.findViewById(R.id.img_platillo);
                tvNombre = itemView.findViewById(R.id.tv_nombre);
                tvDetalles = itemView.findViewById(R.id.tv_detalles);
                tvDescripcion = itemView.findViewById(R.id.tv_descripcion);
                btnEdit = itemView.findViewById(R.id.btn_edit);
            }

            void bind(Platillo platillo) {
                tvNombre.setText(platillo.nombre);
                String detalles = platillo.categoria + " • $" + String.format("%.2f", platillo.precio);
                tvDetalles.setText(detalles);
                tvDescripcion.setText(platillo.descripcion);

                if (platillo.imagenUrl != null && !platillo.imagenUrl.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(platillo.imagenUrl)
                            .placeholder(R.drawable.icono_pizza)
                            .error(R.drawable.icono_pizza)
                            .into(imgPlatillo);
                } else {
                    imgPlatillo.setImageResource(R.drawable.icono_pizza);
                }

                btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), AgregarPlatilloActivity.class);
                    intent.putExtra("platillo_id", platillo.id);
                    startActivityForResult(intent, REQUEST_AGREGAR_PLATILLO);
                });

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), AgregarPlatilloActivity.class);
                    intent.putExtra("platillo_id", platillo.id);
                    startActivityForResult(intent, REQUEST_AGREGAR_PLATILLO);
                });
            }
        }
    }
}
