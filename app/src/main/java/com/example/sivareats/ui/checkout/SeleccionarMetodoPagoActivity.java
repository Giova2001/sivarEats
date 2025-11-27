package com.example.sivareats.ui.checkout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
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

    private LinearLayout containerTarjetas;
    private MaterialCardView cardAnadirTarjeta;
    private MaterialButton btnConfirmarEnviar;
    private MaterialAutoCompleteTextView autoCompleteMetodoPago;
    private TextInputLayout textInputLayoutMetodoPago;

    private String metodoSeleccionado = "efectivo"; // "efectivo" o el cardId de Firebase
    private List<MaterialCardView> tarjetasViews = new ArrayList<>();
    private List<PaymentMethodFirebase> tarjetasGuardadas = new ArrayList<>();
    private List<PaymentMethodItem> metodoPagoItems = new ArrayList<>();
    private PaymentMethodAdapter adapter;

    private static final int REQUEST_ADD_CARD = 1001;
    
    // Clase para representar items del dropdown
    private static class PaymentMethodItem {
        String id; // "efectivo" o cardId
        String displayName; // "Efectivo" o "**** **** **** 1234"
        boolean isEfectivo;
        
        PaymentMethodItem(String id, String displayName, boolean isEfectivo) {
            this.id = id;
            this.displayName = displayName;
            this.isEfectivo = isEfectivo;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Adapter personalizado para el dropdown
    private class PaymentMethodAdapter extends ArrayAdapter<PaymentMethodItem> {
        PaymentMethodAdapter(Context context, List<PaymentMethodItem> items) {
            super(context, 0, items);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.item_metodo_pago_dropdown, parent, false);
            }
            
            PaymentMethodItem item = getItem(position);
            if (item != null) {
                ImageView icon = convertView.findViewById(R.id.icon_metodo);
                TextView text = convertView.findViewById(R.id.text_metodo);
                
                if (item.isEfectivo) {
                    icon.setImageResource(R.drawable.ic_cash);
                } else {
                    icon.setImageResource(R.drawable.ic_visa_mastercard);
                }
                text.setText(item.displayName);
            }
            
            return convertView;
        }
        
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }

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
        containerTarjetas = findViewById(R.id.container_tarjetas);
        cardAnadirTarjeta = findViewById(R.id.card_anadir_tarjeta);
        btnConfirmarEnviar = findViewById(R.id.btnConfirmarEnviar);
        autoCompleteMetodoPago = findViewById(R.id.autoComplete_metodo_pago);
        textInputLayoutMetodoPago = findViewById(R.id.textInputLayout_metodo_pago);

        // Validar que todos los views se encontraron
        if (cardAnadirTarjeta == null) {
            Log.e("SeleccionarMetodoPago", "ERROR: cardAnadirTarjeta es null!");
            Toast.makeText(this, "Error: No se encontró el botón de añadir tarjeta", Toast.LENGTH_LONG).show();
        }

        // Cargar método de pago seleccionado previamente, o efectivo por defecto
        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        String metodoGuardado = prefs.getString("METODO_PAGO_SELECCIONADO", "efectivo");
        metodoSeleccionado = metodoGuardado;
        
        // Inicializar adapter
        metodoPagoItems = new ArrayList<>();
        adapter = new PaymentMethodAdapter(this, metodoPagoItems);
        autoCompleteMetodoPago.setAdapter(adapter);
        
        // Listener para cuando se selecciona un método
        autoCompleteMetodoPago.setOnItemClickListener((parent, view, position, id) -> {
            PaymentMethodItem selected = adapter.getItem(position);
            if (selected != null) {
                metodoSeleccionado = selected.id;
                // Guardar selección
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("METODO_PAGO_SELECCIONADO", metodoSeleccionado);
                editor.apply();
                
                // Actualizar icono
                actualizarIconoMetodoPago(selected.isEfectivo);
                
                Log.d("SeleccionarMetodoPago", "Método seleccionado: " + selected.displayName);
            }
        });
    }
    
    private void actualizarIconoMetodoPago(boolean isEfectivo) {
        if (textInputLayoutMetodoPago != null) {
            if (isEfectivo) {
                textInputLayoutMetodoPago.setStartIconDrawable(R.drawable.ic_cash);
            } else {
                textInputLayoutMetodoPago.setStartIconDrawable(R.drawable.ic_visa_mastercard);
            }
        }
    }

    private void setupClickListeners() {
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


    private void loadPaymentMethods() {
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }

        // Obtener método seleccionado guardado
        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        String metodoGuardado = prefs.getString("METODO_PAGO_SELECCIONADO", "efectivo");
        final String metodoARestaurar = metodoGuardado;

        db.collection("users").document(userEmail)
                .collection("payment_methods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tarjetasGuardadas.clear();
                    tarjetasViews.clear();
                    metodoPagoItems.clear();
                    containerTarjetas.removeAllViews();

                    // Agregar efectivo como primera opción
                    metodoPagoItems.add(new PaymentMethodItem("efectivo", "Efectivo", true));

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

                                // Agregar al dropdown
                                String maskedCard = maskCardNumber(cardNumber);
                                metodoPagoItems.add(new PaymentMethodItem(cardId, maskedCard, false));

                                MaterialCardView cardView = crearCardTarjeta(card);
                                containerTarjetas.addView(cardView);
                                tarjetasViews.add(cardView);
                            }
                        }
                        Log.d("SeleccionarMetodoPago", "Total de tarjetas cargadas: " + tarjetasGuardadas.size());
                    } else {
                        Log.d("SeleccionarMetodoPago", "No se encontraron tarjetas en Firebase");
                    }
                    
                    // Actualizar adapter
                    adapter.notifyDataSetChanged();
                    
                    // Restaurar la selección guardada
                    seleccionarMetodoGuardado(metodoARestaurar);
                })
                .addOnFailureListener(e -> {
                    Log.e("SeleccionarMetodoPago", "Error al cargar tarjetas: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar métodos de pago", Toast.LENGTH_SHORT).show();
                    // En caso de error, asegurar que efectivo esté seleccionado
                    metodoSeleccionado = "efectivo";
                    seleccionarMetodoGuardado("efectivo");
                });
    }
    
    private void seleccionarMetodoGuardado(String metodoARestaurar) {
        // Buscar el método en la lista
        for (int i = 0; i < metodoPagoItems.size(); i++) {
            PaymentMethodItem item = metodoPagoItems.get(i);
            if (item.id.equals(metodoARestaurar)) {
                autoCompleteMetodoPago.setText(item.displayName, false);
                actualizarIconoMetodoPago(item.isEfectivo);
                metodoSeleccionado = item.id;
                Log.d("SeleccionarMetodoPago", "Método restaurado: " + item.displayName);
                return;
            }
        }
        // Si no se encuentra, usar efectivo
        if (!metodoPagoItems.isEmpty()) {
            PaymentMethodItem efectivo = metodoPagoItems.get(0);
            autoCompleteMetodoPago.setText(efectivo.displayName, false);
            actualizarIconoMetodoPago(true);
            metodoSeleccionado = "efectivo";
        }
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

        // Obtener el LinearLayout de acciones
        View layoutActions = cardView.findViewById(R.id.layout_actions);
        
        // Listener para editar tarjeta
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditMetodoPagoActivity.class);
            intent.putExtra("card_id", method.cardId);
            startActivityForResult(intent, REQUEST_ADD_CARD);
        });

        // Listener para eliminar tarjeta
        btnDelete.setOnClickListener(v -> {
            mostrarDialogoEliminar(method);
        });

        // Click listener para seleccionar esta tarjeta
        // El layout_actions ya tiene clickable="true" en el XML, lo que debería prevenir la propagación
        cardView.setOnClickListener(v -> {
            Log.d("SeleccionarMetodoPago", "Tarjeta seleccionada: " + method.cardId);
            // Actualizar el dropdown con la tarjeta seleccionada
            metodoSeleccionado = method.cardId;
            SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("METODO_PAGO_SELECCIONADO", metodoSeleccionado);
            editor.apply();
            
            // Buscar el item en el dropdown y seleccionarlo
            for (PaymentMethodItem item : metodoPagoItems) {
                if (item.id.equals(method.cardId)) {
                    autoCompleteMetodoPago.setText(item.displayName, false);
                    actualizarIconoMetodoPago(false);
                    break;
                }
            }
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
                        metodoSeleccionado = "efectivo";
                        SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("METODO_PAGO_SELECCIONADO", "efectivo");
                        editor.apply();
                        seleccionarMetodoGuardado("efectivo");
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
        // Recargar si no hay tarjetas cargadas aún
        if (metodoPagoItems.size() <= 1) { // Solo efectivo
            loadPaymentMethods();
        }
    }
}