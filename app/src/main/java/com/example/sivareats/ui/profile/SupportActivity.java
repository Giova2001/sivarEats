package com.example.sivareats.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sivareats.R;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SupportActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etMensaje;
    private MaterialAutoCompleteTextView acAsunto;
    private Button btnEnviar;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore db;
    private static final String PREF_NAME = "loginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soporte_tecnico);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        
        etNombre = findViewById(R.id.et_nombre);
        etEmail = findViewById(R.id.et_email);
        etMensaje = findViewById(R.id.et_mensaje);
        acAsunto = findViewById(R.id.ac_asunto);
        btnEnviar = findViewById(R.id.btn_enviar);

        // Configurar dropdown de asuntos
        setupAsuntoDropdown();
        
        // Cargar datos del usuario
        loadUserData();

        btnEnviar.setOnClickListener(v -> sendSupportEmail());
    }
    
    private void setupAsuntoDropdown() {
        String[] asuntos = {
            "Bug o Error en la aplicación",
            "Problema con el pago",
            "Problema con mi pedido",
            "Problema con mi cuenta",
            "Sugerencia o mejora",
            "Problema con la entrega",
            "Otro"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, asuntos);
        acAsunto.setAdapter(adapter);
    }

    private void loadUserData() {
        // Obtener email del usuario
        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("CURRENT_USER_EMAIL", null);
        
        if (userEmail == null || userEmail.isEmpty()) {
            // Intentar obtener desde Firebase Auth
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                userEmail = currentUser.getEmail();
            } else {
                userEmail = sharedPreferences.getString("email", null);
                if (userEmail == null) {
                    userEmail = sharedPreferences.getString("last_logged_email", null);
                }
            }
        }
        
        // Hacer la variable final para usar en lambdas
        final String finalUserEmail = userEmail;
        
        // Cargar email
        if (finalUserEmail != null && etEmail != null) {
            etEmail.setText(finalUserEmail);
        }
        
        // Cargar nombre desde Firebase
        if (finalUserEmail != null && db != null) {
            db.collection("users").document(finalUserEmail)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty() && etNombre != null) {
                                etNombre.setText(name);
                            } else {
                                // Si el nombre está vacío, usar parte del email
                                if (etNombre != null) {
                                    String displayName = finalUserEmail.split("@")[0];
                                    etNombre.setText(displayName);
                                }
                            }
                        } else {
                            // Si no existe en Firestore, usar parte del email como nombre
                            if (etNombre != null) {
                                String displayName = finalUserEmail.split("@")[0];
                                etNombre.setText(displayName);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SupportActivity", "Error al cargar nombre: " + e.getMessage());
                        // En caso de error, usar parte del email como nombre
                        if (etNombre != null) {
                            String displayName = finalUserEmail.split("@")[0];
                            etNombre.setText(displayName);
                        }
                    });
        } else if (finalUserEmail != null && etNombre != null) {
            // Si no hay email de Firebase, usar parte del email como nombre
            String displayName = finalUserEmail.split("@")[0];
            etNombre.setText(displayName);
        }
    }

    private void sendSupportEmail() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String asunto = acAsunto.getText() != null ? acAsunto.getText().toString().trim() : "";
        String mensaje = etMensaje.getText() != null ? etMensaje.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) {
            Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email inválido");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(asunto)) {
            acAsunto.setError("Selecciona un asunto");
            acAsunto.requestFocus();
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
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Soporte Técnico - " + asunto);
        emailIntent.putExtra(Intent.EXTRA_TEXT, 
                "Nombre: " + nombre + "\n" +
                "Email: " + email + "\n" +
                "Asunto: " + asunto + "\n\n" +
                "Mensaje:\n" + mensaje);

        try {
            startActivity(Intent.createChooser(emailIntent, "Enviar correo usando..."));
            Toast.makeText(this, "Abre tu cliente de correo para enviar el mensaje", Toast.LENGTH_SHORT).show();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No hay cliente de correo instalado", Toast.LENGTH_SHORT).show();
        }
    }
}

