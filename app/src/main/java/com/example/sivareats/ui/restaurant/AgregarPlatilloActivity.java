package com.example.sivareats.ui.restaurant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.sivareats.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgregarPlatilloActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private Cloudinary cloudinary;
    private ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private String userEmail;
    private String restaurantName; // Nombre del restaurante (usado como ID del documento)
    private String platilloId; // Si viene del intent, significa que estamos editando
    private boolean isEditing = false;
    private boolean isVisible = true;
    private Uri imagenUri = null;

    private ScrollView scrollView;
    private ImageView imgPlatilloPlaceholder;
    private ImageView imgPlatilloSelected;
    private TextInputEditText etNombre;
    private TextInputEditText etPrecio;
    private MaterialAutoCompleteTextView acCategoria;
    private TextInputEditText etDescripcion;
    private SwitchMaterial switchVisible;
    private MaterialButton btnGuardar;

    private static final int PICK_IMAGE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_platillo);

        db = FirebaseFirestore.getInstance();
        
        // Inicializar Cloudinary
        initCloudinary();

        // Obtener email del usuario
        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("CURRENT_USER_EMAIL", null);

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Verificar si estamos editando un platillo existente
        platilloId = getIntent().getStringExtra("platillo_id");
        if (platilloId != null && !platilloId.isEmpty()) {
            isEditing = true;
        }

        initViews();
        setupCategoriaDropdown();
        setupVisibilitySwitch();
        setupKeyboardScroll();
        
        // Cargar nombre del restaurante
        loadRestaurantName();

        if (isEditing) {
            loadPlatilloData();
        }

        imgPlatilloPlaceholder.setOnClickListener(v -> seleccionarImagen());
        imgPlatilloSelected.setOnClickListener(v -> seleccionarImagen());
        btnGuardar.setOnClickListener(v -> guardarPlatillo());
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditing ? "Editar Platillo" : "Agregar Platillo");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        scrollView = findViewById(R.id.scrollView);
        imgPlatilloPlaceholder = findViewById(R.id.img_platillo_placeholder);
        imgPlatilloSelected = findViewById(R.id.img_platillo_selected);
        etNombre = findViewById(R.id.et_nombre);
        etPrecio = findViewById(R.id.et_precio);
        acCategoria = findViewById(R.id.ac_categoria);
        etDescripcion = findViewById(R.id.et_descripcion);
        switchVisible = findViewById(R.id.switch_visible);
        btnGuardar = findViewById(R.id.btn_guardar);
    }
    
    private void setupKeyboardScroll() {
        if (scrollView == null) return;
        
        // Listener para detectar cuando aparece/desaparece el teclado
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                scrollView.getWindowVisibleDisplayFrame(r);
                int screenHeight = scrollView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                
                // Si el teclado está visible (keypadHeight > screenHeight * 0.15)
                if (keypadHeight > screenHeight * 0.15) {
                    // El teclado está visible
                } else {
                    // El teclado está oculto
                }
            }
        });
        
        // Agregar listeners de foco a los campos de texto
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus) {
                // Esperar un momento para que el teclado aparezca
                scrollView.postDelayed(() -> scrollToView(v), 300);
            }
        };
        
        if (etNombre != null) {
            etNombre.setOnFocusChangeListener(focusListener);
        }
        if (etPrecio != null) {
            etPrecio.setOnFocusChangeListener(focusListener);
        }
        if (acCategoria != null) {
            acCategoria.setOnFocusChangeListener(focusListener);
        }
        if (etDescripcion != null) {
            etDescripcion.setOnFocusChangeListener(focusListener);
        }
    }
    
    private void scrollToView(View view) {
        if (scrollView == null || view == null) return;
        
        Rect scrollBounds = new Rect();
        scrollView.getHitRect(scrollBounds);
        
        int[] location = new int[2];
        view.getLocationInWindow(location);
        
        int viewTop = location[1];
        int viewBottom = viewTop + view.getHeight();
        
        // Obtener la posición del ScrollView en la ventana
        int[] scrollLocation = new int[2];
        scrollView.getLocationInWindow(scrollLocation);
        int scrollTop = scrollLocation[1];
        int scrollBottom = scrollTop + scrollView.getHeight();
        
        // Calcular el desplazamiento necesario
        int scrollY = 0;
        if (viewTop < scrollTop) {
            // El campo está arriba del ScrollView visible
            scrollY = viewTop - scrollTop - 100; // 100dp de margen superior
        } else if (viewBottom > scrollBottom) {
            // El campo está abajo del ScrollView visible
            scrollY = viewBottom - scrollBottom + 100; // 100dp de margen inferior
        }
        
        if (scrollY != 0) {
            scrollView.smoothScrollBy(0, scrollY);
        }
    }


    private void setupCategoriaDropdown() {
        String[] categorias = {
            "China",
            "Pizzas",
            "Hamburguesas",
            "Pollo",
            "Mariscos",
            "Postres",
            "Bebidas",
            "Otro"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, categorias);
        acCategoria.setAdapter(adapter);
    }

    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            // Credenciales de Cloudinary
            String cloudName = "dpjadtypv";
            String apiKey = "863696682844582";
            String apiSecret = "4eFE6ozxIG26OXHSmzcsKBYvma0";
            
            config.put("cloud_name", cloudName);
            config.put("api_key", apiKey);
            config.put("api_secret", apiSecret);
            
            cloudinary = new Cloudinary(config);
            Log.d("AgregarPlatillo", "Cloudinary inicializado correctamente");
        } catch (Exception e) {
            Log.e("AgregarPlatillo", "Error al inicializar Cloudinary", e);
            cloudinary = null;
        }
    }

    private void loadRestaurantName() {
        db.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        restaurantName = documentSnapshot.getString("name");
                        if (restaurantName == null || restaurantName.isEmpty()) {
                            Toast.makeText(this, "Error: Nombre de restaurante no encontrado", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AgregarPlatillo", "Error al cargar nombre del restaurante: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar datos del restaurante", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupVisibilitySwitch() {
        switchVisible.setChecked(isVisible);
        switchVisible.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isVisible = isChecked;
        });
    }

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imagenUri = data.getData();
            if (imagenUri != null) {
                imgPlatilloPlaceholder.setVisibility(View.GONE);
                imgPlatilloSelected.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(imagenUri)
                        .into(imgPlatilloSelected);
            }
        }
    }

    private void loadPlatilloData() {
        if (restaurantName == null || restaurantName.isEmpty()) {
            // Esperar a que se cargue el nombre del restaurante
            db.collection("users").document(userEmail)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            restaurantName = documentSnapshot.getString("name");
                            if (restaurantName != null && !restaurantName.isEmpty()) {
                                loadPlatilloDataFromFirebase();
                            }
                        }
                    });
            return;
        }
        loadPlatilloDataFromFirebase();
    }

    private void loadPlatilloDataFromFirebase() {
        // Cargar desde el documento del restaurante directamente
        db.collection("restaurantes").document(restaurantName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && platilloId != null) {
                        // Los platillos están guardados como campos del documento
                        // Cada platillo es un mapa con el ID como clave
                        Map<String, Object> platilloData = (Map<String, Object>) documentSnapshot.get(platilloId);
                        if (platilloData != null) {
                            String nombre = (String) platilloData.get("nombrePlatillo");
                            Double precio = (Double) platilloData.get("precio");
                            String categoria = (String) platilloData.get("categoria");
                            String descripcion = (String) platilloData.get("Descripcion");
                            Boolean visible = (Boolean) platilloData.get("visible");
                            String imagenUrl = (String) platilloData.get("URL_imagen_platillo");
                            
                            if (nombre != null) {
                                etNombre.setText(nombre);
                            }
                            if (precio != null) {
                                etPrecio.setText(String.valueOf(precio));
                            }
                            if (categoria != null) {
                                acCategoria.setText(categoria, false);
                            }
                            if (descripcion != null) {
                                etDescripcion.setText(descripcion);
                            }
                            if (visible != null) {
                                isVisible = visible;
                                switchVisible.setChecked(isVisible);
                            }
                            if (imagenUrl != null && !imagenUrl.isEmpty()) {
                                imgPlatilloPlaceholder.setVisibility(View.GONE);
                                imgPlatilloSelected.setVisibility(View.VISIBLE);
                                Glide.with(this)
                                        .load(imagenUrl)
                                        .into(imgPlatilloSelected);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AgregarPlatillo", "Error al cargar platillo: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar platillo", Toast.LENGTH_SHORT).show();
                });
    }

    private void guardarPlatillo() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String precioStr = etPrecio.getText() != null ? etPrecio.getText().toString().trim() : "";
        String categoria = acCategoria.getText() != null ? acCategoria.getText().toString().trim() : "";
        String descripcion = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";

        // Validaciones
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Ingresa el nombre del platillo");
            etNombre.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(precioStr)) {
            etPrecio.setError("Ingresa el precio");
            etPrecio.requestFocus();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
            if (precio <= 0) {
                etPrecio.setError("El precio debe ser mayor a 0");
                etPrecio.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etPrecio.setError("Precio inválido");
            etPrecio.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(categoria)) {
            acCategoria.setError("Selecciona una categoría");
            acCategoria.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(descripcion)) {
            etDescripcion.setError("Ingresa una descripción");
            etDescripcion.requestFocus();
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        // Si hay una imagen nueva, subirla primero
        if (imagenUri != null) {
            subirImagenYGuardar(nombre, precio, categoria, descripcion);
        } else {
            // Si estamos editando y no hay nueva imagen, usar la URL existente
            guardarPlatilloEnFirebase(nombre, precio, categoria, descripcion, null);
        }
    }

    private void subirImagenYGuardar(String nombre, double precio, String categoria, String descripcion) {
        if (cloudinary == null) {
            Toast.makeText(this, "Error: Cloudinary no está configurado", Toast.LENGTH_LONG).show();
            btnGuardar.setEnabled(true);
            btnGuardar.setText("Guardar");
            return;
        }

        try {
            // Convertir URI a Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imagenUri);
            
            // Redimensionar imagen si es muy grande (máximo 1024px)
            int maxSize = 1024;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxSize || height > maxSize) {
                float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                int newWidth = Math.round(width * scale);
                int newHeight = Math.round(height * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }
            
            // Crear archivo temporal para la imagen
            File tempFile = File.createTempFile("platillo_image_", ".jpg", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            
            final File finalTempFile = tempFile;
            final Cloudinary finalCloudinary = cloudinary;
            
            // Subir en un hilo separado
            ioExecutor.execute(() -> {
                try {
                    // Crear public_id único
                    String publicId = "platillo_" + restaurantName.replace(" ", "_").replace("@", "_").replace(".", "_") + "_" + UUID.randomUUID().toString();
                    
                    // Subir archivo a Cloudinary
                    Map<String, Object> uploadResult = finalCloudinary.uploader().upload(finalTempFile, 
                        ObjectUtils.asMap(
                            "folder", "platillos",
                            "public_id", publicId,
                            "overwrite", true,
                            "resource_type", "image"
                        ));
                    
                    // Eliminar archivo temporal después de subir
                    if (finalTempFile.exists()) {
                        finalTempFile.delete();
                    }
                    
                    // Obtener URL de la imagen
                    String imageUrl = (String) uploadResult.get("secure_url");
                    if (imageUrl == null) {
                        imageUrl = (String) uploadResult.get("url");
                    }
                    
                    final String finalImageUrl = imageUrl;
                    
                    // Actualizar en el hilo principal
                    runOnUiThread(() -> {
                        if (finalImageUrl != null && !finalImageUrl.isEmpty()) {
                            guardarPlatilloEnFirebase(nombre, precio, categoria, descripcion, finalImageUrl);
                        } else {
                            Toast.makeText(this, "Error al obtener URL de la imagen", Toast.LENGTH_SHORT).show();
                            btnGuardar.setEnabled(true);
                            btnGuardar.setText("Guardar");
                        }
                    });
                } catch (Exception e) {
                    Log.e("AgregarPlatillo", "Error al subir imagen a Cloudinary: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("Guardar");
                    });
                }
            });
        } catch (IOException e) {
            Log.e("AgregarPlatillo", "Error al procesar imagen: " + e.getMessage());
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
            btnGuardar.setEnabled(true);
            btnGuardar.setText("Guardar");
        }
    }

    private void guardarPlatilloEnFirebase(String nombre, double precio, String categoria, String descripcion, String imagenUrl) {
        if (restaurantName == null || restaurantName.isEmpty()) {
            Toast.makeText(this, "Error: Nombre de restaurante no disponible", Toast.LENGTH_SHORT).show();
            btnGuardar.setEnabled(true);
            btnGuardar.setText("Guardar");
            return;
        }

        Map<String, Object> platilloData = new HashMap<>();
        platilloData.put("nombrePlatillo", nombre);
        platilloData.put("precio", precio);
        platilloData.put("categoria", categoria);
        platilloData.put("Descripcion", descripcion);
        platilloData.put("visible", isVisible);
        if (imagenUrl != null && !imagenUrl.isEmpty()) {
            platilloData.put("URL_imagen_platillo", imagenUrl);
        }

        // Guardar platillo directamente en el documento del restaurante
        // Usar merge para no sobrescribir otros platillos
        Map<String, Object> updateData = new HashMap<>();
        
        if (isEditing && platilloId != null) {
            // Actualizar platillo existente
            updateData.put(platilloId, platilloData);
        } else {
            // Crear nuevo platillo
            String newPlatilloId = UUID.randomUUID().toString();
            updateData.put(newPlatilloId, platilloData);
        }
        
        db.collection("restaurantes").document(restaurantName)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    String message = isEditing ? "Platillo actualizado correctamente" : "Platillo guardado correctamente";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("AgregarPlatillo", "Error al guardar: " + e.getMessage());
                    String message = isEditing ? "Error al actualizar platillo" : "Error al guardar platillo";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar");
                });
    }
}

