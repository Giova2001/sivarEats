package com.example.sivareats.LOGIN;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.User;
import com.example.sivareats.data.UserDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class LoginRegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ImageButton btnBack;
    private FirebaseAuth mAuth;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_register);

        // Inicializar vistas
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();

        // Inicializar Room DB
        AppDatabase db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // Botón volver
        btnBack.setOnClickListener(v -> finish());

        // Botón registrar
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        // Registrar en Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        // Guardar datos del usuario en Firebase Database
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(userId)
                                .setValue(new User(name, email))
                                .addOnCompleteListener(this, dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        // Guardar localmente en Room
                                        saveUserLocal(name, email);

                                        // ✅ Todo lo que afecta la UI se ejecuta en el hilo principal
                                        runOnUiThread(() -> {
                                            Toast.makeText(LoginRegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                            clearFields();

                                            // Cerrar Activity tras 1.5 segundos
                                            etName.postDelayed(() -> finish(), 1500);
                                        });

                                    } else {
                                        runOnUiThread(() ->
                                                Toast.makeText(LoginRegisterActivity.this, "Error al guardar en Firebase", Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                });
                    } else {
                        String errorMsg = "Error en el registro";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error != null) {
                                if (error.contains("email address is already in use")) {
                                    errorMsg = "El correo ya está registrado";
                                } else if (error.contains("badly formatted")) {
                                    errorMsg = "Formato de email inválido";
                                } else {
                                    errorMsg = error;
                                }
                            }
                        }
                        Toast.makeText(LoginRegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserLocal(String name, String email) {
        new Thread(() -> {
            try {
                userDao.insertUser(new User(name, email));
                // Log para verificar que se guardó localmente
                Log.d("LoginRegister", "Usuario guardado localmente: " + email);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ✅ Función para limpiar los campos
    private void clearFields() {
        runOnUiThread(() -> {
            etName.setText("");
            etEmail.setText("");
            etPassword.setText("");
            etConfirmPassword.setText("");
            Log.d("LoginRegister", "Campos limpiados correctamente");
        });
    }
}
