package com.example.sivareats.ui.profile;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.User;
import com.example.sivareats.data.UserDao;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etNombre, etCorreo, etAlias, etTelefono;
    private ImageView imgProfile;
    private Button btnGuardar, btnCambiarFoto;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private UserDao userDao;
    private ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private User currentUser;

    private String userEmail;
    private Cloudinary cloudinary;
    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    private static final int REQUEST_CODE_PERMISSION = 101;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_editprofile);

        // --- Inicialización de vistas ---
        etNombre = findViewById(R.id.et_name);
        etCorreo = findViewById(R.id.et_email);
        etAlias = findViewById(R.id.et_alias);
        etTelefono = findViewById(R.id.et_phone);
        imgProfile = findViewById(R.id.imgProfile);
        btnGuardar = findViewById(R.id.btn_save);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        toolbar = findViewById(R.id.toolbar);

        // --- Configurar Toolbar ---
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // ← Botón atrás
            getSupportActionBar().setTitle("Editar Perfil");
        }

        // --- Inicializar Firestore y Room ---
        db = FirebaseFirestore.getInstance();
        AppDatabase roomDb = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "sivareats_db"
        ).fallbackToDestructiveMigration().build();
        userDao = roomDb.userDao();

        // --- Obtener correo del Intent ---
        userEmail = getIntent().getStringExtra("user_email");

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "No se recibió correo del usuario.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- Inicializar Cloudinary ---
        initCloudinary();

        // --- Cargar usuario ---
        loadUserFromFirestore(userEmail);

        // --- Guardar cambios ---
        btnGuardar.setOnClickListener(v -> saveChanges());

        // --- Cambiar foto ---
        btnCambiarFoto.setOnClickListener(v -> requestImagePermission());
    }

    // === Inicializar Cloudinary ===
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
            Log.d("EditProfile", "Cloudinary inicializado correctamente con cloud_name: " + cloudName);
        } catch (Exception e) {
            Log.e("EditProfile", "Error al inicializar Cloudinary", e);
            e.printStackTrace();
            cloudinary = null;
        }
    }

    // === Solicitar permiso para leer imágenes ===
    private void requestImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 
                    REQUEST_CODE_PERMISSION);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                    REQUEST_CODE_PERMISSION);
            } else {
                openImagePicker();
            }
        }
    }

    // === Abrir selector de imágenes ===
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    // === Manejar resultado de permisos ===
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Se necesita permiso para seleccionar imágenes", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // === Manejar resultado de selección de imagen ===
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // Mostrar imagen seleccionada
                imgProfile.setImageURI(selectedImageUri);
                // Subir imagen a Cloudinary
                uploadImageToCloudinary(selectedImageUri);
            }
        }
    }

    // === Subir imagen a Cloudinary ===
    private void uploadImageToCloudinary(Uri imageUri) {
        // Verificar que Cloudinary esté inicializado
        if (cloudinary == null) {
            Toast.makeText(this, "Error: Cloudinary no está configurado. Por favor configura tus credenciales en initCloudinary().", Toast.LENGTH_LONG).show();
            Log.e("EditProfile", "Cloudinary es null - verifica initCloudinary()");
            btnCambiarFoto.setEnabled(true);
            btnCambiarFoto.setText("Cambiar imagen");
            return;
        }
        
        // Verificar que las credenciales no sean las de ejemplo
        // Esto es una verificación básica - el usuario debe configurar las credenciales reales
        
        try {
            // Mostrar indicador de carga
            btnCambiarFoto.setEnabled(false);
            btnCambiarFoto.setText("Subiendo...");
            
            // Convertir URI a Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            
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
            File tempFile = null;
            try {
                // Crear archivo temporal
                tempFile = File.createTempFile("profile_image_", ".jpg", getCacheDir());
                FileOutputStream fos = new FileOutputStream(tempFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.flush();
                fos.close();
                
                Log.d("EditProfile", "Archivo temporal creado: " + tempFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e("EditProfile", "Error al crear archivo temporal", e);
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                btnCambiarFoto.setEnabled(true);
                btnCambiarFoto.setText("Cambiar imagen");
                return;
            }
            
            // Crear copias finales para usar en lambda
            final Cloudinary finalCloudinary = cloudinary;
            final String finalUserEmail = userEmail;
            final File finalTempFile = tempFile;
            
            Log.d("EditProfile", "Tamaño de archivo: " + finalTempFile.length() + " bytes");
            
            // Subir en un hilo separado
            ioExecutor.execute(() -> {
                try {
                    // Crear public_id único
                    final String publicId = "profile_" + finalUserEmail.replace("@", "_").replace(".", "_");
                    
                    Log.d("EditProfile", "Iniciando subida a Cloudinary con public_id: " + publicId);
                    Log.d("EditProfile", "Cloudinary config: " + (finalCloudinary != null ? "OK" : "NULL"));
                    
                    // Verificar credenciales antes de subir
                    if (finalCloudinary == null) {
                        throw new Exception("Cloudinary no está inicializado");
                    }
                    
                    // Subir archivo a Cloudinary (método más confiable)
                    Map<String, Object> uploadResult = finalCloudinary.uploader().upload(finalTempFile, 
                        ObjectUtils.asMap(
                            "folder", "profile_images",
                            "public_id", publicId,
                            "overwrite", true,
                            "resource_type", "image"
                        ));
                    
                    Log.d("EditProfile", "Respuesta de Cloudinary: " + uploadResult.toString());
                    
                    // Eliminar archivo temporal después de subir
                    if (finalTempFile.exists()) {
                        finalTempFile.delete();
                    }
                    
                    // Obtener URL de la imagen
                    String imageUrl = (String) uploadResult.get("secure_url");
                    if (imageUrl == null) {
                        imageUrl = (String) uploadResult.get("url");
                    }
                    
                    // Crear copia final para usar en lambda
                    final String finalImageUrl = imageUrl;
                    final User finalCurrentUser = currentUser;
                    final EditProfileActivity activity = EditProfileActivity.this;
                    
                    Log.d("EditProfile", "URL de imagen obtenida: " + finalImageUrl);
                    
                    // Actualizar en el hilo principal
                    runOnUiThread(() -> {
                        if (finalCurrentUser != null && finalImageUrl != null && !finalImageUrl.isEmpty()) {
                            finalCurrentUser.setProfileImageUrl(finalImageUrl);
                            loadImage(finalImageUrl);
                            
                            // Guardar en base de datos local
                            ioExecutor.execute(() -> userDao.update(finalCurrentUser));
                            
                            // Actualizar en Firestore si hay conexión
                            if (isConnected()) {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("profile_image_url", finalImageUrl);
                                db.collection("users").document(finalCurrentUser.getEmail())
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> 
                                            Log.d("EditProfile", "Imagen actualizada en Firestore"))
                                        .addOnFailureListener(e -> 
                                            Log.e("EditProfile", "Error al actualizar imagen en Firestore", e));
                            }
                            
                            Toast.makeText(activity, "Imagen actualizada correctamente", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("EditProfile", "URL de imagen es null o vacía");
                            Toast.makeText(activity, "Error: No se pudo obtener la URL de la imagen", Toast.LENGTH_SHORT).show();
                        }
                        btnCambiarFoto.setEnabled(true);
                        btnCambiarFoto.setText("Cambiar imagen");
                    });
                } catch (Exception e) {
                    Log.e("EditProfile", "Error al subir imagen a Cloudinary", e);
                    e.printStackTrace();
                    
                    // Eliminar archivo temporal en caso de error
                    if (finalTempFile != null && finalTempFile.exists()) {
                        finalTempFile.delete();
                    }
                    
                    final Exception finalException = e;
                    final EditProfileActivity activity = EditProfileActivity.this;
                    runOnUiThread(() -> {
                        String errorMsg = "Error al subir la imagen";
                        if (finalException.getMessage() != null) {
                            String msg = finalException.getMessage();
                            // Mostrar mensaje más amigable
                            if (msg.contains("401") || msg.contains("Unauthorized")) {
                                errorMsg = "Credenciales de Cloudinary incorrectas. Verifica tu configuración.";
                            } else if (msg.contains("400") || msg.contains("Bad Request")) {
                                errorMsg = "Error en la solicitud. Verifica el formato de la imagen.";
                            } else {
                                errorMsg += ": " + msg;
                            }
                        }
                        Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();
                        btnCambiarFoto.setEnabled(true);
                        btnCambiarFoto.setText("Cambiar imagen");
                    });
                }
            });
        } catch (IOException e) {
            Log.e("EditProfile", "Error al leer imagen", e);
            Toast.makeText(this, "Error al leer la imagen", Toast.LENGTH_SHORT).show();
            btnCambiarFoto.setEnabled(true);
            btnCambiarFoto.setText("Cambiar imagen");
        }
    }

    // === Cargar usuario desde Firestore ===
    private void loadUserFromFirestore(String email) {
        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        populateUserFromDocument(document);
                    } else {
                        loadUserFromLocal(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EditProfile", "Error al obtener usuario Firestore: ", e);
                    loadUserFromLocal(email);
                });
    }

    // === Cargar usuario desde Room si falla Firestore ===
    private void loadUserFromLocal(String email) {
        ioExecutor.execute(() -> {
            currentUser = userDao.findUserByEmail(email);
            runOnUiThread(() -> {
                if (currentUser != null) {
                    etNombre.setText(currentUser.getName());
                    etCorreo.setText(currentUser.getEmail());
                    etAlias.setText(currentUser.getAlias());
                    etTelefono.setText(currentUser.getTelefono());
                    loadImage(currentUser.getProfileImageUrl());
                } else {
                    Toast.makeText(this, "Usuario no encontrado en local.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // === Llenar campos desde Firestore ===
    private void populateUserFromDocument(DocumentSnapshot doc) {
        currentUser = new User();
        currentUser.setName(doc.getString("name"));
        currentUser.setEmail(doc.getString("email"));
        currentUser.setAlias(doc.getString("alias"));
        currentUser.setTelefono(doc.getString("telefono"));
        currentUser.setProfileImageUrl(doc.getString("profile_image_url"));

        etNombre.setText(currentUser.getName());
        etCorreo.setText(currentUser.getEmail());
        etAlias.setText(currentUser.getAlias());
        etTelefono.setText(currentUser.getTelefono());

        loadImage(currentUser.getProfileImageUrl());

        ioExecutor.execute(() -> {
            if (userDao.findUserByEmail(currentUser.getEmail()) == null)
                userDao.insert(currentUser);
            else
                userDao.update(currentUser);
        });
    }

    private void loadImage(String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    // === Guardar cambios ===
    private void saveChanges() {
        if (currentUser == null) {
            Toast.makeText(this, "Error: usuario no cargado.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.setName(etNombre.getText().toString().trim());
        currentUser.setAlias(etAlias.getText().toString().trim());
        currentUser.setTelefono(etTelefono.getText().toString().trim());

        if (isConnected()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", currentUser.getName());
            updates.put("alias", currentUser.getAlias());
            updates.put("telefono", currentUser.getTelefono());
            updates.put("profile_image_url", currentUser.getProfileImageUrl());

            db.collection("users").document(currentUser.getEmail())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Perfil actualizado.", Toast.LENGTH_SHORT).show();
                        ioExecutor.execute(() -> userDao.update(currentUser));
                        finish(); // ✅ Regresar al ProfileFragment
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al actualizar Firestore. Guardado local.", Toast.LENGTH_SHORT).show();
                        ioExecutor.execute(() -> userDao.update(currentUser));
                        finish(); // ✅ También regresa aunque falle Firestore
                    });
        } else {
            Toast.makeText(this, "Sin conexión. Guardado localmente.", Toast.LENGTH_SHORT).show();
            ioExecutor.execute(() -> userDao.update(currentUser));
            finish(); // ✅ Regresar al fragmento
        }
    }

    // === Verificar conexión ===
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }

    // === Botón de retroceso del Toolbar ===
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // ✅ Regresa al fragmento
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
