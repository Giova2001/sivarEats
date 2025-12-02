package com.example.sivareats.LOGIN;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sivareats.R;
import com.google.android.material.card.MaterialCardView;

public class ForgotPasswordActivity extends AppCompatActivity {

    private MaterialCardView cardEmail, cardPhone;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Configurar toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Olvidé mi contraseña");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Vistas
        cardEmail = findViewById(R.id.cardEmail);
        cardPhone = findViewById(R.id.cardPhone);
        btnContinue = findViewById(R.id.btnContinue);

        // Inicialmente seleccionar email (único método disponible)
        selectEmailMethod();

        // Configurar cards
        cardEmail.setOnClickListener(v -> selectEmailMethod());
        cardPhone.setOnClickListener(v -> {
            Toast.makeText(this, "La recuperación por teléfono no está disponible", Toast.LENGTH_SHORT).show();
        });

        // Continuar al siguiente paso
        btnContinue.setOnClickListener(v -> {
            // Ir directamente a ingresar email
            Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void selectEmailMethod() {
        cardEmail.setCardBackgroundColor(ContextCompat.getColor(this, R.color.info));
        cardEmail.setStrokeColor(ContextCompat.getColor(this, R.color.info));
        cardPhone.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        cardPhone.setAlpha(0.5f);
    }
}

