package com.example.sivareats.LOGIN;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.User;
import com.example.sivareats.data.UserDao;
import com.example.sivareats.ui.NavegacionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox checkBox;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "loginPrefs";

    private UserDao userDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        checkBox = findViewById(R.id.checkBox);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegistrate = findViewById(R.id.tvRegistrate);

        // Room
        AppDatabase db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadLoginData();

        // Login local + Firebase Auth
        btnLogin.setOnClickListener(v -> loginUserLocal());

        // Ir a registro
        tvRegistrate.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, LoginRegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUserLocal() {
        String email = safeText(etEmail);
        String password = safeText(etPassword);

        // Validaciones UI
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo");
            etEmail.requestFocus();
            toast("Completa todos los campos");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Formato de correo inválido");
            etEmail.requestFocus();
            toast("Formato de email inválido");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa tu contraseña");
            etPassword.requestFocus();
            toast("Completa todos los campos");
            return;
        }

        findViewById(R.id.btnLogin).setEnabled(false);

        // Primero validar en BD local (Room)
        ioExecutor.execute(() -> {
            boolean ok = false;
            try {
                ok = userDao.validateLogin(email, password);
            } catch (Exception ignored) {}

            boolean finalOk = ok;
            runOnUiThread(() -> {
                if (finalOk) {
                    // Si la validación local es exitosa, autenticar con Firebase Auth
                    authenticateWithFirebase(email, password);
                } else {
                    findViewById(R.id.btnLogin).setEnabled(true);
                    toast("Correo o contraseña inválidos");
                }
            });
        });
    }

    private void authenticateWithFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    findViewById(R.id.btnLogin).setEnabled(true);
                    if (task.isSuccessful()) {
                        // Autenticación exitosa
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Guardar email en SharedPreferences (loginPrefs)
                            if (checkBox.isChecked()) {
                                saveLoginData(email);
                            } else {
                                // Aún guardar el email aunque no esté marcado "recordar"
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("email", email);
                                editor.putBoolean("remember", false);
                                editor.apply();
                            }
                            // También guardar en SivarEatsPrefs para uso en otras actividades
                            SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                            sessionPrefs.edit().putString("CURRENT_USER_EMAIL", email).apply();
                            
                            // Sincronizar con Firestore (3ra forma: colección)
                            syncUserToFirestore(email);
                            
                            toast("Inicio de sesión exitoso");
                            goToMainScreen();
                        }
                    } else {
                        // Si no existe en Firebase Auth, crearlo
                        createFirebaseUser(email, password);
                    }
                });
    }

    private void createFirebaseUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    findViewById(R.id.btnLogin).setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Guardar email en SharedPreferences (loginPrefs)
                            if (checkBox.isChecked()) {
                                saveLoginData(email);
                            } else {
                                // Aún guardar el email aunque no esté marcado "recordar"
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("email", email);
                                editor.putBoolean("remember", false);
                                editor.apply();
                            }
                            // También guardar en SivarEatsPrefs para uso en otras actividades
                            SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                            sessionPrefs.edit().putString("CURRENT_USER_EMAIL", email).apply();
                            
                            // Sincronizar con Firestore (3ra forma: colección)
                            syncUserToFirestore(email);
                            
                            toast("Usuario creado y autenticado");
                            goToMainScreen();
                        }
                    } else {
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : "Error desconocido";
                        toast("Error al autenticar: " + errorMsg);
                    }
                });
    }

    /**
     * Sincroniza el usuario con Firestore (colección "users").
     * Obtiene los datos desde Room y los guarda/actualiza en Firestore.
     */
    private void syncUserToFirestore(String email) {
        if (firestore == null) {
            return;
        }

        // Obtener datos completos del usuario desde Room
        ioExecutor.execute(() -> {
            try {
                User localUser = userDao.findByEmail(email);
                if (localUser != null) {
                    // Preparar datos para Firestore
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", localUser.getName());
                    userData.put("email", localUser.getEmail());
                    userData.put("alias", localUser.getAlias() != null ? localUser.getAlias() : "");
                    userData.put("telefono", localUser.getTelefono() != null ? localUser.getTelefono() : "");
                    userData.put("profile_image_url", localUser.getProfileImageUrl() != null ? localUser.getProfileImageUrl() : "");
                    userData.put("rol", localUser.getRol() != null ? localUser.getRol() : "USUARIO_NORMAL");
                    userData.put("lastLoginAt", FieldValue.serverTimestamp());
                    
                    // Si el documento no existe, agregar createdAt
                    firestore.collection("users").document(email)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (!documentSnapshot.exists()) {
                                    userData.put("createdAt", FieldValue.serverTimestamp());
                                }
                                
                                // Guardar/actualizar en Firestore
                                firestore.collection("users")
                                        .document(email)
                                        .set(userData, SetOptions.merge())
                                        .addOnSuccessListener(unused -> {
                                            // Sincronización exitosa
                                        })
                                        .addOnFailureListener(e -> {
                                            // Error silencioso, no afecta el login
                                        });
                            });
                }
            } catch (Exception e) {
                // Error silencioso, no afecta el login
            }
        });
    }

    // Utils
    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void saveLoginData(String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putBoolean("remember", true);
        editor.apply();
    }

    private void loadLoginData() {
        boolean remember = sharedPreferences.getBoolean("remember", false);
        if (remember) {
            etEmail.setText(sharedPreferences.getString("email", ""));
            checkBox.setChecked(true);
        }
    }

    private void clearLoginData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private void goToMainScreen() {
        Intent intent = new Intent(LoginActivity.this, NavegacionActivity.class);
        startActivity(intent);
        finish();
    }
}
