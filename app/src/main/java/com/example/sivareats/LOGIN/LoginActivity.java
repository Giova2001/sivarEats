package com.example.sivareats.LOGIN;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etCountryCode, etPhoneNumber;
    private CheckBox checkBox;
    private Button btnCorreo, btnTelefono;
    private android.view.View emailContainer, phoneContainer, passwordContainer;
    private boolean isEmailMode = true; // true = correo, false = teléfono
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
        
        // Sincronizar usuarios antiguos de Room a Firebase (solo una vez al iniciar)
        syncAllUsersFromRoomToFirebase();

        // Vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etCountryCode = findViewById(R.id.etCountryCode);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        checkBox = findViewById(R.id.checkBox);
        btnCorreo = findViewById(R.id.btnCorreo);
        btnTelefono = findViewById(R.id.btnTelefono);
        emailContainer = findViewById(R.id.emailContainer);
        phoneContainer = findViewById(R.id.phoneContainer);
        passwordContainer = findViewById(R.id.passwordContainer);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegistrate = findViewById(R.id.tvRegistrate);

        // Room
        AppDatabase db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadLoginData();

        // Configurar cambio de modo
        setupModeSwitching();
        
        // Configurar scroll automático cuando aparece el teclado
        setupKeyboardScroll();

        // Login local + Firebase Auth
        btnLogin.setOnClickListener(v -> {
            if (isEmailMode) {
                loginUserLocal();
            } else {
                loginWithPhone();
            }
        });

        // Ir a registro
        tvRegistrate.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, LoginRegisterActivity.class);
            startActivity(intent);
        });
    }

    private void setupModeSwitching() {
        btnCorreo.setOnClickListener(v -> switchToEmailMode());
        btnTelefono.setOnClickListener(v -> switchToPhoneMode());
        
        // Inicializar en modo correo
        switchToEmailMode();
    }

    private void switchToEmailMode() {
        isEmailMode = true;
        btnCorreo.setBackgroundColor(ContextCompat.getColor(this, R.color.info));
        btnCorreo.setTextColor(ContextCompat.getColor(this, R.color.text_on_secondary));
        btnTelefono.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnTelefono.setTextColor(android.graphics.Color.BLACK);
        
        emailContainer.setVisibility(android.view.View.VISIBLE);
        phoneContainer.setVisibility(android.view.View.GONE);
        passwordContainer.setVisibility(android.view.View.VISIBLE);
        
        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setText("Iniciar sesión");
    }

    private void switchToPhoneMode() {
        isEmailMode = false;
        btnTelefono.setBackgroundColor(ContextCompat.getColor(this, R.color.info));
        btnTelefono.setTextColor(ContextCompat.getColor(this, R.color.text_on_secondary));
        btnCorreo.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnCorreo.setTextColor(android.graphics.Color.BLACK);
        
        emailContainer.setVisibility(android.view.View.GONE);
        phoneContainer.setVisibility(android.view.View.VISIBLE);
        passwordContainer.setVisibility(android.view.View.GONE);
        
        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setText("Enviar SMS");
    }

    private void loginWithPhone() {
        String countryCode = safeText(etCountryCode);
        String phoneNumber = safeText(etPhoneNumber);
        String fullPhoneNumber = countryCode + phoneNumber;

        // Validaciones
        if (TextUtils.isEmpty(countryCode)) {
            etCountryCode.setError("Ingresa el código de país");
            etCountryCode.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            etPhoneNumber.setError("Ingresa tu número de teléfono");
            etPhoneNumber.requestFocus();
            return;
        }

        // Por ahora solo mostrar un mensaje, la implementación completa de SMS
        // requeriría Firebase Phone Authentication
        toast("Funcionalidad de SMS en desarrollo. Usa el modo correo por ahora.");
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
                            
                            // Obtener rol del usuario desde Firestore y guardarlo
                            loadAndSaveUserRole(email);
                            
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
                            
                            // Obtener rol del usuario desde Firestore y guardarlo
                            loadAndSaveUserRole(email);
                            
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
     * Sincroniza todos los usuarios de Room a Firebase (Firestore y Auth).
     * Esto asegura que usuarios antiguos se migren automáticamente.
     */
    private void syncAllUsersFromRoomToFirebase() {
        ioExecutor.execute(() -> {
            try {
                List<User> allUsers = userDao.getAllUsers();
                if (allUsers != null && !allUsers.isEmpty()) {
                    for (User user : allUsers) {
                        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                            syncSingleUserToFirebase(user);
                        }
                    }
                }
            } catch (Exception e) {
                // Error silencioso, no afecta el login
            }
        });
    }

    /**
     * Sincroniza un usuario individual de Room a Firebase.
     */
    private void syncSingleUserToFirebase(User localUser) {
        if (localUser == null || localUser.getEmail() == null) return;

        // Verificar si existe en Firestore
        firestore.collection("users").document(localUser.getEmail()).get()
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
                        userData.put("createdAt", FieldValue.serverTimestamp());
                        userData.put("lastLoginAt", FieldValue.serverTimestamp());

                        firestore.collection("users")
                                .document(localUser.getEmail())
                                .set(userData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    // Usuario sincronizado a Firestore
                                    // Intentar crear en Firebase Auth si no existe
                                    createUserInFirebaseAuthIfNeeded(localUser.getEmail(), localUser.getPassword());
                                });
                    }
                });
    }

    /**
     * Crea el usuario en Firebase Auth si no existe.
     */
    private void createUserInFirebaseAuthIfNeeded(String email, String password) {
        // Intentar autenticar primero para ver si existe
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnFailureListener(e -> {
                    // Si falla, intentar crear el usuario
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                // Usuario creado en Firebase Auth
                            })
                            .addOnFailureListener(e2 -> {
                                // Error al crear, probablemente ya existe o hay un problema
                            });
                });
    }

    /**
     * Sincroniza el usuario con Firestore (colección "users").
     * Obtiene los datos desde Room y los guarda/actualiza en Firestore.
     */
    /**
     * Carga el rol del usuario desde Firestore y lo guarda en SharedPreferences.
     */
    /**
     * Carga el rol del usuario desde Firestore y lo guarda en SharedPreferences.
     */
    private void loadAndSaveUserRole(String email) {
        firestore.collection("users").document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String rol = "USUARIO_NORMAL"; // Valor por defecto
                    
                    if (documentSnapshot.exists()) {
                        // Obtener rol desde Firestore
                        String firestoreRol = documentSnapshot.getString("rol");
                        if (firestoreRol != null && !firestoreRol.isEmpty()) {
                            rol = firestoreRol;
                            // Guardar email y rol en SivarEatsPrefs
                            saveUserRoleToPrefs(email, rol);
                        } else {
                            // Si no existe en Firestore, intentar obtenerlo desde Room
                            loadRoleFromRoomAndSave(email);
                        }
                    } else {
                        // Si no existe en Firestore, intentar obtenerlo desde Room
                        loadRoleFromRoomAndSave(email);
                    }
                })
                .addOnFailureListener(e -> {
                    // Si falla obtener de Firestore, intentar desde Room
                    loadRoleFromRoomAndSave(email);
                });
    }
    
    /**
     * Carga el rol desde Room y lo guarda en SharedPreferences.
     */
    private void loadRoleFromRoomAndSave(String email) {
        ioExecutor.execute(() -> {
            try {
                User localUser = userDao.findByEmail(email);
                String rol = "USUARIO_NORMAL";
                if (localUser != null && localUser.getRol() != null && !localUser.getRol().isEmpty()) {
                    rol = localUser.getRol();
                }
                saveUserRoleToPrefs(email, rol);
            } catch (Exception ex) {
                // Error silencioso, usar valor por defecto
                saveUserRoleToPrefs(email, "USUARIO_NORMAL");
            }
        });
    }
    
    /**
     * Guarda el email y rol del usuario en SharedPreferences.
     */
    private void saveUserRoleToPrefs(String email, String rol) {
        SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sessionPrefs.edit();
        editor.putString("CURRENT_USER_EMAIL", email);
        editor.putString("CURRENT_USER_ROL", rol);
        editor.apply();
    }

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

    private void setupKeyboardScroll() {
        final ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Obtener la altura actual de la vista raíz
                    View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
                    int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                    
                    // Si la diferencia es significativa, el teclado está visible
                    if (heightDiff > 200) {
                        // Hacer scroll al campo activo
                        View focusedView = getCurrentFocus();
                        if (focusedView != null) {
                            scrollView.post(() -> {
                                int scrollAmount = focusedView.getBottom() - (scrollView.getHeight() - 300);
                                if (scrollAmount > 0) {
                                    scrollView.smoothScrollBy(0, scrollAmount);
                                }
                            });
                        }
                    }
                }
            });
        }
        
        // También agregar listeners a los campos para hacer scroll cuando se enfocan
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToView(v);
            }
        });
        
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToView(v);
            }
        });
        
        etPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToView(v);
            }
        });
    }
    
    private void scrollToView(View view) {
        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.post(() -> {
                int[] location = new int[2];
                view.getLocationInWindow(location);
                int scrollY = location[1] - 200; // Dejar un margen de 200px desde arriba
                if (scrollY > 0) {
                    scrollView.smoothScrollTo(0, scrollY);
                }
            });
        }
    }

    private void goToMainScreen() {
        Intent intent = new Intent(LoginActivity.this, NavegacionActivity.class);
        startActivity(intent);
        finish();
    }
}
