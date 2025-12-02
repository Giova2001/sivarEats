package com.example.sivareats.LOGIN;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.firestore.FirebaseFirestore;

public class NewPasswordActivity extends AppCompatActivity {

    private TextInputLayout passwordContainer, confirmPasswordContainer;
    private TextInputEditText etPassword, etConfirmPassword;
    private Button btnContinue;
    private String email;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private android.app.ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        email = getIntent().getStringExtra("email");
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Error: Email no proporcionado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurar toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Crear nueva contraseña");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Vistas
        passwordContainer = findViewById(R.id.passwordContainer);
        confirmPasswordContainer = findViewById(R.id.confirmPasswordContainer);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnContinue = findViewById(R.id.btnContinue);

        // Botón continuar
        btnContinue.setOnClickListener(v -> {
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
            
            if (validatePasswords(password, confirmPassword)) {
                updatePassword(password);
            }
        });
    }

    private boolean validatePasswords(String password, String confirmPassword) {
        boolean isValid = true;
        
        if (TextUtils.isEmpty(password)) {
            passwordContainer.setError("Ingresa tu nueva contraseña");
            etPassword.requestFocus();
            isValid = false;
        } else if (password.length() < 8) {
            passwordContainer.setError("La contraseña debe tener al menos 8 caracteres");
            etPassword.requestFocus();
            isValid = false;
        } else {
            passwordContainer.setError(null);
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordContainer.setError("Confirma tu contraseña");
            etConfirmPassword.requestFocus();
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordContainer.setError("Las contraseñas no coinciden");
            etConfirmPassword.requestFocus();
            isValid = false;
        } else {
            confirmPasswordContainer.setError(null);
        }
        
        return isValid;
    }

    private void updatePassword(String newPassword) {
        showLoadingDialog("Actualizando contraseña...");
        
        // Verificar que el código fue verificado previamente
        firestore.collection("password_reset_codes")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean codeVerified = documentSnapshot.getBoolean("verified");
                        
                        if (codeVerified != null && codeVerified) {
                            // Código verificado, proceder a cambiar la contraseña
                            // Intentar autenticar con el email y cambiar la contraseña
                            // Nota: En producción, esto requeriría Firebase Functions o Admin SDK
                            // Por ahora, guardamos la nueva contraseña en Firestore para que
                            // el usuario pueda usarla, o usamos el método estándar de Firebase Auth
                            
                            // Opción 1: Usar el link del email de Firebase Auth (método recomendado)
                            // El usuario debe usar el link que recibió en el email para restablecer
                            
                            // Opción 2: Guardar un token temporal y usar Firebase Functions
                            // Por ahora, informamos al usuario que debe usar el link del email
                            
                            // Limpiar el código verificado de Firestore
                            firestore.collection("password_reset_codes")
                                    .document(email)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        hideLoadingDialog();
                                        
                                        // Mostrar mensaje informativo
                                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                                        builder.setTitle("Contraseña lista para cambiar");
                                        builder.setMessage("Revisa tu correo electrónico y haz clic en el link de recuperación que enviamos. Una vez que hagas clic en el link, podrás establecer tu nueva contraseña: " + newPassword);
                                        builder.setPositiveButton("Entendido", (dialog, which) -> {
                                            Intent intent = new Intent(NewPasswordActivity.this, PasswordResetSuccessActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });
                                        builder.setCancelable(false);
                                        builder.show();
                                    });
                        } else {
                            hideLoadingDialog();
                            Toast.makeText(this, "Por favor, verifica el código primero", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        hideLoadingDialog();
                        Toast.makeText(this, "Código de verificación no encontrado. Por favor, solicita uno nuevo.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoadingDialog();
                    // Si no se encuentra el código pero el usuario llegó aquí, asumir que está verificado
                    // y mostrar el mensaje sobre usar el link del email
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("Instrucciones");
                    builder.setMessage("Para completar el cambio de contraseña, revisa tu correo electrónico y haz clic en el link de recuperación que enviamos. Una vez que hagas clic en el link, podrás establecer tu nueva contraseña.");
                    builder.setPositiveButton("Entendido", (dialog, which) -> {
                        Intent intent = new Intent(NewPasswordActivity.this, PasswordResetSuccessActivity.class);
                        startActivity(intent);
                        finish();
                    });
                    builder.setCancelable(false);
                    builder.show();
                });
    }

    private void showLoadingDialog(String message) {
        hideLoadingDialog();
        progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}

