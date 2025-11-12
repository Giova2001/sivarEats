package com.example.sivareats.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sivareats.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SupportActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etMensaje;
    private Button btnEnviar;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "loginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soporte_tecnico);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        etNombre = findViewById(R.id.et_nombre);
        etEmail = findViewById(R.id.et_email);
        etMensaje = findViewById(R.id.et_mensaje);
        btnEnviar = findViewById(R.id.btn_enviar);

        // Cargar email del usuario
        loadUserEmail();

        btnEnviar.setOnClickListener(v -> sendSupportEmail());
    }

    private void loadUserEmail() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String email = null;
        
        if (currentUser != null && currentUser.getEmail() != null) {
            email = currentUser.getEmail();
        } else {
            email = sharedPreferences.getString("email", null);
            if (email == null) {
                email = sharedPreferences.getString("last_logged_email", null);
            }
        }
        
        if (email != null && etEmail != null) {
            etEmail.setText(email);
        }
    }

    private void sendSupportEmail() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String mensaje = etMensaje.getText() != null ? etMensaje.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Ingresa tu nombre");
            etNombre.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingresa un email válido");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(mensaje)) {
            etMensaje.setError("Ingresa tu mensaje");
            etMensaje.requestFocus();
            return;
        }

        // Enviar correo usando Intent
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"tecnostore141@gmail.com"}); // Cambiar por el email real
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Soporte Técnico - " + nombre);
        emailIntent.putExtra(Intent.EXTRA_TEXT, 
                "Nombre: " + nombre + "\n" +
                "Email: " + email + "\n\n" +
                "Mensaje:\n" + mensaje);

        try {
            startActivity(Intent.createChooser(emailIntent, "Enviar correo usando..."));
            Toast.makeText(this, "Abre tu cliente de correo para enviar el mensaje", Toast.LENGTH_SHORT).show();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No hay cliente de correo instalado", Toast.LENGTH_SHORT).show();
        }
    }
}

