package com.example.sivareats.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {

    // --- UI Views ---
    private Toolbar toolbar;
    private TextInputEditText etName, etAlias, etEmail, etPhone; // Campo de contraseña eliminado
    private Button btnSave;
    private ImageView imgProfile;
    private Button btnCambiarFoto;
    private ScrollView scrollView;
    private LinearLayout mainContainer;

    private ActivityResultLauncher<String> galleryLauncher;

    // --- Firebase & Room ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AppDatabase roomDb;
    private String userEmail;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // --- Cloudinary Config ---
    private static final String CLOUDINARY_CLOUD_NAME = "dpjadtypv";
    private static final String CLOUDINARY_API_KEY = "863696682844582";
    private static final String CLOUDINARY_API_SECRET = "4eFE6ozxIG26OXHSmzcsKBYvma0";
    private static final String CLOUDINARY_FOLDER = "sivarEats";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_editprofile);

        initViews();
        initFirebaseAndRoom();
        initGalleryLauncher();
        initCloudinary();

        loadUserProfile();

        btnSave.setOnClickListener(v -> saveUserProfile());
        btnCambiarFoto.setOnClickListener(v -> openGallery());
        setupKeyboardHandling();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mainContainer = findViewById(R.id.main_container);
        scrollView = findViewById(R.id.scroll_view);

        etName = findViewById(R.id.et_name);
        etAlias = findViewById(R.id.et_alias);
        etEmail = findViewById(R.id.et_email);
        etEmail.setEnabled(false); // El email no debe ser editable
        etPhone = findViewById(R.id.et_phone);
        btnSave = findViewById(R.id.btn_save);
        imgProfile = findViewById(R.id.imgProfile);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
    }

    private void initFirebaseAndRoom() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        roomDb = AppDatabase.getInstance(getApplicationContext());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
            // Guardar en SharedPreferences para uso futuro
            SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
            sessionPrefs.edit().putString("CURRENT_USER_EMAIL", userEmail).apply();
            SharedPreferences loginPrefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
            loginPrefs.edit().putString("email", userEmail).apply();
        } else {
            // Como fallback, intentar obtenerlo de múltiples fuentes
            SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
            userEmail = sessionPrefs.getString("CURRENT_USER_EMAIL", null);
            
            if (userEmail == null) {
                SharedPreferences loginPrefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
                userEmail = loginPrefs.getString("email", null);
            }
            
            // Si aún no se encuentra, intentar obtener desde Room (último usuario registrado)
            if (userEmail == null) {
                ioExecutor.execute(() -> {
                    try {
                        User lastUser = roomDb.userDao().findFirstUser();
                        if (lastUser != null && lastUser.getEmail() != null) {
                            String foundEmail = lastUser.getEmail();
                            runOnUiThread(() -> {
                                userEmail = foundEmail;
                                // Guardar para uso futuro
                                SharedPreferences sessionPrefs2 = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                                sessionPrefs2.edit().putString("CURRENT_USER_EMAIL", userEmail).apply();
                                SharedPreferences loginPrefs2 = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
                                loginPrefs2.edit().putString("email", userEmail).apply();
                                // Continuar con la carga del perfil
                                loadUserProfile();
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error: Usuario no identificado. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error: Usuario no identificado. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                });
                return; // Salir aquí, loadUserProfile se llamará desde el hilo si encuentra usuario
            }
        }

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
            config.put("api_key", CLOUDINARY_API_KEY);
            config.put("api_secret", CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (Exception e) {
            Log.e("EditProfile", "Error al inicializar Cloudinary", e);
        }
    }

    private void initGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Glide.with(this).load(uri).circleCrop().into(imgProfile);
                        uploadImageToCloudinary(uri);
                    }
                }
        );
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void loadUserProfile() {
        if (userEmail == null) return;

        // Cargar desde Firestore como fuente principal de la verdad
        db.collection("users").document(userEmail).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        if(user != null) {
                             populateUi(user);
                             // Sincronizar con Room
                             syncUserToRoom(user);
                        }
                    } else {
                         // Si no existe en Firestore, intentar cargar desde Room
                         loadUserFromRoom();
                    }
                })
                .addOnFailureListener(e -> {
                     Log.e("EditProfile", "Error al cargar de Firestore, cargando de Room", e);
                     loadUserFromRoom();
                });
    }
    
    private void loadUserFromRoom(){
        ioExecutor.execute(() -> {
            User user = roomDb.userDao().findByEmail(userEmail);
            if (user != null) {
                runOnUiThread(() -> populateUi(user));
            }
        });
    }

    private void populateUi(User user) {
        etName.setText(user.getName());
        etAlias.setText(user.getAlias());
        etEmail.setText(user.getEmail());
        etPhone.setText(user.getTelefono());

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imgProfile);
        }
    }
    
     private void syncUserToRoom(User firestoreUser) {
        ioExecutor.execute(() -> {
            // Se necesita la contraseña hasheada que solo está en Room
            User localUser = roomDb.userDao().findByEmail(userEmail);
            String hashedPassword = (localUser != null) ? localUser.getPassword() : "";

            // Crear una instancia de usuario para Room
            User userToSave = new User(firestoreUser.getName(), firestoreUser.getEmail(), hashedPassword);
            userToSave.setAlias(firestoreUser.getAlias());
            userToSave.setTelefono(firestoreUser.getTelefono());
            userToSave.setProfileImageUrl(firestoreUser.getProfileImageUrl());

            // Insertar o reemplazar el usuario en Room
            roomDb.userDao().insert(userToSave);
        });
    }


    private void saveUserProfile() {
        if (userEmail == null) {
            Toast.makeText(this, "No se puede guardar: usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        String newName = etName.getText().toString().trim();
        String newAlias = etAlias.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        
        // El campo de contraseña ya no existe en la UI

        // Crear un mapa SOLAMENTE con los campos a actualizar en Firestore.
        // NUNCA incluir la contraseña.
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("alias", newAlias);
        updates.put("telefono", newPhone);

        btnSave.setEnabled(false);

        // Verificar que el usuario esté autenticado antes de actualizar
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Error: Sesión expirada. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
            btnSave.setEnabled(true);
            return;
        }

        // 1. Guardar en Firestore
        db.collection("users").document(userEmail)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 2. Si Firestore tiene éxito, actualizar Room
                    ioExecutor.execute(() -> {
                        roomDb.userDao().updateProfileDetails(userEmail, newName, newAlias, newPhone);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e.getMessage();
                    // Si el error es de permisos, intentar con set en lugar de update
                    if (errorMsg != null && (errorMsg.contains("permission") || errorMsg.contains("PERMISSION_DENIED"))) {
                        // Intentar crear/actualizar con set
                        Map<String, Object> fullData = new HashMap<>(updates);
                        fullData.put("email", userEmail);
                        db.collection("users").document(userEmail)
                                .set(fullData, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    ioExecutor.execute(() -> {
                                        roomDb.userDao().updateProfileDetails(userEmail, newName, newAlias, newPhone);
                                        runOnUiThread(() -> {
                                            Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                                    });
                                })
                                .addOnFailureListener(e2 -> {
                                    Toast.makeText(this, "Error al actualizar el perfil: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                    btnSave.setEnabled(true);
                                });
                    } else {
                        Toast.makeText(this, "Error al actualizar el perfil: " + errorMsg, Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                    }
                });
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (userEmail == null) return;
        
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show();
        String fileName = "profile_" + userEmail.replace("@", "_").replace(".", "_");

        MediaManager.get().upload(imageUri)
                .option("folder", CLOUDINARY_FOLDER)
                .option("public_id", fileName)
                .option("overwrite", true)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        if (imageUrl != null) {
                            updateProfileImageUrl(imageUrl);
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                         runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Error al subir imagen.", Toast.LENGTH_SHORT).show());
                    }
                    
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void updateProfileImageUrl(String imageUrl) {
        if (userEmail == null) return;

        // Verificar autenticación
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Error: Sesión expirada", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizar en Firestore
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("profile_image_url", imageUrl);
        
        db.collection("users").document(userEmail)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    // Actualizar en Room
                    ioExecutor.execute(() -> roomDb.userDao().updateProfileUrl(userEmail, imageUrl));
                    Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Si falla update, intentar con set
                    Map<String, Object> fullData = new HashMap<>();
                    fullData.put("email", userEmail);
                    fullData.put("profile_image_url", imageUrl);
                    db.collection("users").document(userEmail)
                            .set(fullData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                ioExecutor.execute(() -> roomDb.userDao().updateProfileUrl(userEmail, imageUrl));
                                Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e2 -> {
                                Toast.makeText(this, "Error al actualizar foto: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void setupKeyboardHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
            int imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(), scrollView.getPaddingRight(), imeInsets);
            return insets;
        });
    }
}