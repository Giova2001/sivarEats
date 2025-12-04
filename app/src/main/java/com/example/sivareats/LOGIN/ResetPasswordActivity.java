package com.example.sivareats.LOGIN;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private static final String TAG = "ResetPasswordActivity";

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
        showLoadingDialog("Enviando enlace de recuperación...");
        
        Log.d(TAG, "Enviando email de recuperación a: " + email);
        
        // Usar Firebase Auth nativo para enviar el email de recuperación
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideLoadingDialog();
                    
                    if (task.isSuccessful()) {
                        // Email enviado exitosamente
                        Log.d(TAG, "Email de recuperación enviado exitosamente");
                        mostrarDialogoExito(email);
                    } else {
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : "Error desconocido";
                        Log.e(TAG, "Error al enviar email: " + errorMsg);
                        
                        // Verificar si es error de usuario no encontrado
                        if (errorMsg.contains("user-not-found") || errorMsg.contains("no user record")) {
                            Toast.makeText(this, "Este correo electrónico no está registrado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error al enviar email: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    private void mostrarDialogoExito(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Email enviado")
                .setMessage("Se ha enviado un enlace de recuperación de contraseña a:\n\n" + email + "\n\n" +
                        "Revisa tu correo electrónico y haz clic en el enlace para restablecer tu contraseña.\n\n" +
                        "Si no recibes el email, verifica tu carpeta de spam.")
                .setPositiveButton("Entendido", (dialog, which) -> {
                    // Volver a la pantalla de login
                    finish();
                })
                .setCancelable(false)
                .show();
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

