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
import androidx.activity.OnBackPressedCallback;
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

        btnSave.setOnClickListener(v -> saveUserProfile());
        btnCambiarFoto.setOnClickListener(v -> openGallery());
        setupKeyboardHandling();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Usar OnBackPressedDispatcher en lugar de onBackPressed() deprecado
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        toolbar.setNavigationOnClickListener(v -> callback.handleOnBackPressed());

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
        roomDb = AppDatabase.getInstance(getApplicationContext());
        
        // Intentar inicializar Firebase de manera segura
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            
            // Intentar obtener usuario de Firebase Auth, pero no bloquear si falla
            try {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null && currentUser.getEmail() != null) {
                    userEmail = currentUser.getEmail();
                    Log.d("EditProfile", "Usuario encontrado en Firebase Auth: " + userEmail);
                    // Guardar en SharedPreferences para uso futuro
                    SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                    sessionPrefs.edit().putString("CURRENT_USER_EMAIL", userEmail).apply();
                    SharedPreferences loginPrefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
                    loginPrefs.edit().putString("email", userEmail).apply();
                    // Cargar perfil
                    loadUserProfile();
                    return;
                }
            } catch (Exception e) {
                Log.w("EditProfile", "Error al obtener usuario de Firebase Auth (continuando sin Firebase): " + e.getMessage());
                // Continuar sin Firebase Auth
            }
        } catch (Exception e) {
            Log.w("EditProfile", "Error al inicializar Firebase (continuando sin Firebase): " + e.getMessage());
            // Continuar sin Firebase
        }

        // Como fallback, intentar obtenerlo de múltiples fuentes
        Log.d("EditProfile", "Buscando usuario en SharedPreferences y Room...");
        SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        userEmail = sessionPrefs.getString("CURRENT_USER_EMAIL", null);

        if (userEmail == null) {
            SharedPreferences loginPrefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
            userEmail = loginPrefs.getString("email", null);
        }

        // Si aún no se encuentra, intentar obtener desde Room (último usuario registrado)
        if (userEmail == null) {
            Log.d("EditProfile", "Buscando usuario en Room...");
            ioExecutor.execute(() -> {
                try {
                    User lastUser = roomDb.userDao().findFirstUser();
                    if (lastUser != null && lastUser.getEmail() != null) {
                        String foundEmail = lastUser.getEmail();
                        Log.d("EditProfile", "Usuario encontrado en Room: " + foundEmail);
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
                        Log.e("EditProfile", "No se encontró usuario en Room");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error: Usuario no identificado. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                } catch (Exception e) {
                    Log.e("EditProfile", "Error al buscar usuario en Room", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: Usuario no identificado. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            });
            return; // Salir aquí, loadUserProfile se llamará desde el hilo si encuentra usuario
        } else {
            Log.d("EditProfile", "Usuario encontrado en SharedPreferences: " + userEmail);
            // Si encontramos el email en SharedPreferences, cargar el perfil
            loadUserProfile();
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
        if (userEmail == null || userEmail.isEmpty()) {
            Log.e("EditProfile", "userEmail es null o vacío, no se puede cargar el perfil");
            return;
        }

        Log.d("EditProfile", "Cargando perfil para: " + userEmail);

        // Intentar cargar desde Firestore si está disponible
        if (db != null) {
            try {
                db.collection("users").document(userEmail).get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                Log.d("EditProfile", "Usuario encontrado en Firestore");
                                User user = document.toObject(User.class);
                                if(user != null) {
                                    populateUi(user);
                                    // Sincronizar con Room
                                    syncUserToRoom(user);
                                } else {
                                    Log.e("EditProfile", "Error al convertir documento a User, cargando desde Room");
                                    loadUserFromRoom();
                                }
                            } else {
                                Log.d("EditProfile", "Usuario no encontrado en Firestore, cargando desde Room");
                                // Si no existe en Firestore, cargar desde Room
                                loadUserFromRoom();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.w("EditProfile", "Error al cargar de Firestore, cargando de Room: " + e.getMessage());
                            loadUserFromRoom();
                        });
                return; // Salir si Firestore está disponible
            } catch (Exception e) {
                Log.w("EditProfile", "Error al acceder a Firestore, cargando de Room: " + e.getMessage());
            }
        }
        
        // Si Firestore no está disponible o falla, cargar directamente desde Room
        loadUserFromRoom();
    }

    private void loadUserFromRoom(){
        if (userEmail == null || userEmail.isEmpty()) {
            Log.e("EditProfile", "userEmail es null, no se puede cargar desde Room");
            return;
        }

        Log.d("EditProfile", "Cargando usuario desde Room: " + userEmail);
        ioExecutor.execute(() -> {
            try {
                User user = roomDb.userDao().findByEmail(userEmail);
                if (user != null) {
                    Log.d("EditProfile", "Usuario encontrado en Room, mostrando en UI");
                    runOnUiThread(() -> {
                        populateUi(user);
                        // Intentar sincronizar este usuario a Firebase si está disponible
                        if (db != null) {
                            syncRoomUserToFirestore(user);
                        }
                    });
                } else {
                    Log.e("EditProfile", "Usuario no encontrado en Room: " + userEmail);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Usuario no encontrado. Por favor, inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("EditProfile", "Error al cargar usuario desde Room", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error al cargar datos del usuario.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Sincroniza un usuario de Room a Firestore si no existe en Firebase.
     * También intenta autenticarlo en Firebase Auth si no está autenticado.
     */
    private void syncRoomUserToFirestore(User localUser) {
        if (localUser == null || localUser.getEmail() == null || db == null) return;

        try {
            // Verificar si existe en Firestore
            db.collection("users").document(localUser.getEmail()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // No existe en Firestore, sincronizarlo
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", localUser.getName());
                        userData.put("email", localUser.getEmail());
                        userData.put("alias", localUser.getAlias() != null ? localUser.getAlias() : "");
                        userData.put("telefono", localUser.getTelefono() != null ? localUser.getTelefono() : "");
                        userData.put("profile_image_url", localUser.getProfileImageUrl() != null ? localUser.getProfileImageUrl() : "");
                        userData.put("rol", localUser.getRol() != null ? localUser.getRol() : "USUARIO_NORMAL");
                        userData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        userData.put("lastLoginAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                        db.collection("users")
                                .document(localUser.getEmail())
                                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("EditProfile", "Usuario sincronizado a Firestore: " + localUser.getEmail());
                                    // Intentar autenticar en Firebase Auth si no está autenticado
                                    tryAuthenticateUser(localUser.getEmail(), localUser.getPassword());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("EditProfile", "Error al sincronizar usuario a Firestore", e);
                                });
                    } else {
                        // Ya existe en Firestore, solo intentar autenticar si no está autenticado
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser == null) {
                            tryAuthenticateUser(localUser.getEmail(), localUser.getPassword());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("EditProfile", "Error al verificar usuario en Firestore (continuando sin sincronización): " + e.getMessage());
                });
        } catch (Exception e) {
            Log.w("EditProfile", "Error al sincronizar con Firestore (continuando sin sincronización): " + e.getMessage());
        }
    }

    /**
     * Intenta autenticar al usuario en Firebase Auth usando las credenciales de Room.
     */
    private void tryAuthenticateUser(String email, String password) {
        if (mAuth == null) {
            Log.w("EditProfile", "Firebase Auth no está disponible, omitiendo autenticación");
            return;
        }
        
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null && currentUser.getEmail().equals(email)) {
                // Ya está autenticado
                return;
            }

            // Intentar autenticar
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d("EditProfile", "Usuario autenticado en Firebase Auth: " + email);
                    // Guardar email en SharedPreferences
                    SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                    sessionPrefs.edit().putString("CURRENT_USER_EMAIL", email).apply();
                    SharedPreferences loginPrefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
                    loginPrefs.edit().putString("email", email).apply();
                })
                .addOnFailureListener(e -> {
                    // Si falla el login, intentar crear el usuario en Firebase Auth
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                Log.d("EditProfile", "Usuario creado en Firebase Auth: " + email);
                                SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                                sessionPrefs.edit().putString("CURRENT_USER_EMAIL", email).apply();
                                SharedPreferences loginPrefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
                                loginPrefs.edit().putString("email", email).apply();
                            })
                            .addOnFailureListener(e2 -> {
                                Log.w("EditProfile", "No se pudo autenticar ni crear usuario en Firebase Auth (continuando sin Firebase): " + e2.getMessage());
                            });
                });
        } catch (Exception e) {
            Log.w("EditProfile", "Error al intentar autenticar en Firebase Auth (continuando sin Firebase): " + e.getMessage());
        }
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

        // Crear un mapa con TODOS los campos a actualizar/agregar en Firestore.
        // Usar set con merge para agregar campos nuevos si no existen.
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("alias", newAlias != null ? newAlias : "");
        updates.put("telefono", newPhone != null ? newPhone : "");
        updates.put("email", userEmail); // Asegurar que el email esté presente
        updates.put("lastLoginAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        btnSave.setEnabled(false);

        // 1. Guardar en Firestore si está disponible
        if (db != null) {
            try {
                db.collection("users").document(userEmail)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
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
                    // Si falla, intentar con update como fallback
                    Map<String, Object> updateOnly = new HashMap<>();
                    updateOnly.put("name", newName);
                    updateOnly.put("alias", newAlias != null ? newAlias : "");
                    updateOnly.put("telefono", newPhone != null ? newPhone : "");

                    db.collection("users").document(userEmail)
                            .update(updateOnly)
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
                });
                return; // Salir si Firestore está disponible
            } catch (Exception e) {
                Log.w("EditProfile", "Error al acceder a Firestore, guardando solo en Room: " + e.getMessage());
            }
        }
        
        // Si Firestore no está disponible, guardar solo en Room
        ioExecutor.execute(() -> {
            roomDb.userDao().updateProfileDetails(userEmail, newName, newAlias, newPhone);
            runOnUiThread(() -> {
                Toast.makeText(this, "Perfil actualizado correctamente (solo local)", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                finish();
            });
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

        // Actualizar en Firestore si está disponible
        if (db != null) {
            try {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("profile_image_url", imageUrl);
                updateData.put("email", userEmail); // Asegurar que el email esté presente

                // Usar set con merge para asegurar que se guarde correctamente
                db.collection("users").document(userEmail)
                        .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Actualizar en Room
                    ioExecutor.execute(() -> roomDb.userDao().updateProfileUrl(userEmail, imageUrl));
                    Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Si falla set, intentar con update como fallback
                    Map<String, Object> updateOnly = new HashMap<>();
                    updateOnly.put("profile_image_url", imageUrl);
                    db.collection("users").document(userEmail)
                            .update(updateOnly)
                            .addOnSuccessListener(aVoid -> {
                                ioExecutor.execute(() -> roomDb.userDao().updateProfileUrl(userEmail, imageUrl));
                                Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e2 -> {
                                Log.w("EditProfile", "Error al actualizar foto en Firestore, guardando solo en Room: " + e2.getMessage());
                                // Guardar solo en Room como fallback
                                ioExecutor.execute(() -> roomDb.userDao().updateProfileUrl(userEmail, imageUrl));
                                runOnUiThread(() -> Toast.makeText(this, "Foto de perfil actualizada (solo local)", Toast.LENGTH_SHORT).show());
                            });
                });
                return; // Salir si Firestore está disponible
            } catch (Exception e) {
                Log.w("EditProfile", "Error al acceder a Firestore, guardando solo en Room: " + e.getMessage());
            }
        }
        
        // Si Firestore no está disponible, guardar solo en Room
        ioExecutor.execute(() -> {
            roomDb.userDao().updateProfileUrl(userEmail, imageUrl);
            runOnUiThread(() -> Toast.makeText(this, "Foto de perfil actualizada (solo local)", Toast.LENGTH_SHORT).show());
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