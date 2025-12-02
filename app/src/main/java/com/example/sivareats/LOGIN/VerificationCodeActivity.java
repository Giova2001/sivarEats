package com.example.sivareats.LOGIN;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class VerificationCodeActivity extends AppCompatActivity {

    private TextInputEditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6;
    private Button btnContinue;
    private TextView tvResendCode, tvEmail;
    private String email;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private android.app.ProgressDialog progressDialog;
    private String verificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

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
            getSupportActionBar().setTitle("Código de verificación");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Vistas
        etCode1 = findViewById(R.id.etCode1);
        etCode2 = findViewById(R.id.etCode2);
        etCode3 = findViewById(R.id.etCode3);
        etCode4 = findViewById(R.id.etCode4);
        etCode5 = findViewById(R.id.etCode5);
        etCode6 = findViewById(R.id.etCode6);
        btnContinue = findViewById(R.id.btnContinue);
        tvResendCode = findViewById(R.id.tvResendCode);
        tvEmail = findViewById(R.id.tvEmail);

        // Mostrar email (enmascarado)
        if (email != null && email.contains("@")) {
            String maskedEmail = maskEmail(email);
            tvEmail.setText(maskedEmail);
        }

        // Configurar campos de código
        setupCodeInputs();

        // Botón continuar
        btnContinue.setOnClickListener(v -> {
            String code = getEnteredCode();
            if (code.length() == 6) {
                verifyCode(code);
            } else {
                Toast.makeText(this, "Ingresa el código completo", Toast.LENGTH_SHORT).show();
            }
        });

        // Reenviar código
        tvResendCode.setOnClickListener(v -> resendVerificationCode());

        // Generar y enviar código al iniciar
        generateAndSendCode();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        if (parts[0].length() <= 2) {
            return "***@" + parts[1];
        }
        return parts[0].substring(0, 2) + "****@" + parts[1];
    }

    private void setupCodeInputs() {
        TextInputEditText[] codeInputs = {etCode1, etCode2, etCode3, etCode4, etCode5, etCode6};
        
        for (int i = 0; i < codeInputs.length; i++) {
            final int currentIndex = i;
            final int nextIndex = (i < codeInputs.length - 1) ? i + 1 : -1;
            
            codeInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && nextIndex != -1) {
                        codeInputs[nextIndex].requestFocus();
                    } else if (s.length() == 1) {
                        // Último campo, ocultar teclado
                        hideKeyboard();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            codeInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    TextInputEditText currentEditText = (TextInputEditText) v;
                    Editable text = currentEditText.getText();
                    if ((text == null || text.length() == 0) && currentIndex > 0) {
                        codeInputs[currentIndex - 1].requestFocus();
                        codeInputs[currentIndex - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private String getEnteredCode() {
        return (etCode1.getText() != null ? etCode1.getText().toString() : "") +
               (etCode2.getText() != null ? etCode2.getText().toString() : "") +
               (etCode3.getText() != null ? etCode3.getText().toString() : "") +
               (etCode4.getText() != null ? etCode4.getText().toString() : "") +
               (etCode5.getText() != null ? etCode5.getText().toString() : "") +
               (etCode6.getText() != null ? etCode6.getText().toString() : "");
    }

    private void generateAndSendCode() {
        showLoadingDialog("Generando código...");
        
        // Generar código de 6 dígitos
        Random random = new Random();
        verificationCode = String.format("%06d", random.nextInt(1000000));
        
        // Guardar código en Firestore con expiración (10 minutos)
        Map<String, Object> codeData = new HashMap<>();
        codeData.put("code", verificationCode);
        codeData.put("email", email);
        codeData.put("createdAt", com.google.firebase.Timestamp.now());
        codeData.put("expiresAt", System.currentTimeMillis() + (10 * 60 * 1000)); // 10 minutos
        
        firestore.collection("password_reset_codes")
                .document(email)
                .set(codeData)
                .addOnSuccessListener(aVoid -> {
                    // Enviar código por email usando Firebase Auth
                    // Nota: Firebase Auth envía un link, pero también podemos enviar el código manualmente
                    // Por ahora, usamos sendPasswordResetEmail que enviará un link
                    // En producción, deberías usar un servicio de email o Firebase Functions
                    
                    // Enviar código por email usando Firebase Auth (para desarrollo, el código también se muestra)
                    // En producción, deberías usar Firebase Functions para enviar el código por email
                    
                    // Guardar el código para referencia (en producción, esto se enviaría por email)
                    // Por ahora, también enviamos el link estándar de Firebase
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                hideLoadingDialog();
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Revisa tu correo para el código de verificación", Toast.LENGTH_SHORT).show();
                                    // Nota: En producción, el código debe enviarse por email usando Firebase Functions
                                    // Por ahora, para pruebas, puedes ver el código en los logs de Firestore
                                } else {
                                    hideLoadingDialog();
                                    Toast.makeText(this, "Error al enviar email: " + (task.getException() != null ? task.getException().getMessage() : "Error desconocido"), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Error al generar código: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void verifyCode(String code) {
        showLoadingDialog("Verificando código...");
        
        // Verificar código en Firestore
        firestore.collection("password_reset_codes")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    hideLoadingDialog();
                    
                    if (documentSnapshot.exists()) {
                        String storedCode = documentSnapshot.getString("code");
                        Long expiresAt = documentSnapshot.getLong("expiresAt");
                        
                        // Verificar si el código ha expirado
                        if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
                            Toast.makeText(this, "El código ha expirado. Solicita uno nuevo", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Verificar si el código coincide
                        if (code.equals(storedCode)) {
                            // Código válido, marcar como verificado y ir a crear nueva contraseña
                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("verified", true);
                            updateData.put("verifiedAt", com.google.firebase.Timestamp.now());
                            
                            firestore.collection("password_reset_codes")
                                    .document(email)
                                    .update(updateData)
                                    .addOnSuccessListener(aVoid -> {
                                        hideLoadingDialog();
                                        Intent intent = new Intent(VerificationCodeActivity.this, NewPasswordActivity.class);
                                        intent.putExtra("email", email);
                                        startActivity(intent);
                                    })
                                    .addOnFailureListener(e -> {
                                        hideLoadingDialog();
                                        Toast.makeText(this, "Error al verificar código", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            hideLoadingDialog();
                            Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Código no encontrado. Solicita uno nuevo", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Error al verificar código", Toast.LENGTH_SHORT).show();
                });
    }

    private void resendVerificationCode() {
        generateAndSendCode();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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

