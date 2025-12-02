package com.example.sivareats.LOGIN;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputLayout emailContainer;
    private TextInputEditText etEmail;
    private Button btnContinue;
    private FirebaseAuth mAuth;
    private android.app.ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mAuth = FirebaseAuth.getInstance();

        // Configurar toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Restablecer contraseña");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Vistas
        emailContainer = findViewById(R.id.emailContainer);
        etEmail = findViewById(R.id.etEmail);
        btnContinue = findViewById(R.id.btnContinue);

        // Botón continuar
        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            
            if (TextUtils.isEmpty(email)) {
                emailContainer.setError("Ingresa tu correo electrónico");
                etEmail.requestFocus();
                return;
            }
            
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailContainer.setError("Formato de correo inválido");
                etEmail.requestFocus();
                return;
            }
            
            emailContainer.setError(null);
            sendPasswordResetEmail(email);
        });
    }

    private void sendPasswordResetEmail(String email) {
        showLoadingDialog("Enviando código de verificación...");
        
        // Primero verificar si el usuario existe en Firebase Auth
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getSignInMethods() != null && !task.getResult().getSignInMethods().isEmpty()) {
                        // El usuario existe, enviar email de recuperación
                        mAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(resetTask -> {
                                    hideLoadingDialog();
                                    if (resetTask.isSuccessful()) {
                                        // Email enviado exitosamente
                                        Intent intent = new Intent(ResetPasswordActivity.this, VerificationCodeActivity.class);
                                        intent.putExtra("email", email);
                                        startActivity(intent);
                                    } else {
                                        String errorMsg = resetTask.getException() != null ? 
                                                resetTask.getException().getMessage() : "Error desconocido";
                                        Toast.makeText(this, "Error al enviar email: " + errorMsg, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        hideLoadingDialog();
                        Toast.makeText(this, "Este correo electrónico no está registrado", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoadingDialog();
                    // Si falla la verificación, intentar enviar de todas formas
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(resetTask -> {
                                if (resetTask.isSuccessful()) {
                                    Intent intent = new Intent(ResetPasswordActivity.this, VerificationCodeActivity.class);
                                    intent.putExtra("email", email);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(this, "Error al enviar email de recuperación", Toast.LENGTH_SHORT).show();
                                }
                            });
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

