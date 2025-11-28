package com.example.sivareats.ui.restaurant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sivareats.R;
import com.example.sivareats.data.cart.CartDao;
import com.example.sivareats.data.AppDatabase;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestaurantDetailActivity extends AppCompatActivity {

    private static final String TAG = "RestaurantDetailActivity";
    private FirebaseFirestore db;
    private ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    
    private MaterialToolbar toolbar;
    private ImageView imgRestaurante;
    private TextView tvNombreRestaurante;
    private RecyclerView recyclerViewPlatillos;
    
    private String restaurantName;
    private List<PlatilloItem> platillosList;
    private PlatilloAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);
        
        // Obtener nombre del restaurante del Intent
        restaurantName = getIntent().getStringExtra("RESTAURANT_NAME");
        if (restaurantName == null || restaurantName.isEmpty()) {
            Toast.makeText(this, "Error: Restaurante no especificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        db = FirebaseFirestore.getInstance();
        platillosList = new ArrayList<>();
        
        initViews();
        setupToolbar();
        loadRestaurantData();
        loadPlatillos();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        imgRestaurante = findViewById(R.id.img_restaurante);
        tvNombreRestaurante = findViewById(R.id.tv_nombre_restaurante);
        recyclerViewPlatillos = findViewById(R.id.recycler_platillos);
        
        adapter = new PlatilloAdapter(platillosList);
        recyclerViewPlatillos.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPlatillos.setAdapter(adapter);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Restaurante");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    
    private void loadRestaurantData() {
        // Buscar el restaurante en la colección users para obtener su imagen
        db.collection("users")
                .whereEqualTo("name", restaurantName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String imageUrl = doc.getString("profile_image_url");
                        
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .into(imgRestaurante);
                        } else {
                            // Usar imagen por defecto según el restaurante
                            imgRestaurante.setImageResource(getRestaurantImageResource(restaurantName));
                        }
                    } else {
                        // Si no se encuentra en users, usar imagen por defecto
                        imgRestaurante.setImageResource(getRestaurantImageResource(restaurantName));
                    }
                    
                    tvNombreRestaurante.setText(restaurantName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar datos del restaurante: " + e.getMessage());
                    imgRestaurante.setImageResource(getRestaurantImageResource(restaurantName));
                    tvNombreRestaurante.setText(restaurantName);
                });
    }
    
    private int getRestaurantImageResource(String nombreRestaurante) {
        if (nombreRestaurante.contains("Pollo Campero") || nombreRestaurante.contains("Campero")) {
            return R.drawable.campero1;
        } else if (nombreRestaurante.contains("Pizza Hut") || nombreRestaurante.contains("Pizza")) {
            return R.drawable.pizza1;
        } else if (nombreRestaurante.contains("Burger King") || nombreRestaurante.contains("Burger")) {
            return R.drawable.hamburguesa1;
        } else if (nombreRestaurante.contains("China Wok") || nombreRestaurante.contains("China")) {
            return R.drawable.chinawok1;
        }
        return R.drawable.ic_profile; // Por defecto
    }
    
    private void loadPlatillos() {
        db.collection("restaurantes").document(restaurantName)
                .collection("platillos")
                .whereEqualTo("visible", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    platillosList.clear();
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String nombrePlatillo = doc.getString("nombrePlatillo");
                            String descripcion = doc.getString("Descripcion");
                            String categoria = doc.getString("categoria");
                            String imagenUrl = doc.getString("URL_imagen_platillo");
                            Double precio = doc.getDouble("precio");
                            
                            if (nombrePlatillo != null && precio != null) {
                                PlatilloItem platillo = new PlatilloItem(
                                        doc.getId(),
                                        nombrePlatillo,
                                        descripcion != null ? descripcion : "",
                                        categoria != null ? categoria : "restaurantes",
                                        imagenUrl != null ? imagenUrl : "",
                                        precio,
                                        4.5, // Rating por defecto
                                        0 // Compras por defecto
                                );
                                platillosList.add(platillo);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al procesar platillo: " + e.getMessage());
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar platillos: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar platillos", Toast.LENGTH_SHORT).show();
                });
    }
    
    // Clase interna para representar un platillo
    public static class PlatilloItem {
        private String id;
        private String nombre;
        private String descripcion;
        private String categoria;
        private String imagenUrl;
        private double precio;
        private double rating;
        private int compras;
        
        public PlatilloItem(String id, String nombre, String descripcion, String categoria, 
                           String imagenUrl, double precio, double rating, int compras) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.categoria = categoria;
            this.imagenUrl = imagenUrl;
            this.precio = precio;
            this.rating = rating;
            this.compras = compras;
        }
        
        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public String getDescripcion() { return descripcion; }
        public String getCategoria() { return categoria; }
        public String getImagenUrl() { return imagenUrl; }
        public double getPrecio() { return precio; }
        public double getRating() { return rating; }
        public int getCompras() { return compras; }
    }
    
    // Adapter para la lista de platillos
    private class PlatilloAdapter extends RecyclerView.Adapter<PlatilloAdapter.ViewHolder> {
        private List<PlatilloItem> platillos;
        private CartDao cartDao;
        
        public PlatilloAdapter(List<PlatilloItem> platillos) {
            this.platillos = platillos;
            this.cartDao = AppDatabase.getInstance(RestaurantDetailActivity.this).cartDao();
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_platillo_restaurante_detail, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PlatilloItem platillo = platillos.get(position);
            
            // Cargar imagen
            if (platillo.getImagenUrl() != null && !platillo.getImagenUrl().isEmpty()) {
                Glide.with(RestaurantDetailActivity.this)
                        .load(platillo.getImagenUrl())
                        .placeholder(R.drawable.campero1)
                        .error(R.drawable.campero1)
                        .into(holder.imgPlatillo);
            } else {
                holder.imgPlatillo.setImageResource(R.drawable.campero1);
            }
            
            holder.tvNombre.setText(platillo.getNombre());
            holder.tvDescripcion.setText(platillo.getDescripcion());
            
            // Formatear detalles: Categoría • Precio • Compras
            String categoriaTexto = getCategoriaTexto(platillo.getCategoria());
            String comprasTexto = formatCompras(platillo.getCompras());
            holder.tvDetalles.setText(String.format("%s • $%.2f • %s", 
                    categoriaTexto, platillo.getPrecio(), comprasTexto));
            
            // Rating (estrellas)
            setRatingStars(holder, platillo.getRating());
            
            // Actualizar estrellas visuales
            updateStarsVisual(holder, platillo.getRating());
            
            // Botón favorito (por ahora solo visual)
            holder.btnFavorito.setOnClickListener(v -> {
                // TODO: Implementar lógica de favoritos
                Toast.makeText(RestaurantDetailActivity.this, "Agregado a favoritos", Toast.LENGTH_SHORT).show();
            });
            
            // Botón agregar al carrito
            holder.btnAgregar.setOnClickListener(v -> {
                com.example.sivareats.data.cart.CartItem item = 
                        new com.example.sivareats.data.cart.CartItem(
                                platillo.getNombre(),
                                platillo.getDescripcion(),
                                0, // resource ID (no usado si hay URL)
                                platillo.getPrecio(),
                                1
                        );
                
                ioExecutor.execute(() -> cartDao.insert(item));
                Toast.makeText(RestaurantDetailActivity.this, 
                        "Agregado al carrito: " + platillo.getNombre(), 
                        Toast.LENGTH_SHORT).show();
            });
        }
        
        @Override
        public int getItemCount() {
            return platillos.size();
        }
        
        private String getCategoriaTexto(String categoria) {
            if (categoria == null) return "Restaurante";
            switch (categoria.toLowerCase()) {
                case "china": return "China";
                case "pizza": return "Pizzas";
                case "restaurantes": return "Comida Rápida";
                default: return categoria;
            }
        }
        
        private String formatCompras(int compras) {
            if (compras >= 1000) {
                return String.format("%.1f k", compras / 1000.0) + " Comprado";
            }
            return compras + " Comprado";
        }
        
        private void setRatingStars(ViewHolder holder, double rating) {
            // Mostrar rating como texto
            holder.tvRating.setText(String.format("%.1f", rating));
        }
        
        private void updateStarsVisual(ViewHolder holder, double rating) {
            if (holder.tvStars != null) {
                int fullStars = (int) rating;
                boolean hasHalfStar = (rating - fullStars) >= 0.5;
                int emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);
                
                StringBuilder starsText = new StringBuilder();
                // Estrellas llenas (★)
                for (int i = 0; i < fullStars; i++) {
                    starsText.append("★");
                }
                // Media estrella (si aplica)
                if (hasHalfStar) {
                    starsText.append("★"); // Por simplicidad, usar estrella llena
                }
                // Estrellas vacías (☆)
                for (int i = 0; i < emptyStars; i++) {
                    starsText.append("☆");
                }
                
                holder.tvStars.setText(starsText.toString());
            }
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPlatillo;
            TextView tvNombre;
            TextView tvDetalles;
            TextView tvDescripcion;
            TextView tvRating;
            TextView tvStars;
            ImageView btnFavorito;
            ImageView btnAgregar;
            
            ViewHolder(View itemView) {
                super(itemView);
                imgPlatillo = itemView.findViewById(R.id.img_platillo);
                tvNombre = itemView.findViewById(R.id.tv_nombre);
                tvDetalles = itemView.findViewById(R.id.tv_detalles);
                tvDescripcion = itemView.findViewById(R.id.tv_descripcion);
                tvRating = itemView.findViewById(R.id.tv_rating);
                tvStars = itemView.findViewById(R.id.tv_stars);
                btnFavorito = itemView.findViewById(R.id.btn_favorito);
                btnAgregar = itemView.findViewById(R.id.btn_agregar);
            }
        }
    }
}

