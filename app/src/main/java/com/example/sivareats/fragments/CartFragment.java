package com.example.sivareats.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sivareats.R;
import com.example.sivareats.adapters.CarritoAdapter;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.cart.CartItem;
import com.example.sivareats.data.cart.CartDao;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private static final String TAG = "CartFragment";

    private RecyclerView recyclerView;
    private CarritoAdapter adapter;
    private TextView tvSubtotal, tvDelivery, tvTotal, tvDescuento;
    private EditText etCodigo;
    private Button btnAplicar, btnConfirmar;

    private CartDao cartDao;
    private FirebaseFirestore db;

    private double descuentoAplicado = 0.0; // Monto del descuento aplicado
    private String codigoDescuentoAplicado = null; // Código de descuento aplicado
    private boolean codigoValido = false;

    public CartFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        recyclerView = view.findViewById(R.id.recyclerCarrito);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvDelivery = view.findViewById(R.id.tvDelivery);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvDescuento = view.findViewById(R.id.tvDescuento);
        etCodigo = view.findViewById(R.id.etCodigo);
        btnAplicar = view.findViewById(R.id.btnAplicar);
        btnConfirmar = view.findViewById(R.id.btnConfirmar);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        cartDao = AppDatabase.getInstance(requireContext()).cartDao();

        adapter = new CarritoAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnCantidadChangeListener(() -> actualizarTotales(adapter.getLista()));

        cartDao.getAllLive().observe(getViewLifecycleOwner(), new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                adapter.updateList(cartItems);

                // Cargar código de descuento guardado si existe
                cargarCodigoDescuentoGuardado();

                actualizarTotales(cartItems);
            }
        });

        // Configurar botón de aplicar código de descuento
        btnAplicar.setOnClickListener(v -> validarYApplicarCodigoDescuento());

        // 👉 AQUÍ ABRIMOS EL FRAGMENT DE ENVÍO
        btnConfirmar.setOnClickListener(v -> abrirEnvio());

        return view;
    }

    private void abrirEnvio() {
        EnvioFragment envioFragment = new EnvioFragment();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .hide(this) // Ocultar el CartFragment actual
                .add(R.id.fragment_container, envioFragment, "EnvioFragment")
                .addToBackStack(null)
                .commit();
    }

    private void validarYApplicarCodigoDescuento() {
        String codigo = etCodigo.getText().toString().trim().toUpperCase();

        if (TextUtils.isEmpty(codigo)) {
            Toast.makeText(getContext(), "Por favor ingrese un código de descuento", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deshabilitar botón mientras se valida
        btnAplicar.setEnabled(false);
        btnAplicar.setText("Validando...");

        // Buscar el código en Firebase
        db.collection("codigos_descuento")
                .document(codigo)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    btnAplicar.setEnabled(true);
                    btnAplicar.setText("Aplicar");

                    if (documentSnapshot.exists()) {
                        // Verificar si el código está activo
                        Boolean activo = documentSnapshot.getBoolean("activo");
                        if (activo != null && activo) {
                            // Código válido - aplicar descuento del 10%
                            codigoValido = true;
                            codigoDescuentoAplicado = codigo;

                            // Guardar el código de descuento en SharedPreferences
                            guardarCodigoDescuento(codigo);

                            Toast.makeText(getContext(), "¡Código de descuento aplicado! 10% de descuento", Toast.LENGTH_SHORT).show();

                            // Recalcular totales con el descuento
                            actualizarTotales(adapter.getLista());
                        } else {
                            // Código inactivo
                            codigoValido = false;
                            codigoDescuentoAplicado = null;
                            descuentoAplicado = 0.0;
                            limpiarCodigoDescuento();
                            Toast.makeText(getContext(), "El código de descuento no está activo", Toast.LENGTH_SHORT).show();
                            actualizarTotales(adapter.getLista());
                        }
                    } else {
                        // Código no existe
                        codigoValido = false;
                        codigoDescuentoAplicado = null;
                        descuentoAplicado = 0.0;
                        limpiarCodigoDescuento();
                        Toast.makeText(getContext(), "Código de descuento no válido", Toast.LENGTH_SHORT).show();
                        actualizarTotales(adapter.getLista());
                    }
                })
                .addOnFailureListener(e -> {
                    btnAplicar.setEnabled(true);
                    btnAplicar.setText("Aplicar");
                    Log.e(TAG, "Error al validar código de descuento: " + e.getMessage());
                    Toast.makeText(getContext(), "Error al validar el código. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarTotales(List<CartItem> cartItems) {
        // Calcular subtotal ANTES del descuento
        double subtotalSinDescuento = 0;

        for (CartItem p : cartItems) {
            subtotalSinDescuento += p.getPrecio() * p.getCantidad();
        }

        // Calcular descuento del 10% si hay un código válido aplicado
        double subtotalConDescuento = subtotalSinDescuento;
        if (codigoValido && subtotalSinDescuento > 0) {
            descuentoAplicado = subtotalSinDescuento * 0.10; // 10% de descuento
            subtotalConDescuento = subtotalSinDescuento - descuentoAplicado;

            // Mostrar línea de descuento
            tvDescuento.setVisibility(View.VISIBLE);
            tvDescuento.setText("Descuento por cupón (" + codigoDescuentoAplicado + "): -$" + String.format("%.2f", descuentoAplicado));
        } else {
            // Ocultar línea de descuento si no hay código aplicado
            descuentoAplicado = 0.0;
            tvDescuento.setVisibility(View.GONE);
        }

        double delivery = 1.50;
        double total = subtotalConDescuento + delivery;

        // Mostrar subtotal (después del descuento si hay uno aplicado)
        tvSubtotal.setText("Subtotal: $" + String.format("%.2f", subtotalConDescuento));
        tvDelivery.setText("Delivery: $" + String.format("%.2f", delivery));
        tvTotal.setText("Total: $" + String.format("%.2f", total));

        // Guardar el monto del descuento en SharedPreferences
        guardarDescuentoAplicado(descuentoAplicado);
    }

    private void guardarCodigoDescuento(String codigo) {
        SharedPreferences prefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("CODIGO_DESCUENTO_APLICADO", codigo);
        editor.putBoolean("CODIGO_DESCUENTO_VALIDO", true);
        editor.apply();
    }

    private void limpiarCodigoDescuento() {
        SharedPreferences prefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("CODIGO_DESCUENTO_APLICADO");
        editor.putBoolean("CODIGO_DESCUENTO_VALIDO", false);
        editor.apply();
    }

    private void guardarDescuentoAplicado(double descuento) {
        SharedPreferences prefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("DESCUENTO_APLICADO", (float) descuento);
        editor.apply();
    }

    private void cargarCodigoDescuentoGuardado() {
        SharedPreferences prefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        String codigoGuardado = prefs.getString("CODIGO_DESCUENTO_APLICADO", null);
        boolean codigoValidoGuardado = prefs.getBoolean("CODIGO_DESCUENTO_VALIDO", false);

        if (codigoGuardado != null && codigoValidoGuardado) {
            codigoDescuentoAplicado = codigoGuardado;
            codigoValido = true;
            etCodigo.setText(codigoGuardado);
            etCodigo.setEnabled(false); // Deshabilitar el campo para indicar que ya está aplicado
            btnAplicar.setText("Aplicado");
            btnAplicar.setEnabled(false);
        } else {
            codigoDescuentoAplicado = null;
            codigoValido = false;
        }
    }
}
