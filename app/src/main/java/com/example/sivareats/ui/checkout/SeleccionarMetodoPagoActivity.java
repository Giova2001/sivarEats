package com.example.sivareats.ui.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.PaymentMethod;
import com.example.sivareats.data.PaymentMethodDao;
import com.example.sivareats.ui.profile.EditMetodoPagoActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class SeleccionarMetodoPagoActivity extends AppCompatActivity {

    private AppDatabase roomDb;
    private PaymentMethodDao paymentMethodDao;

    private MaterialCardView cardEfectivo;
    private ImageView checkEfectivo;
    private LinearLayout containerTarjetas;
    private MaterialCardView cardAnadirTarjeta;
    private MaterialButton btnConfirmarEnviar;

    private String metodoSeleccionado = "efectivo"; // "efectivo" o el ID de la tarjeta
    private List<MaterialCardView> tarjetasViews = new ArrayList<>();
    private List<PaymentMethod> tarjetasGuardadas = new ArrayList<>();

    private static final int REQUEST_ADD_CARD = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccionar_metodo_pago);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        roomDb = AppDatabase.getInstance(getApplicationContext());
        paymentMethodDao = roomDb.paymentMethodDao();

        initViews();
        setupClickListeners();
        loadPaymentMethods();
    }

    private void initViews() {
        cardEfectivo = findViewById(R.id.card_efectivo);
        checkEfectivo = findViewById(R.id.check_efectivo);
        containerTarjetas = findViewById(R.id.container_tarjetas);
        cardAnadirTarjeta = findViewById(R.id.card_anadir_tarjeta);
        btnConfirmarEnviar = findViewById(R.id.btnConfirmarEnviar);

        // Marcar efectivo como seleccionado por defecto
        checkEfectivo.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        // Seleccionar efectivo
        cardEfectivo.setOnClickListener(v -> seleccionarMetodo("efectivo", null));

        // Añadir nueva tarjeta
        cardAnadirTarjeta.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditMetodoPagoActivity.class);
            startActivityForResult(intent, REQUEST_ADD_CARD);
        });

        // Confirmar y enviar
        btnConfirmarEnviar.setOnClickListener(v -> confirmarYEnviar());
    }

    private void seleccionarMetodo(String tipo, PaymentMethod tarjeta) {
        // Ocultar todos los checks
        checkEfectivo.setVisibility(View.GONE);
        for (MaterialCardView cardView : tarjetasViews) {
            ImageView check = cardView.findViewById(R.id.check_tarjeta);
            if (check != null) {
                check.setVisibility(View.GONE);
            }
        }

        // Mostrar check del seleccionado
        if ("efectivo".equals(tipo)) {
            checkEfectivo.setVisibility(View.VISIBLE);
            metodoSeleccionado = "efectivo";
        } else if (tarjeta != null) {
            // Buscar el card view correspondiente
            for (int i = 0; i < tarjetasViews.size(); i++) {
                if (tarjetasGuardadas.get(i).getId() == tarjeta.getId()) {
                    ImageView check = tarjetasViews.get(i).findViewById(R.id.check_tarjeta);
                    if (check != null) {
                        check.setVisibility(View.VISIBLE);
                    }
                    metodoSeleccionado = String.valueOf(tarjeta.getId());
                    break;
                }
            }
        }
    }

    private void loadPaymentMethods() {
        new Thread(() -> {
            List<PaymentMethod> methods = paymentMethodDao.getAll();
            runOnUiThread(() -> {
                tarjetasGuardadas.clear();
                tarjetasViews.clear();
                containerTarjetas.removeAllViews();

                if (methods != null && !methods.isEmpty()) {
                    tarjetasGuardadas.addAll(methods);

                    for (PaymentMethod method : methods) {
                        MaterialCardView cardView = crearCardTarjeta(method);
                        containerTarjetas.addView(cardView);
                        tarjetasViews.add(cardView);
                    }
                }
            });
        }).start();
    }

    private MaterialCardView crearCardTarjeta(PaymentMethod method) {
        LayoutInflater inflater = LayoutInflater.from(this);
        MaterialCardView cardView = (MaterialCardView) inflater.inflate(
                R.layout.item_tarjeta_seleccion, containerTarjetas, false);

        TextView tvNumero = cardView.findViewById(R.id.tv_numero_tarjeta);
        ImageView checkTarjeta = cardView.findViewById(R.id.check_tarjeta);

        String maskedCard = maskCardNumber(method.getCardNumber());
        tvNumero.setText(maskedCard);

        // Click listener para seleccionar esta tarjeta
        cardView.setOnClickListener(v -> seleccionarMetodo("tarjeta", method));

        return cardView;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private void confirmarYEnviar() {
        if (metodoSeleccionado == null) {
            Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();
            return;
        }

        // Aquí puedes procesar el pedido con el método de pago seleccionado
        String mensaje;
        if ("efectivo".equals(metodoSeleccionado)) {
            mensaje = "Pedido confirmado. Método de pago: Efectivo";
        } else {
            PaymentMethod tarjeta = null;
            for (PaymentMethod method : tarjetasGuardadas) {
                if (String.valueOf(method.getId()).equals(metodoSeleccionado)) {
                    tarjeta = method;
                    break;
                }
            }
            if (tarjeta != null) {
                mensaje = "Pedido confirmado. Método de pago: " + maskCardNumber(tarjeta.getCardNumber());
            } else {
                mensaje = "Pedido confirmado";
            }
        }

        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();

        // TODO: Aquí puedes agregar la lógica para procesar el pedido
        // Por ejemplo, crear la orden en la base de datos, enviar a Firebase, etc.

        // Cerrar esta actividad y volver al fragment anterior
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_CARD) {
            // Recargar métodos de pago después de regresar de añadir tarjeta
            // (incluso si no se guardó, por si acaso se añadió una)
            loadPaymentMethods();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar métodos de pago cada vez que se vuelve a esta actividad
        // por si se añadió una tarjeta en otra pantalla
        loadPaymentMethods();
    }
}