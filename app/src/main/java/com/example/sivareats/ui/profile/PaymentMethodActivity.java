package com.example.sivareats.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sivareats.R;
import com.example.sivareats.ui.profile.EditMetodoPagoActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PaymentMethodActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userEmail;
    private LinearLayout containerTarjetas;
    private MaterialCardView cardAnadirTarjeta;

    private List<CardData> tarjetasList = new ArrayList<>();

    private static final int REQUEST_EDIT_CARD = 1001;

    private static class CardData {
        String cardId;
        String cardNumber;
        String cardType;

        CardData(String cardId, String cardNumber, String cardType) {
            this.cardId = cardId;
            this.cardNumber = cardNumber;
            this.cardType = cardType;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metodo_pago);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();

        // Obtener email del usuario
        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("CURRENT_USER_EMAIL", null);

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        containerTarjetas = findViewById(R.id.container_tarjetas);
        cardAnadirTarjeta = findViewById(R.id.card_anadir_tarjeta);

        // Validar que todos los views se encontraron
        if (cardAnadirTarjeta == null) {
            Log.e("PaymentMethodActivity", "ERROR: cardAnadirTarjeta es null!");
            Toast.makeText(this, "Error: No se encontró el botón de añadir tarjeta", Toast.LENGTH_LONG).show();
        }

        // Configurar botón para añadir tarjeta
        cardAnadirTarjeta.setOnClickListener(v -> {
            Log.d("PaymentMethodActivity", "Click en añadir nueva tarjeta");
            Toast.makeText(this, "Abriendo formulario de tarjeta...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, EditMetodoPagoActivity.class);
            try {
                startActivityForResult(intent, REQUEST_EDIT_CARD);
            } catch (Exception e) {
                Log.e("PaymentMethodActivity", "Error al abrir EditMetodoPagoActivity: " + e.getMessage());
                Toast.makeText(this, "Error al abrir formulario: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        loadPaymentMethods();
    }

    private void loadPaymentMethods() {
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }

        db.collection("users").document(userEmail)
                .collection("payment_methods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tarjetasList.clear();
                    containerTarjetas.removeAllViews();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String cardId = document.getId();
                            String cardNumber = document.getString("cardNumber");
                            String cardType = document.getString("cardType");

                            if (cardNumber != null) {
                                CardData card = new CardData(cardId, cardNumber, cardType);
                                tarjetasList.add(card);

                                MaterialCardView cardView = crearCardTarjeta(card);
                                containerTarjetas.addView(cardView);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PaymentMethodActivity", "Error al cargar tarjetas: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar métodos de pago", Toast.LENGTH_SHORT).show();
                });
    }

    private MaterialCardView crearCardTarjeta(CardData card) {
        LayoutInflater inflater = LayoutInflater.from(this);
        MaterialCardView cardView = (MaterialCardView) inflater.inflate(
                R.layout.item_tarjeta_metodo_pago, containerTarjetas, false);

        TextView tvNumero = cardView.findViewById(R.id.tv_numero_tarjeta);
        ImageView btnEdit = cardView.findViewById(R.id.btn_edit_card);
        ImageView btnDelete = cardView.findViewById(R.id.btn_delete_card);

        String maskedCard = maskCardNumber(card.cardNumber);
        tvNumero.setText(maskedCard);

        // Click listener para editar tarjeta
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditMetodoPagoActivity.class);
            intent.putExtra("card_id", card.cardId);
            startActivityForResult(intent, REQUEST_EDIT_CARD);
        });

        // Click listener para eliminar tarjeta
        btnDelete.setOnClickListener(v -> {
            mostrarDialogoEliminar(card);
        });

        return cardView;
    }

    private void mostrarDialogoEliminar(CardData card) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar tarjeta")
                .setMessage("¿Estás seguro de que deseas eliminar esta tarjeta?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    eliminarTarjeta(card);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTarjeta(CardData card) {
        if (card == null || card.cardId == null || userEmail == null) {
            Toast.makeText(this, "Error: No se puede eliminar la tarjeta", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userEmail)
                .collection("payment_methods").document(card.cardId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("PaymentMethodActivity", "Tarjeta eliminada: " + card.cardId);
                    Toast.makeText(this, "Tarjeta eliminada correctamente", Toast.LENGTH_SHORT).show();
                    // Recargar tarjetas
                    loadPaymentMethods();
                })
                .addOnFailureListener(e -> {
                    Log.e("PaymentMethodActivity", "Error al eliminar tarjeta: " + e.getMessage());
                    Toast.makeText(this, "Error al eliminar la tarjeta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_CARD && resultCode == RESULT_OK) {
            // Recargar tarjetas después de editar/añadir
            loadPaymentMethods();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar tarjetas al volver a la actividad
        loadPaymentMethods();
    }
}