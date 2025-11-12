package com.example.sivareats.LOGIN;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.User;
import com.example.sivareats.data.UserDao;

// ★ Firebase
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginRegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ImageButton btnBack;

    private UserDao userDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // ★ Firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_register);

        // Firebase (opcional pero seguro)
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Vistas
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        // Room
        AppDatabase db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // Volver
        btnBack.setOnClickListener(v -> finish());

        // Registrar (local + Firestore)
        btnRegister.setOnClickListener(v -> registerUserLocal());
    }

    private void registerUserLocal() {
        // 1) Obtener datos
        String name = safeText(etName);
        String email = safeText(etEmail);
        String password = safeText(etPassword);
        String confirmPassword = safeText(etConfirmPassword);

        // 2) Validaciones
        if (!validateName(name)) return;
        if (!validateEmail(email)) return;
        if (!validatePassword(password, confirmPassword)) return;

        // 3) Operación en Room (hilo de I/O)
        final String fName = name;
        final String fEmail = email;

        ioExecutor.execute(() -> {
            try {
                boolean exists = userDao.existsByEmail(fEmail);
                if (exists) {
                    runOnUiThread(() -> {
                        etEmail.setError("El correo ya está registrado");
                        etEmail.requestFocus();
                        toast("El correo ya está registrado");
                    });
                    return;
                }

                long id = userDao.insert(new User(fName, fEmail, password));

                runOnUiThread(() -> {
                    toast("Registro local exitoso (ID " + id + ")");

                    // ★ Crear usuario en Firebase Auth y luego guardar en Firestore
                    createFirebaseUserAndSave(fName, fEmail, password);
                });

            } catch (Exception e) {
                runOnUiThread(() -> toast("Error al guardar: " + e.getMessage()));
            }
        });
    }

    // ===========================
    // ★ Crear usuario en Firebase Auth y guardar en Firestore
    // ===========================
    private void createFirebaseUserAndSave(String name, String email, String password) {
        if (mAuth == null) {
            toast("Firebase Auth no inicializado");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Guardar en Firestore después de crear en Auth
                            saveUserToFirestore(name, email);
                            clearFields();
                            etName.postDelayed(this::finish, 900);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : "Error desconocido";
                        toast("Error al crear usuario en Firebase: " + errorMsg);
                    }
                });
    }

    /**
     * Guarda/actualiza el usuario en la colección "users" usando el email como ID de documento.
     * Estructura completa del documento en Firestore.
     */
    private void saveUserToFirestore(String name, String email) {
        if (firestore == null) {
            toast("Firestore no inicializado");
            return;
        }

        // Obtener datos completos desde Room para sincronizar todo
        ioExecutor.execute(() -> {
            try {
                User localUser = userDao.findByEmail(email);
                
                // Preparar datos completos para Firestore
                Map<String, Object> data = new HashMap<>();
                data.put("name", name);
                data.put("email", email);
                data.put("alias", localUser != null && localUser.getAlias() != null ? localUser.getAlias() : "");
                data.put("telefono", localUser != null && localUser.getTelefono() != null ? localUser.getTelefono() : "");
                data.put("profile_image_url", localUser != null && localUser.getProfileImageUrl() != null ? localUser.getProfileImageUrl() : "");
                data.put("rol", localUser != null && localUser.getRol() != null ? localUser.getRol() : "USUARIO_NORMAL");
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("lastLoginAt", FieldValue.serverTimestamp());

                // Usamos el email como ID de documento para evitar duplicados.
                firestore.collection("users")
                        .document(email)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener(unused -> {
                            runOnUiThread(() -> toast("Usuario guardado en Firebase (Auth + Firestore)"));
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> toast("No se pudo guardar en Firestore: " + e.getMessage()));
                        });
            } catch (Exception e) {
                // Si falla obtener de Room, guardar con datos básicos
                Map<String, Object> basicData = new HashMap<>();
                basicData.put("name", name);
                basicData.put("email", email);
                basicData.put("alias", "");
                basicData.put("telefono", "");
                basicData.put("profile_image_url", "");
                basicData.put("rol", "USUARIO_NORMAL");
                basicData.put("createdAt", FieldValue.serverTimestamp());
                basicData.put("lastLoginAt", FieldValue.serverTimestamp());

                firestore.collection("users")
                        .document(email)
                        .set(basicData, SetOptions.merge())
                        .addOnSuccessListener(unused -> {
                            runOnUiThread(() -> toast("Usuario guardado en Firebase (Auth + Firestore)"));
                        })
                        .addOnFailureListener(e2 -> {
                            runOnUiThread(() -> toast("No se pudo guardar en Firestore: " + e2.getMessage()));
                        });
            }
        });
    }

    // --------------------
    // Validaciones
    // --------------------
    private boolean validateName(String name) {
        if (TextUtils.isEmpty(name)) {
            etName.setError("Ingresa tu nombre");
            etName.requestFocus();
            toast("Completa todos los campos");
            return false;
        }
        if (name.length() < 2) {
            etName.setError("Nombre demasiado corto");
            etName.requestFocus();
            toast("El nombre debe tener al menos 2 caracteres");
            return false;
        }
        return true;
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo");
            etEmail.requestFocus();
            toast("Completa todos los campos");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Formato de correo inválido");
            etEmail.requestFocus();
            toast("Formato de email inválido");
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password, String confirmPassword) {
        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            if (TextUtils.isEmpty(password)) etPassword.setError("Ingresa una contraseña");
            if (TextUtils.isEmpty(confirmPassword)) etConfirmPassword.setError("Confirma tu contraseña");
            toast("Completa todos los campos");
            return false;
        }
        if (password.length() < 8) {
            etPassword.setError("Mínimo 8 caracteres");
            etPassword.requestFocus();
            toast("La contraseña debe tener al menos 8 caracteres");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("No coincide con la contraseña");
            etConfirmPassword.requestFocus();
            toast("Las contraseñas no coinciden");
            return false;
        }
        return true;
    }

    // --------------------
    // Utilidades
    // --------------------
    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void clearFields() {
        etName.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
