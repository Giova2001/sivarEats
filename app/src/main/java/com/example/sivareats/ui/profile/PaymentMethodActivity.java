package com.example.sivareats.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.PaymentMethod;
import com.example.sivareats.data.PaymentMethodDao;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class PaymentMethodActivity extends AppCompatActivity {

    private AppDatabase roomDb;
    private PaymentMethodDao paymentMethodDao;
    private MaterialCardView cardVisa;
    private TextView tvCardNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metodo_pago);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        roomDb = AppDatabase.getInstance(getApplicationContext());
        paymentMethodDao = roomDb.paymentMethodDao();

        cardVisa = findViewById(R.id.card_visa);
        
        // Buscar el TextView dentro del card_visa
        if (cardVisa != null) {
            View cardContent = cardVisa.getChildAt(0);
            if (cardContent != null) {
                // Buscar el TextView que muestra el número de tarjeta
                // El layout tiene un RelativeLayout con un TextView
                ViewGroup relativeLayout = (ViewGroup) cardContent;
                for (int i = 0; i < relativeLayout.getChildCount(); i++) {
                    View child = relativeLayout.getChildAt(i);
                    if (child instanceof TextView && child.getId() != R.id.btn_edit_card) {
                        tvCardNumber = (TextView) child;
                        break;
                    }
                }
            }
        }
        
        // Configurar botón de editar tarjeta
        View btnEditCard = findViewById(R.id.btn_edit_card);
        if (btnEditCard != null) {
            btnEditCard.setOnClickListener(v -> {
                // Abrir actividad de edición de tarjeta
                // TODO: Implementar EditPaymentMethodActivity
                Toast.makeText(this, "Funcionalidad de edición próximamente", Toast.LENGTH_SHORT).show();
            });
        }

        loadPaymentMethods();
    }

    private void loadPaymentMethods() {
        new Thread(() -> {
            List<PaymentMethod> methods = paymentMethodDao.getAll();
            runOnUiThread(() -> {
                if (methods != null && !methods.isEmpty()) {
                    // Mostrar el método de pago por defecto
                    PaymentMethod defaultMethod = paymentMethodDao.getDefault();
                    if (defaultMethod == null && !methods.isEmpty()) {
                        defaultMethod = methods.get(0);
                    }
                    
                    if (defaultMethod != null) {
                        // Mostrar tarjeta enmascarada
                        String maskedCard = maskCardNumber(defaultMethod.getCardNumber());
                        if (tvCardNumber != null) {
                            tvCardNumber.setText(maskedCard);
                        }
                        cardVisa.setVisibility(View.VISIBLE);
                    }
                } else {
                    // No hay métodos de pago guardados
                    cardVisa.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}

