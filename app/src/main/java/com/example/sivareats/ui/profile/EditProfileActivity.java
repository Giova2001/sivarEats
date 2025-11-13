package com.example.sivareats.ui.profile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.User;
import com.example.sivareats.data.UserDao;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

        // --- Cargar usuario ---
        loadUserFromFirestore(userEmail);

        // --- Guardar cambios ---
        btnGuardar.setOnClickListener(v -> saveChanges());
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
