package com.example.sivareats.LOGIN;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuthException;
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
    private android.app.ProgressDialog progressDialog;

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

        // Mostrar animación de carga
        showLoadingDialog("Verificando credenciales...");

        // Siempre intentar autenticar primero con Firebase Auth
        // Esto permite que usuarios existentes en Firebase puedan iniciar sesión en dispositivos nuevos
        authenticateWithFirebase(email, password);
    }
    
    /**
     * Muestra un diálogo de carga mientras se verifica el login.
     */
    private void showLoadingDialog(String message) {
        hideLoadingDialog(); // Asegurarse de que no haya otro diálogo activo
        progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }
    
    /**
     * Oculta el diálogo de carga.
     */
    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void authenticateWithFirebase(String email, String password) {
        Log.d("LoginActivity", "Intentando autenticar: " + email);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    findViewById(R.id.btnLogin).setEnabled(true);
                    if (task.isSuccessful()) {
                        // Autenticación exitosa
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("LoginActivity", "Autenticación exitosa para: " + email);
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
                            // Esperar a que se cargue el rol antes de navegar
                            loadAndSaveUserRoleAndNavigate(email);
                            
                            // Sincronizar con Firestore (3ra forma: colección)
                            syncUserToFirestore(email);
                        }
                    } else {
                        // Si falla la autenticación, verificar si el usuario existe en Firestore
                        Exception exception = task.getException();
                        String errorMsg = exception != null ? exception.getMessage() : "Error desconocido";
                        String errorCode = "UNKNOWN";
                        
                        // Intentar obtener el código de error si es FirebaseAuthException
                        if (exception instanceof FirebaseAuthException) {
                            errorCode = ((FirebaseAuthException) exception).getErrorCode();
                        }
                        
                        // Log detallado del error
                        Log.e("LoginActivity", "=== ERROR DE AUTENTICACIÓN ===");
                        Log.e("LoginActivity", "Email: " + email);
                        Log.e("LoginActivity", "Error Code: " + errorCode);
                        Log.e("LoginActivity", "Error Message: " + errorMsg);
                        if (exception != null) {
                            Log.e("LoginActivity", "Exception Class: " + exception.getClass().getName());
                            Log.e("LoginActivity", "Exception toString: " + exception.toString());
                        }
                        Log.e("LoginActivity", "=============================");
                        
                        // SIEMPRE verificar primero en Firestore cuando falla Auth
                        // Esto permite manejar usuarios que existen en Firestore pero no en Auth
                        checkUserExistsInFirestoreAndHandle(email, password, errorMsg, errorCode);
                    }
                });
    }

    /**
     * Verifica si el usuario existe en Firestore cuando falla la autenticación en Firebase Auth.
     * Maneja diferentes casos: usuario no existe, contraseña incorrecta, etc.
     */
    private void checkUserExistsInFirestoreAndHandle(String email, String password, String errorMsg, String errorCode) {
        Log.d("LoginActivity", "Verificando si usuario existe en Firestore: " + email);
        Log.d("LoginActivity", "Error Code recibido: " + errorCode);
        
        // Actualizar mensaje de carga
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage("Verificando usuario en la base de datos...");
        }
        
        firestore.collection("users").document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // El usuario existe en Firestore pero no pudo autenticarse en Firebase Auth
                        Log.d("LoginActivity", "Usuario encontrado en Firestore: " + email);
                        
                        // Obtener el rol del documento
                        String rol = documentSnapshot.getString("rol");
                        if (rol == null || rol.isEmpty()) {
                            rol = "USUARIO_NORMAL";
                        }
                        Log.d("LoginActivity", "Rol del usuario: " + rol);
                        
                        // Analizar el error específico de Firebase Auth usando tanto el código como el mensaje
                        String errorMsgLower = errorMsg.toLowerCase();
                        String errorCodeLower = errorCode.toLowerCase();
                        Log.d("LoginActivity", "Analizando error - Code: " + errorCode + ", Message: " + errorMsgLower);
                        
                        // Verificar si el error indica que la contraseña es incorrecta
                        // Usar tanto el código de error como el mensaje para mayor precisión
                        if (errorCodeLower.equals("wrong-password") || 
                            errorCodeLower.equals("invalid-credential") ||
                            errorCodeLower.contains("wrong-password") ||
                            errorCodeLower.contains("invalid-credential") ||
                            errorMsgLower.contains("wrong-password") || 
                            errorMsgLower.contains("invalid-credential") ||
                            errorMsgLower.contains("wrong password") ||
                            errorMsgLower.contains("invalid credential") ||
                            (errorMsgLower.contains("password") && errorMsgLower.contains("invalid")) ||
                            errorMsgLower.contains("the password is invalid") ||
                            errorMsgLower.contains("password is invalid") ||
                            errorMsgLower.contains("incorrect password") ||
                            errorMsgLower.contains("auth/wrong-password") ||
                            errorMsgLower.contains("auth/invalid-credential")) {
                            // Contraseña incorrecta - el usuario existe en Firebase Auth pero la contraseña no coincide
                            Log.e("LoginActivity", "Contraseña incorrecta - usuario existe en Firebase Auth");
                            hideLoadingDialog();
                            toast("Correo o contraseña inválidos");
                            
                        } else if (errorCodeLower.equals("user-not-found") ||
                                   errorCodeLower.contains("user-not-found") ||
                                   errorMsgLower.contains("no user record") || 
                                   errorMsgLower.contains("user_not_found") ||
                                   errorMsgLower.contains("there is no user record") ||
                                   errorMsgLower.contains("user does not exist") ||
                                   errorMsgLower.contains("auth/user-not-found") ||
                                   errorMsgLower.contains("there is no user record corresponding")) {
                            // Usuario NO existe en Firebase Auth pero SÍ existe en Firestore
                            // Esto significa que el usuario fue creado directamente en Firestore sin pasar por Auth
                            Log.w("LoginActivity", "Usuario existe en Firestore pero NO en Firebase Auth");
                            Log.d("LoginActivity", "Intentando crear usuario en Firebase Auth con contraseña proporcionada...");
                            
                            // Intentar crear el usuario en Firebase Auth
                            createFirebaseUserWithFallback(email, password, rol);
                            
                        } else if (errorCodeLower.equals("email-already-in-use") ||
                                   errorCodeLower.contains("email-already-in-use") ||
                                   errorMsgLower.contains("email-already-in-use") ||
                                   errorMsgLower.contains("email already exists") ||
                                   errorMsgLower.contains("already registered") ||
                                   errorMsgLower.contains("auth/email-already-in-use")) {
                            // El usuario ya existe en Firebase Auth pero falló la autenticación
                            // Esto puede significar que la contraseña es incorrecta o hay un problema
                            Log.w("LoginActivity", "Usuario ya existe en Firebase Auth pero falló autenticación: " + errorMsg);
                            hideLoadingDialog();
                            toast("Correo o contraseña inválidos");
                            
                        } else {
                            // Otro tipo de error - como el usuario existe en Firestore, intentar permitir login
                            Log.w("LoginActivity", "Error desconocido de Firebase Auth pero usuario existe en Firestore: " + errorMsg);
                            Log.d("LoginActivity", "Error completo: " + errorMsg);
                            
                            // Si es un error de red o conexión, permitir login con Firestore
                            if (errorMsgLower.contains("network") || 
                                errorMsgLower.contains("connection") ||
                                errorMsgLower.contains("timeout") ||
                                errorMsgLower.contains("unavailable") ||
                                errorMsgLower.contains("network error") ||
                                errorMsgLower.contains("failed to connect")) {
                                Log.d("LoginActivity", "Error de conexión, permitiendo login con Firestore como fallback");
                                allowLoginWithFirestoreData(email, rol, errorMsg);
                            } else {
                                // Para cualquier otro error cuando el usuario existe en Firestore,
                                // intentar crear en Auth o permitir login con Firestore como último recurso
                                Log.d("LoginActivity", "Error desconocido, intentando crear en Auth o permitir login con Firestore");
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.setMessage("Intentando solucionar el problema...");
                                }
                                createFirebaseUserWithFallback(email, password, rol);
                            }
                        }
                    } else {
                        Log.d("LoginActivity", "Usuario NO encontrado en Firestore: " + email);
                        Log.d("LoginActivity", "Error de Firebase Auth: " + errorMsg);
                        
                        // El usuario no existe ni en Firestore ni en Firebase Auth
                        // Verificar si existe en Room antes de crear uno nuevo
                        ioExecutor.execute(() -> {
                            try {
                                User localUser = userDao.findByEmail(email);
                                boolean finalExists = (localUser != null);
                                runOnUiThread(() -> {
                                    if (finalExists) {
                                        // Existe en Room pero no en Firebase - sincronizar
                                        Log.d("LoginActivity", "Usuario encontrado en Room, sincronizando con Firebase...");
                                        if (progressDialog != null && progressDialog.isShowing()) {
                                            progressDialog.setMessage("Sincronizando cuenta...");
                                        }
                                        syncLocalUserToFirebase(email, password);
                                    } else {
                                        // No existe en ningún lugar - mostrar error
                                        hideLoadingDialog();
                                        toast("Correo o contraseña inválidos");
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("LoginActivity", "Error al buscar en Room: " + e.getMessage());
                                runOnUiThread(() -> {
                                    hideLoadingDialog();
                                    toast("Correo o contraseña inválidos");
                                });
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Error al verificar en Firestore, intentar con Room como fallback
                    Log.e("LoginActivity", "Error al verificar en Firestore: " + e.getMessage());
                    hideLoadingDialog();
                    tryLoginLocalOnly(email, password, errorMsg);
                });
    }
    
    /**
     * Permite login usando solo datos de Firestore cuando falla Firebase Auth.
     * Esto es un fallback para usuarios que existen en Firestore pero tienen problemas con Auth.
     */
    private void allowLoginWithFirestoreData(String email, String rol, String errorMsg) {
        Log.d("LoginActivity", "Permitiendo login con datos de Firestore para: " + email);
        
        // Ocultar diálogo de carga antes de continuar
        hideLoadingDialog();
        
        // Guardar email en SharedPreferences
        if (checkBox.isChecked()) {
            saveLoginData(email);
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("email", email);
            editor.putBoolean("remember", false);
            editor.apply();
        }
        
        // Guardar email y rol en SivarEatsPrefs
        saveUserRoleToPrefs(email, rol);
        
        // Actualizar lastLoginAt en Firestore
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastLoginAt", FieldValue.serverTimestamp());
        firestore.collection("users").document(email).update(updateData);
        
        // Navegar a la pantalla principal
        toast("Inicio de sesión exitoso");
        goToMainScreen();
        
        // Mostrar advertencia sobre posible problema con Auth
        Log.w("LoginActivity", "Login realizado con datos de Firestore. Error de Auth: " + errorMsg);
    }
    
    /**
     * Sincroniza un usuario de Room a Firebase Auth y Firestore.
     */
    private void syncLocalUserToFirebase(String email, String password) {
        ioExecutor.execute(() -> {
            try {
                User localUser = userDao.findByEmail(email);
                if (localUser != null) {
                    // Primero crear en Firestore
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", localUser.getName() != null ? localUser.getName() : "");
                    userData.put("email", email);
                    userData.put("alias", localUser.getAlias() != null ? localUser.getAlias() : "");
                    userData.put("telefono", localUser.getTelefono() != null ? localUser.getTelefono() : "");
                    userData.put("profile_image_url", localUser.getProfileImageUrl() != null ? localUser.getProfileImageUrl() : "");
                    userData.put("rol", localUser.getRol() != null && !localUser.getRol().isEmpty() ? localUser.getRol() : "USUARIO_NORMAL");
                    userData.put("createdAt", FieldValue.serverTimestamp());
                    userData.put("lastLoginAt", FieldValue.serverTimestamp());
                    
                    firestore.collection("users")
                            .document(email)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                // Luego crear en Firebase Auth
                                createFirebaseUser(email, password);
                            })
                            .addOnFailureListener(e -> {
                                runOnUiThread(() -> {
                                    toast("Error al sincronizar cuenta");
                                });
                            });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    toast("Error al sincronizar cuenta");
                });
            }
        });
    }
    
    /**
     * Intenta hacer login solo con Room (sin internet).
     */
    private void tryLoginLocalOnly(String email, String password, String firebaseError) {
        ioExecutor.execute(() -> {
            boolean ok = false;
            try {
                ok = userDao.validateLogin(email, password);
            } catch (Exception ignored) {}

            boolean finalOk = ok;
            runOnUiThread(() -> {
                if (finalOk) {
                    // Login local exitoso, guardar datos y continuar
                    if (checkBox.isChecked()) {
                        saveLoginData(email);
                    } else {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("email", email);
                        editor.putBoolean("remember", false);
                        editor.apply();
                    }
                    
                    // Obtener rol desde Room y guardarlo
                    loadRoleFromRoomAndSaveAndNavigate(email);
                    
                    toast("Inicio de sesión exitoso (modo offline)");
                } else {
                    hideLoadingDialog();
                    toast("Correo o contraseña inválidos");
                }
            });
        });
    }

    /**
     * Intenta crear el usuario en Firebase Auth, pero si falla, permite login usando datos de Firestore.
     * Este método se usa cuando el usuario existe en Firestore pero no en Firebase Auth.
     */
    private void createFirebaseUserWithFallback(String email, String password, String rol) {
        // Actualizar mensaje del diálogo de carga
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage("Sincronizando cuenta con Firebase...");
        } else {
            showLoadingDialog("Sincronizando cuenta con Firebase...");
        }
        
        Log.d("LoginActivity", "Intentando crear usuario en Firebase Auth: " + email);
        
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    findViewById(R.id.btnLogin).setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("LoginActivity", "Usuario creado exitosamente en Firebase Auth");
                            
                            // Guardar email en SharedPreferences
                            if (checkBox.isChecked()) {
                                saveLoginData(email);
                            } else {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("email", email);
                                editor.putBoolean("remember", false);
                                editor.apply();
                            }
                            
                            // Obtener rol del usuario desde Firestore y guardarlo
                            loadAndSaveUserRoleAndNavigate(email);
                            
                            // Sincronizar con Firestore (actualizar lastLoginAt)
                            syncUserToFirestore(email);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : "Error desconocido";
                        String errorMsgLower = errorMsg.toLowerCase();
                        Log.w("LoginActivity", "No se pudo crear usuario en Auth: " + errorMsg);
                        
                        // Si el usuario ya existe en Firebase Auth
                        if (errorMsgLower.contains("email-already-in-use") ||
                            errorMsgLower.contains("already exists") ||
                            errorMsgLower.contains("already in use") ||
                            errorMsgLower.contains("email_exists") ||
                            errorMsgLower.contains("already registered")) {
                            // El usuario ya existe en Auth pero no pudimos autenticarnos
                            // Esto significa que la contraseña proporcionada NO es la correcta para ese usuario
                            Log.e("LoginActivity", "Usuario ya existe en Auth con contraseña diferente");
                            hideLoadingDialog();
                            toast("Correo o contraseña inválidos");
                            
                        } else {
                            // Otro error al crear - permitir login con Firestore como último recurso
                            Log.w("LoginActivity", "Error al crear en Auth, permitiendo login con Firestore como fallback");
                            allowLoginWithFirestoreData(email, rol, errorMsg);
                        }
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
                            // Esperar a que se cargue el rol antes de navegar
                            loadAndSaveUserRoleAndNavigate(email);
                            
                            // Sincronizar con Firestore (3ra forma: colección)
                            syncUserToFirestore(email);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : "Error desconocido";
                        
                        // Si el error es que el usuario ya existe, intentar autenticar nuevamente
                        if (errorMsg.contains("already exists") || errorMsg.contains("already in use") ||
                            errorMsg.contains("EMAIL_EXISTS") || errorMsg.toLowerCase().contains("already registered")) {
                            // El usuario ya existe, intentar autenticar con las credenciales proporcionadas
                            toast("El usuario ya existe. Verificando credenciales...");
                            authenticateWithFirebase(email, password);
                        } else {
                            toast("Error al crear cuenta: " + errorMsg);
                        }
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
                        //userData.put("profile_image_url", localUser.getProfileImageUrl() != null ? localUser.getProfileImageUrl() : "");
                        //userData.put("rol", localUser.getRol() != null ? localUser.getRol() : "USUARIO_NORMAL");
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
     * Carga el rol del usuario desde Firestore y lo guarda en SharedPreferences, luego navega.
     * Si el documento no existe en Firestore, lo crea con datos básicos.
     */
    private void loadAndSaveUserRoleAndNavigate(String email) {
        firestore.collection("users").document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String rol = "USUARIO_NORMAL"; // Valor por defecto
                    
                    if (documentSnapshot.exists()) {
                        // Obtener rol desde Firestore
                        String firestoreRol = documentSnapshot.getString("rol");
                        if (firestoreRol != null && !firestoreRol.isEmpty()) {
                            rol = firestoreRol;
                        } else {
                            // Si el documento existe pero no tiene rol, actualizarlo
                            updateUserRoleInFirestore(email, rol);
                        }
                        
                        // Actualizar lastLoginAt
                        updateLastLoginTime(email);
                        
                        // Guardar email y rol en SivarEatsPrefs
                        saveUserRoleToPrefs(email, rol);
                        toast("Inicio de sesión exitoso");
                        goToMainScreen();
                    } else {
                        // El documento no existe en Firestore, crearlo con datos básicos
                        createUserDocumentInFirestore(email, rol);
                    }
                })
                .addOnFailureListener(e -> {
                    // Si falla obtener de Firestore, intentar desde Room como fallback
                    // y luego crear el documento en Firestore
                    loadRoleFromRoomAndSaveAndNavigate(email);
                });
    }
    
    /**
     * Crea el documento del usuario en Firestore si no existe.
     */
    private void createUserDocumentInFirestore(String email, String rol) {
        // Primero intentar obtener datos desde Room (si existen)
        ioExecutor.execute(() -> {
            try {
                User localUser = userDao.findByEmail(email);
                
                Map<String, Object> userData = new HashMap<>();
                if (localUser != null) {
                    // Usar datos de Room si están disponibles
                    userData.put("name", localUser.getName() != null ? localUser.getName() : "");
                    userData.put("email", email);
                    userData.put("alias", localUser.getAlias() != null ? localUser.getAlias() : "");
                    userData.put("telefono", localUser.getTelefono() != null ? localUser.getTelefono() : "");
                    userData.put("profile_image_url", localUser.getProfileImageUrl() != null ? localUser.getProfileImageUrl() : "");
                    userData.put("rol", localUser.getRol() != null && !localUser.getRol().isEmpty() ? localUser.getRol() : rol);
                    userData.put("createdAt", FieldValue.serverTimestamp());
                    userData.put("lastLoginAt", FieldValue.serverTimestamp());
                } else {
                    // Crear documento básico si no hay datos en Room
                    userData.put("name", email.split("@")[0]); // Usar parte antes del @ como nombre
                    userData.put("email", email);
                    userData.put("alias", "");
                    userData.put("telefono", "");
                    userData.put("profile_image_url", "");
                    userData.put("rol", rol);
                    userData.put("createdAt", FieldValue.serverTimestamp());
                    userData.put("lastLoginAt", FieldValue.serverTimestamp());
                }
                
                // Guardar/actualizar en Firestore
                firestore.collection("users")
                        .document(email)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            // Obtener el rol guardado
                            String finalRol = localUser != null && localUser.getRol() != null && !localUser.getRol().isEmpty() 
                                    ? localUser.getRol() : rol;
                            
                            // Guardar email y rol en SivarEatsPrefs
                            saveUserRoleToPrefs(email, finalRol);
                            
                            runOnUiThread(() -> {
                                toast("Inicio de sesión exitoso");
                                goToMainScreen();
                            });
                        })
                        .addOnFailureListener(e -> {
                            // Aún así, permitir login con rol por defecto
                            saveUserRoleToPrefs(email, rol);
                            runOnUiThread(() -> {
                                toast("Inicio de sesión exitoso");
                                goToMainScreen();
                            });
                        });
            } catch (Exception ex) {
                // Error al obtener de Room, crear documento básico
                Map<String, Object> userData = new HashMap<>();
                userData.put("name", email.split("@")[0]);
                userData.put("email", email);
                userData.put("alias", "");
                userData.put("telefono", "");
                userData.put("profile_image_url", "");
                userData.put("rol", rol);
                userData.put("createdAt", FieldValue.serverTimestamp());
                userData.put("lastLoginAt", FieldValue.serverTimestamp());
                
                firestore.collection("users")
                        .document(email)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            saveUserRoleToPrefs(email, rol);
                            runOnUiThread(() -> {
                                toast("Inicio de sesión exitoso");
                                goToMainScreen();
                            });
                        })
                        .addOnFailureListener(e -> {
                            saveUserRoleToPrefs(email, rol);
                            runOnUiThread(() -> {
                                toast("Inicio de sesión exitoso");
                                goToMainScreen();
                            });
                        });
            }
        });
    }
    
    /**
     * Actualiza el rol del usuario en Firestore si no tiene uno.
     */
    private void updateUserRoleInFirestore(String email, String rol) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("rol", rol);
        firestore.collection("users")
                .document(email)
                .update(updateData);
    }
    
    /**
     * Actualiza la última vez que el usuario inició sesión.
     */
    private void updateLastLoginTime(String email) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastLoginAt", FieldValue.serverTimestamp());
        firestore.collection("users")
                .document(email)
                .update(updateData);
    }
    
    /**
     * Carga el rol desde Room y lo guarda en SharedPreferences, luego navega.
     * También sincroniza con Firestore para asegurar que el documento exista.
     */
    private void loadRoleFromRoomAndSaveAndNavigate(String email) {
        ioExecutor.execute(() -> {
            try {
                User localUser = userDao.findByEmail(email);
                String rol = "USUARIO_NORMAL";
                if (localUser != null && localUser.getRol() != null && !localUser.getRol().isEmpty()) {
                    rol = localUser.getRol();
                }
                
                // Guardar email y rol en SivarEatsPrefs
                saveUserRoleToPrefs(email, rol);
                
                // Sincronizar con Firestore para asegurar que el documento exista
                syncUserToFirestoreFromRoom(email, localUser, rol);
                
                runOnUiThread(() -> {
                    toast("Inicio de sesión exitoso");
                    goToMainScreen();
                });
            } catch (Exception ex) {
                // Error silencioso, usar valor por defecto y crear documento básico en Firestore
                saveUserRoleToPrefs(email, "USUARIO_NORMAL");
                
                // Crear documento básico en Firestore
                Map<String, Object> userData = new HashMap<>();
                userData.put("name", email.split("@")[0]);
                userData.put("email", email);
                userData.put("alias", "");
                userData.put("telefono", "");
                userData.put("profile_image_url", "");
                userData.put("rol", "USUARIO_NORMAL");
                userData.put("createdAt", FieldValue.serverTimestamp());
                userData.put("lastLoginAt", FieldValue.serverTimestamp());
                
                firestore.collection("users")
                        .document(email)
                        .set(userData, SetOptions.merge());
                
                runOnUiThread(() -> {
                    toast("Inicio de sesión exitoso");
                    goToMainScreen();
                });
            }
        });
    }
    
    /**
     * Sincroniza el usuario desde Room a Firestore.
     */
    private void syncUserToFirestoreFromRoom(String email, User localUser, String rol) {
        if (localUser != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", localUser.getName() != null ? localUser.getName() : "");
            userData.put("email", email);
            userData.put("alias", localUser.getAlias() != null ? localUser.getAlias() : "");
            userData.put("telefono", localUser.getTelefono() != null ? localUser.getTelefono() : "");
            userData.put("profile_image_url", localUser.getProfileImageUrl() != null ? localUser.getProfileImageUrl() : "");
            userData.put("rol", rol);
            userData.put("lastLoginAt", FieldValue.serverTimestamp());
            
            // Verificar si el documento ya existe para no sobrescribir createdAt
            firestore.collection("users").document(email).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            userData.put("createdAt", FieldValue.serverTimestamp());
                        }
                        firestore.collection("users")
                                .document(email)
                                .set(userData, SetOptions.merge());
                    });
        }
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
        // Ocultar diálogo de carga si está visible
        hideLoadingDialog();
        
        // Obtener rol guardado en SharedPreferences
        SharedPreferences sessionPrefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        String userRol = sessionPrefs.getString("CURRENT_USER_ROL", "USUARIO_NORMAL");
        
        Intent intent = new Intent(LoginActivity.this, NavegacionActivity.class);
        intent.putExtra("USER_TYPE", userRol);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Asegurarse de cerrar el diálogo si la actividad se destruye
        hideLoadingDialog();
    }
}
