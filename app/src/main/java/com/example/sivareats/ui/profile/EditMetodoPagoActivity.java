package com.example.sivareats.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sivareats.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditMetodoPagoActivity extends AppCompatActivity {

    private EditText etCardNumber;
    private EditText etExpiryDate;
    private EditText etCvc;
    private Button btnGuardar;
    private MaterialButton btnEliminar;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private String userEmail;
    private String cardId; // Si viene del intent, significa que estamos editando
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_metodo_pago);

        db = FirebaseFirestore.getInstance();

        // Obtener email del usuario
        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("CURRENT_USER_EMAIL", null);

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Verificar si estamos editando una tarjeta existente
        cardId = getIntent().getStringExtra("card_id");
        if (cardId != null && !cardId.isEmpty()) {
            isEditing = true;
        }

        initViews();
        setupToolbar();

        if (isEditing) {
            loadCardData();
            btnEliminar.setVisibility(android.view.View.VISIBLE);
        } else {
            btnEliminar.setVisibility(android.view.View.GONE);
        }

        btnGuardar.setOnClickListener(v -> guardarTarjeta());
        btnEliminar.setOnClickListener(v -> eliminarTarjeta());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etCardNumber = findViewById(R.id.et_card_number);
        etExpiryDate = findViewById(R.id.et_expiry_date);
        etCvc = findViewById(R.id.et_cvc);
        btnGuardar = findViewById(R.id.btn_guardar);
        btnEliminar = findViewById(R.id.btn_eliminar);

        // Limpiar valores por defecto
        etCardNumber.setText("");
        etExpiryDate.setText("");
        etCvc.setText("");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditing ? "Editar tarjeta" : "Añadir tarjeta");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadCardData() {
        if (cardId == null || userEmail == null) return;

        db.collection("users").document(userEmail)
                .collection("payment_methods").document(cardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etCardNumber.setText(documentSnapshot.getString("cardNumber"));
                        etExpiryDate.setText(documentSnapshot.getString("expiryDate"));
                        etCvc.setText(documentSnapshot.getString("cvv"));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar la tarjeta", Toast.LENGTH_SHORT).show();
                });
    }

    private void guardarTarjeta() {
        String cardNumber = etCardNumber.getText().toString().trim();
        String expiryDate = etExpiryDate.getText().toString().trim();
        String cvv = etCvc.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(cardNumber)) {
            etCardNumber.setError("Ingresa el número de tarjeta");
            etCardNumber.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(expiryDate)) {
            etExpiryDate.setError("Ingresa la fecha de expiración");
            etExpiryDate.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(cvv) || cvv.length() != 3) {
            etCvc.setError("Ingresa un CVC válido (3 dígitos)");
            etCvc.requestFocus();
            return;
        }

        // Determinar tipo de tarjeta (simplificado)
        String cardType = determinarTipoTarjeta(cardNumber);

        // Crear mapa de datos
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("cardNumber", cardNumber);
        cardData.put("expiryDate", expiryDate);
        cardData.put("cvv", cvv);
        cardData.put("cardType", cardType);
        cardData.put("isDefault", false);
        cardData.put("cardHolder", ""); // Se puede agregar después

        // Guardar en Firebase
        if (isEditing && cardId != null) {
            // Actualizar tarjeta existente
            db.collection("users").document(userEmail)
                    .collection("payment_methods").document(cardId)
                    .update(cardData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Tarjeta actualizada correctamente", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al actualizar la tarjeta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Crear nueva tarjeta
            String newCardId = UUID.randomUUID().toString();
            Log.d("EditMetodoPago", "Guardando tarjeta en Firebase con ID: " + newCardId);
            Log.d("EditMetodoPago", "Usuario: " + userEmail);
            Log.d("EditMetodoPago", "Datos de tarjeta: " + cardData.toString());

            db.collection("users").document(userEmail)
                    .collection("payment_methods").document(newCardId)
                    .set(cardData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("EditMetodoPago", "Tarjeta guardada exitosamente en Firebase");
                        Toast.makeText(this, "Tarjeta guardada correctamente", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EditMetodoPago", "Error al guardar tarjeta en Firebase: " + e.getMessage());
                        e.printStackTrace();
                        Toast.makeText(this, "Error al guardar la tarjeta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void eliminarTarjeta() {
        if (cardId == null || userEmail == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar tarjeta")
                .setMessage("¿Estás seguro de que deseas eliminar esta tarjeta?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.collection("users").document(userEmail)
                            .collection("payment_methods").document(cardId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Tarjeta eliminada correctamente", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al eliminar la tarjeta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String determinarTipoTarjeta(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "Visa";
        }
        // Remover espacios
        String cleanNumber = cardNumber.replaceAll("\\s+", "");
        if (cleanNumber.startsWith("4")) {
            return "Visa";
        } else if (cleanNumber.startsWith("5") || cleanNumber.startsWith("2")) {
            return "Mastercard";
        } else if (cleanNumber.startsWith("3")) {
            return "American Express";
        }
        return "Visa"; // Por defecto
    }
}