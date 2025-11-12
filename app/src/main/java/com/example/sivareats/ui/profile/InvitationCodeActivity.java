package com.example.sivareats.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sivareats.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class InvitationCodeActivity extends AppCompatActivity {

    private TextInputEditText etInvitationCode;
    private Button btnRegistrarCodigo;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codigo_invitacion);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        etInvitationCode = findViewById(R.id.et_invitation_code);
        btnRegistrarCodigo = findViewById(R.id.btn_registrar_codigo);

        btnRegistrarCodigo.setOnClickListener(v -> validateAndRegisterCode());
    }

    private void validateAndRegisterCode() {
        String code = etInvitationCode.getText() != null ? 
                etInvitationCode.getText().toString().trim().toUpperCase() : "";

        if (TextUtils.isEmpty(code)) {
            etInvitationCode.setError("Ingresa un código de invitación");
            etInvitationCode.requestFocus();
            return;
        }

        btnRegistrarCodigo.setEnabled(false);
        btnRegistrarCodigo.setText("Validando...");

        // Validar en Firebase que existe un cupón activo
        db.collection("cupones")
                .document(code)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Verificar que el cupón esté activo
                        Boolean isActive = document.getBoolean("activo");
                        Long fechaExpiracion = document.getLong("fecha_expiracion");
                        
                        if (isActive != null && isActive) {
                            // Verificar fecha de expiración si existe
                            if (fechaExpiracion != null && fechaExpiracion < System.currentTimeMillis()) {
                                Toast.makeText(this, "El código ha expirado", Toast.LENGTH_LONG).show();
                            } else {
                                // Código válido
                                String beneficio = document.getString("beneficio");
                                Toast.makeText(this, 
                                        "Código registrado exitosamente: " + (beneficio != null ? beneficio : "Cupón activo"), 
                                        Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(this, "El código no está activo", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Código de invitación no válido", Toast.LENGTH_LONG).show();
                    }
                    btnRegistrarCodigo.setEnabled(true);
                    btnRegistrarCodigo.setText("Registrar código");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al validar el código: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnRegistrarCodigo.setEnabled(true);
                    btnRegistrarCodigo.setText("Registrar código");
                });
    }
}

