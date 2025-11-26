package com.example.sivareats.ui.checkout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.example.sivareats.data.cart.CartDao;
import com.example.sivareats.data.cart.CartItem;
import com.example.sivareats.model.Producto;
import com.example.sivareats.ui.profile.EditMetodoPagoActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SeleccionarMetodoPagoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userEmail;
    private CartDao cartDao;
    private ExecutorService executor;

    private MaterialCardView cardEfectivo;
    private ImageView checkEfectivo;
    private LinearLayout containerTarjetas;
    private MaterialCardView cardAnadirTarjeta;
    private MaterialButton btnConfirmarEnviar;

    private String metodoSeleccionado = "efectivo"; // "efectivo" o el cardId de Firebase
    private List<MaterialCardView> tarjetasViews = new ArrayList<>();
    private List<PaymentMethodFirebase> tarjetasGuardadas = new ArrayList<>();

    private static final int REQUEST_ADD_CARD = 1001;

    // Clase auxiliar para representar tarjetas de Firebase
    private static class PaymentMethodFirebase {
        String cardId;
        String cardNumber;
        String cardType;
        String expiryDate;

        PaymentMethodFirebase(String cardId, String cardNumber, String cardType, String expiryDate) {
            this.cardId = cardId;
            this.cardNumber = cardNumber;
            this.cardType = cardType;
            this.expiryDate = expiryDate;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccionar_metodo_pago);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        cartDao = AppDatabase.getInstance(this).cartDao();
        executor = Executors.newSingleThreadExecutor();

        // Obtener email del usuario
        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("CURRENT_USER_EMAIL", null);

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        // Validar que todos los views se encontraron
        if (cardAnadirTarjeta == null) {
            Log.e("SeleccionarMetodoPago", "ERROR: cardAnadirTarjeta es null!");
            Toast.makeText(this, "Error: No se encontró el botón de añadir tarjeta", Toast.LENGTH_LONG).show();
        }

        // Marcar efectivo como seleccionado por defecto
        checkEfectivo.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        // Seleccionar efectivo
        cardEfectivo.setOnClickListener(v -> seleccionarMetodo("efectivo", null));

        // Añadir nueva tarjeta
        cardAnadirTarjeta.setOnClickListener(v -> {
            Log.d("SeleccionarMetodoPago", "Click en añadir nueva tarjeta");
            Toast.makeText(this, "Abriendo formulario de tarjeta...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, EditMetodoPagoActivity.class);
            try {
                startActivityForResult(intent, REQUEST_ADD_CARD);
            } catch (Exception e) {
                Log.e("SeleccionarMetodoPago", "Error al abrir EditMetodoPagoActivity: " + e.getMessage());
                Toast.makeText(this, "Error al abrir formulario: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        // Confirmar y enviar
        btnConfirmarEnviar.setOnClickListener(v -> confirmarYEnviar());
    }

    private void seleccionarMetodo(String tipo, PaymentMethodFirebase tarjeta) {
        Log.d("SeleccionarMetodoPago", "seleccionarMetodo llamado - tipo: " + tipo + ", tarjeta: " + (tarjeta != null ? tarjeta.cardId : "null"));

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
            Log.d("SeleccionarMetodoPago", "Efectivo seleccionado");
        } else if (tarjeta != null) {
            // Buscar el card view correspondiente
            boolean encontrada = false;
            for (int i = 0; i < tarjetasViews.size(); i++) {
                if (i < tarjetasGuardadas.size() && tarjetasGuardadas.get(i).cardId.equals(tarjeta.cardId)) {
                    ImageView check = tarjetasViews.get(i).findViewById(R.id.check_tarjeta);
                    if (check != null) {
                        check.setVisibility(View.VISIBLE);
                        encontrada = true;
                        metodoSeleccionado = tarjeta.cardId;
                        Log.d("SeleccionarMetodoPago", "Tarjeta seleccionada: " + tarjeta.cardId);
                        break;
                    }
                }
            }
            if (!encontrada) {
                Log.e("SeleccionarMetodoPago", "No se encontró la tarjeta en la lista");
            }
        }
    }

    private void loadPaymentMethods() {
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }

        // Guardar el método seleccionado antes de recargar
        String metodoSeleccionadoAnterior = metodoSeleccionado;

        db.collection("users").document(userEmail)
                .collection("payment_methods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tarjetasGuardadas.clear();
                    tarjetasViews.clear();
                    containerTarjetas.removeAllViews();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d("SeleccionarMetodoPago", "Cargadas " + queryDocumentSnapshots.size() + " tarjetas desde Firebase");
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String cardId = document.getId();
                            String cardNumber = document.getString("cardNumber");
                            String cardType = document.getString("cardType");
                            String expiryDate = document.getString("expiryDate");

                            if (cardNumber != null) {
                                Log.d("SeleccionarMetodoPago", "Agregando tarjeta: " + cardId + " - " + maskCardNumber(cardNumber));
                                PaymentMethodFirebase card = new PaymentMethodFirebase(
                                        cardId, cardNumber, cardType, expiryDate);
                                tarjetasGuardadas.add(card);

                                MaterialCardView cardView = crearCardTarjeta(card);
                                containerTarjetas.addView(cardView);
                                tarjetasViews.add(cardView);
                            }
                        }
                        Log.d("SeleccionarMetodoPago", "Total de tarjetas cargadas: " + tarjetasGuardadas.size());
                    } else {
                        Log.d("SeleccionarMetodoPago", "No se encontraron tarjetas en Firebase");
                    }

                    // Restaurar la selección anterior si existe
                    if (metodoSeleccionadoAnterior != null && !metodoSeleccionadoAnterior.equals("efectivo")) {
                        // Buscar la tarjeta que estaba seleccionada
                        for (PaymentMethodFirebase card : tarjetasGuardadas) {
                            if (card.cardId.equals(metodoSeleccionadoAnterior)) {
                                seleccionarMetodo("tarjeta", card);
                                break;
                            }
                        }
                    } else if (metodoSeleccionadoAnterior != null && metodoSeleccionadoAnterior.equals("efectivo")) {
                        // Si estaba seleccionado efectivo, mantenerlo
                        seleccionarMetodo("efectivo", null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SeleccionarMetodoPago", "Error al cargar tarjetas: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar métodos de pago", Toast.LENGTH_SHORT).show();
                });
    }

    private MaterialCardView crearCardTarjeta(PaymentMethodFirebase method) {
        LayoutInflater inflater = LayoutInflater.from(this);
        MaterialCardView cardView = (MaterialCardView) inflater.inflate(
                R.layout.item_tarjeta_seleccion, containerTarjetas, false);

        TextView tvNumero = cardView.findViewById(R.id.tv_numero_tarjeta);
        ImageView checkTarjeta = cardView.findViewById(R.id.check_tarjeta);
        ImageView btnEdit = cardView.findViewById(R.id.btn_edit_tarjeta);
        ImageView btnDelete = cardView.findViewById(R.id.btn_delete_tarjeta);

        String maskedCard = maskCardNumber(method.cardNumber);
        tvNumero.setText(maskedCard);

        // Asegurar que el MaterialCardView sea clickable
        cardView.setClickable(true);
        cardView.setFocusable(true);

        // Obtener el LinearLayout de acciones para evitar que los clics se propaguen al cardView
        View layoutActions = cardView.findViewById(R.id.layout_actions);
        if (layoutActions != null) {
            layoutActions.setOnClickListener(v -> {
                // Consumir el evento para que no se propague al cardView
            });
            // También prevenir que el clic se propague en el onTouch
            layoutActions.setOnTouchListener((v, event) -> {
                // Consumir el evento touch para que no se propague
                return false; // Dejar que los hijos manejen el evento
            });
        }

        // Click listener para seleccionar esta tarjeta (solo en el área principal, no en los botones)
        cardView.setOnClickListener(v -> {
            Log.d("SeleccionarMetodoPago", "Tarjeta seleccionada: " + method.cardId);
            seleccionarMetodo("tarjeta", method);
        });

        // Listener para editar tarjeta - prevenir propagación al cardView
        btnEdit.setOnClickListener(v -> {
            // Consumir el evento para que no se propague al cardView
            Intent intent = new Intent(this, EditMetodoPagoActivity.class);
            intent.putExtra("card_id", method.cardId);
            startActivityForResult(intent, REQUEST_ADD_CARD);
        });

        // Listener para eliminar tarjeta - prevenir propagación al cardView
        btnDelete.setOnClickListener(v -> {
            // Consumir el evento para que no se propague al cardView
            mostrarDialogoEliminar(method);
        });

        return cardView;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private void mostrarDialogoEliminar(PaymentMethodFirebase method) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar tarjeta")
                .setMessage("¿Estás seguro de que deseas eliminar esta tarjeta?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    eliminarTarjeta(method);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTarjeta(PaymentMethodFirebase method) {
        if (method == null || method.cardId == null || userEmail == null) {
            Toast.makeText(this, "Error: No se puede eliminar la tarjeta", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userEmail)
                .collection("payment_methods").document(method.cardId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("SeleccionarMetodoPago", "Tarjeta eliminada: " + method.cardId);
                    Toast.makeText(this, "Tarjeta eliminada correctamente", Toast.LENGTH_SHORT).show();

                    // Si la tarjeta eliminada estaba seleccionada, cambiar a efectivo
                    if (method.cardId.equals(metodoSeleccionado)) {
                        seleccionarMetodo("efectivo", null);
                    }

                    // Recargar tarjetas
                    loadPaymentMethods();
                })
                .addOnFailureListener(e -> {
                    Log.e("SeleccionarMetodoPago", "Error al eliminar tarjeta: " + e.getMessage());
                    Toast.makeText(this, "Error al eliminar la tarjeta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmarYEnviar() {
        if (metodoSeleccionado == null) {
            Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener dirección del intent
        Intent intent = getIntent();
        String direccionTemp = intent.getStringExtra("direccion");
        final String direccion = (direccionTemp == null || direccionTemp.isEmpty())
                ? "Dirección no especificada"
                : direccionTemp;

        // Obtener coordenadas si están disponibles
        final double latitud = intent.getDoubleExtra("latitud", 0.0);
        final double longitud = intent.getDoubleExtra("longitud", 0.0);

        // Deshabilitar el botón para evitar doble clic
        btnConfirmarEnviar.setEnabled(false);
        btnConfirmarEnviar.setText("Procesando...");

        // Obtener productos del carrito y crear el pedido
        executor.execute(() -> {
            try {
                List<CartItem> cartItems = cartDao.getAll();

                if (cartItems == null || cartItems.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show();
                        btnConfirmarEnviar.setEnabled(true);
                        btnConfirmarEnviar.setText("Confirmar y enviar");
                    });
                    return;
                }

                // Convertir CartItems a Productos
                List<Producto> productos = new ArrayList<>();
                double subtotal = 0.0;
                String restaurante = "Restaurante"; // Valor por defecto

                for (CartItem item : cartItems) {
                    Producto producto = new Producto(
                            item.getNombre(),
                            item.getDescripcion(),
                            item.getImageResId(),
                            item.getPrecio(),
                            item.getCantidad()
                    );
                    productos.add(producto);
                    subtotal += item.getPrecio() * item.getCantidad();
                }

                // Calcular descuento si hay un código aplicado
                double descuento = 0.0;
                String codigoDescuento = null;
                SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                boolean codigoValido = prefs.getBoolean("CODIGO_DESCUENTO_VALIDO", false);
                if (codigoValido) {
                    codigoDescuento = prefs.getString("CODIGO_DESCUENTO_APLICADO", null);
                    if (codigoDescuento != null && subtotal > 0) {
                        descuento = subtotal * 0.10; // 10% de descuento
                        subtotal -= descuento;

                        // Limpiar el código de descuento después de usarlo
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove("CODIGO_DESCUENTO_APLICADO");
                        editor.putBoolean("CODIGO_DESCUENTO_VALIDO", false);
                        editor.remove("DESCUENTO_APLICADO");
                        editor.apply();
                    }
                }

                // Calcular total (subtotal con descuento + delivery)
                double delivery = 1.50;
                double total = subtotal + delivery;

                // Crear ID único para el pedido
                String pedidoId = "PED" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                // Determinar restaurante (usar valor por defecto o extraer si está disponible)
                // Por ahora usamos un valor genérico, pero puedes mejorarlo extrayendo del nombre
                restaurante = "Restaurante";

                // Crear mapa de datos para Firebase
                Map<String, Object> pedidoData = new HashMap<>();
                pedidoData.put("id", pedidoId);
                pedidoData.put("restaurante", restaurante);
                pedidoData.put("direccion", direccion);
                pedidoData.put("total", total);
                pedidoData.put("subtotal", subtotal);
                pedidoData.put("delivery", delivery);
                pedidoData.put("descuento", descuento);
                if (codigoDescuento != null) {
                    pedidoData.put("codigoDescuento", codigoDescuento);
                }
                pedidoData.put("estado", "activo"); // "activo" o "completado"
                pedidoData.put("fecha", FieldValue.serverTimestamp());
                pedidoData.put("tiempoEstimado", 30); // 30 minutos por defecto
                pedidoData.put("metodoPago", metodoSeleccionado);
                pedidoData.put("latitud", latitud);
                pedidoData.put("longitud", longitud);
                pedidoData.put("userId", userEmail);

                // Convertir productos a lista de mapas para Firebase
                List<Map<String, Object>> productosData = new ArrayList<>();
                for (Producto producto : productos) {
                    Map<String, Object> prodData = new HashMap<>();
                    prodData.put("nombre", producto.getNombre());
                    prodData.put("descripcion", producto.getDescripcion());
                    prodData.put("precio", producto.getPrecio());
                    prodData.put("cantidad", producto.getCantidad());
                    prodData.put("imagenResId", producto.getImagenResId());
                    productosData.add(prodData);
                }
                pedidoData.put("productos", productosData);

                // Guardar pedido en Firebase bajo la colección del usuario
                db.collection("users").document(userEmail)
                        .collection("pedidos").document(pedidoId)
                        .set(pedidoData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("SeleccionarMetodoPago", "Pedido guardado en Firebase: " + pedidoId);

                            // Limpiar el carrito después de guardar el pedido exitosamente
                            executor.execute(() -> {
                                cartDao.clearCart();
                                Log.d("SeleccionarMetodoPago", "Carrito limpiado después de confirmar pedido");

                                runOnUiThread(() -> {
                                    // Mensaje de confirmación
                                    String mensajePago;
                                    if ("efectivo".equals(metodoSeleccionado)) {
                                        mensajePago = "¡Pedido confirmado! El carrito se ha vaciado. Método de pago: Efectivo";
                                    } else {
                                        PaymentMethodFirebase tarjeta = null;
                                        for (PaymentMethodFirebase method : tarjetasGuardadas) {
                                            if (method.cardId.equals(metodoSeleccionado)) {
                                                tarjeta = method;
                                                break;
                                            }
                                        }
                                        if (tarjeta != null) {
                                            mensajePago = "¡Pedido confirmado! El carrito se ha vaciado. Método de pago: " + maskCardNumber(tarjeta.cardNumber);
                                        } else {
                                            mensajePago = "¡Pedido confirmado! El carrito se ha vaciado.";
                                        }
                                    }

                                    Toast.makeText(this, mensajePago, Toast.LENGTH_LONG).show();

                                    // Navegar al HomeFragment y cerrar esta actividad
                                    Intent navIntent = new Intent(this, com.example.sivareats.ui.NavegacionActivity.class);
                                    navIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    navIntent.putExtra("navigate_to", "home");
                                    startActivity(navIntent);
                                    finish();
                                });
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("SeleccionarMetodoPago", "Error al guardar pedido: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error al procesar el pedido: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                btnConfirmarEnviar.setEnabled(true);
                                btnConfirmarEnviar.setText("Confirmar y enviar");
                            });
                        });

            } catch (Exception e) {
                Log.e("SeleccionarMetodoPago", "Error al procesar pedido: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error al procesar el pedido", Toast.LENGTH_SHORT).show();
                    btnConfirmarEnviar.setEnabled(true);
                    btnConfirmarEnviar.setText("Confirmar y enviar");
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_CARD && resultCode == RESULT_OK) {
            // Recargar métodos de pago después de guardar una tarjeta
            Log.d("SeleccionarMetodoPago", "Tarjeta guardada, recargando métodos de pago...");
            loadPaymentMethods();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // No recargar aquí automáticamente porque ya se recarga en onActivityResult
        // Solo recargar si no hay tarjetas cargadas aún
        if (tarjetasGuardadas.isEmpty()) {
            loadPaymentMethods();
        }
    }
}