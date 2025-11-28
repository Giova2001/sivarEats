package com.example.sivareats.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sivareats.R;
import com.example.sivareats.adapters.OrdenesAdapter;
import com.example.sivareats.model.Pedido;
import com.example.sivareats.model.Producto;
import com.example.sivareats.model.UserType;
import com.example.sivareats.ui.ordenes.RastreandoPedidoActivity;
import com.example.sivareats.ui.restaurant.RevisarPedidoActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerViewPedidos;
    private OrdenesAdapter adapter;
    private TabLayout tabLayout;
    private TextInputEditText etBuscar;

    private List<Pedido> pedidosActivos;
    private List<Pedido> pedidosCompletados;
    private List<Pedido> pedidosFiltrados;

    private FirebaseFirestore db;
    private String userEmail;
    private String userRole;
    private String restauranteName;
    private boolean isLoading = false; // Flag para prevenir cargas simultáneas
    private boolean isViewCreated = false; // Flag para saber si la vista ya fue creada
    private boolean isRestaurante = false; // Flag para saber si el usuario es restaurante
    private boolean isRepartidor = false; // Flag para saber si el usuario es repartidor

    private static final String TAG = "OrdersFragment";

    public OrdersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        // Inicializar vistas
        recyclerViewPedidos = view.findViewById(R.id.recyclerViewPedidos);
        tabLayout = view.findViewById(R.id.tabLayout);
        etBuscar = view.findViewById(R.id.etBuscar);

        // Configurar RecyclerView
        recyclerViewPedidos.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializar Firebase y usuario
        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("CURRENT_USER_EMAIL", null);
        userRole = prefs.getString("CURRENT_USER_ROL", null);
        
        // Verificar si el usuario es RESTAURANTE o REPARTIDOR
        UserType userType = UserType.fromString(userRole);
        isRestaurante = (userType == UserType.RESTAURANTE);
        isRepartidor = (userType == UserType.REPARTIDOR);
        
        // Si es restaurante, obtener el nombre del restaurante
        if (isRestaurante && userEmail != null) {
            cargarNombreRestaurante();
        }

        // Inicializar listas
        pedidosActivos = new ArrayList<>();
        pedidosCompletados = new ArrayList<>();
        pedidosFiltrados = new ArrayList<>(pedidosActivos);

        // Configurar adapter con lista vacía inicial (pasar también isRepartidor)
        adapter = new OrdenesAdapter(pedidosFiltrados, isRestaurante, isRepartidor);
        adapter.setOnPedidoClickListener(new OrdenesAdapter.OnPedidoClickListener() {
            @Override
            public void onRastrearClick(Pedido pedido) {
                if (isRestaurante) {
                    // Si es restaurante, abrir RevisarPedidoActivity
                    Intent intent = new Intent(getContext(), RevisarPedidoActivity.class);
                    intent.putExtra("pedido_id", pedido.getId());
                    intent.putExtra("restaurante_name", restauranteName);
                    startActivityForResult(intent, 1001);
                } else if (isRepartidor) {
                    // Si es repartidor, abrir RastreandoPedidoActivity con datos del pedido
                    Intent intent = new Intent(getContext(), RastreandoPedidoActivity.class);
                    intent.putExtra("pedido_id", pedido.getId());
                    intent.putExtra("restaurante", pedido.getRestaurante());
                    intent.putExtra("direccion", pedido.getDireccion());
                    intent.putExtra("cliente_email", pedido.getClienteEmail());
                    intent.putExtra("is_repartidor", true);
                    startActivity(intent);
                } else {
                    // Si es usuario normal, abrir RastreandoPedidoActivity
                    Intent intent = new Intent(getContext(), RastreandoPedidoActivity.class);
                    intent.putExtra("pedido_id", pedido.getId());
                    intent.putExtra("restaurante", pedido.getRestaurante());
                    intent.putExtra("repartidor_nombre", pedido.getRepartidorNombre());
                    intent.putExtra("repartidor_telefono", pedido.getRepartidorTelefono());
                    intent.putExtra("tiempo_estimado", pedido.getTiempoEstimado());
                    startActivity(intent);
                }
            }

            @Override
            public void onVerDetallesClick(Pedido pedido) {
                // Aquí puedes abrir una actividad de detalles si la tienes
                // Intent intent = new Intent(getContext(), DetallesPedidoActivity.class);
                // intent.putExtra("pedido_id", pedido.getId());
                // startActivity(intent);
            }
        });
        recyclerViewPedidos.setAdapter(adapter);

        // Configurar pestañas
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                cambiarTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Configurar búsqueda
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarPedidos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        isViewCreated = true;

        // Cargar pedidos desde Firebase solo cuando la vista está lista
        if (userEmail != null) {
            cargarPedidosDesdeFirebase();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Solo recargar pedidos si la vista ya fue creada y no se está cargando actualmente
        if (isViewCreated && userEmail != null && !isLoading) {
            cargarPedidosDesdeFirebase();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isViewCreated = false;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK) {
            // Recargar pedidos después de aceptar/rechazar
            if (isViewCreated && userEmail != null && !isLoading) {
                cargarPedidosDesdeFirebase();
            }
        }
    }

    private void cargarPedidosDesdeFirebase() {
        // Prevenir cargas simultáneas
        if (isLoading) {
            Log.d(TAG, "Ya se está cargando, ignorando nueva solicitud");
            return;
        }

        if (userEmail == null || userEmail.isEmpty()) {
            Log.e(TAG, "Usuario no identificado, no se pueden cargar pedidos");
            // Usar datos de ejemplo si no hay usuario
            inicializarDatosEjemplo();
            return;
        }

        // Si es restaurante, cargar pedidos pendientes
        if (isRestaurante) {
            if (restauranteName == null || restauranteName.isEmpty()) {
                // Esperar a que se cargue el nombre del restaurante
                cargarNombreRestaurante();
                return;
            }
            cargarPedidosRestaurante();
            return;
        }

        isLoading = true;

        // Limpiar listas antes de cargar nuevos datos
        pedidosActivos.clear();
        pedidosCompletados.clear();

        // Usar un mapa para evitar duplicados basados en el ID del pedido
        Map<String, Pedido> pedidosActivosMap = new HashMap<>();
        Map<String, Pedido> pedidosCompletadosMap = new HashMap<>();

        db.collection("users").document(userEmail)
                .collection("pedidos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isLoading = false;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Pedido pedido = convertirDocumentoAPedido(document);
                            if (pedido != null && pedido.getId() != null) {
                                // Usar el ID del pedido como clave para evitar duplicados
                                String pedidoId = pedido.getId();

                                String estado = pedido.getEstado();
                                // Para repartidores, considerar "en_camino" como activo
                                if (isRepartidor) {
                                    if ("en_camino".equals(estado)) {
                                        if (!pedidosActivosMap.containsKey(pedidoId)) {
                                            pedidosActivosMap.put(pedidoId, pedido);
                                        }
                                    } else if ("entregado".equals(estado) || "completado".equals(estado)) {
                                        if (!pedidosCompletadosMap.containsKey(pedidoId)) {
                                            pedidosCompletadosMap.put(pedidoId, pedido);
                                        }
                                    }
                                } else {
                                    // Lógica para usuarios normales
                                    if ("activo".equals(estado) || "preparacion".equals(estado) || "en_camino".equals(estado)) {
                                        if (!pedidosActivosMap.containsKey(pedidoId)) {
                                            pedidosActivosMap.put(pedidoId, pedido);
                                        }
                                    } else if ("entregado".equals(estado) || "completado".equals(estado)) {
                                        // Pedidos entregados o completados van al historial
                                        if (!pedidosCompletadosMap.containsKey(pedidoId)) {
                                            pedidosCompletadosMap.put(pedidoId, pedido);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al convertir documento a pedido: " + e.getMessage());
                        }
                    }

                    // Convertir los mapas a listas
                    pedidosActivos = new ArrayList<>(pedidosActivosMap.values());
                    pedidosCompletados = new ArrayList<>(pedidosCompletadosMap.values());

                    // Ordenar por fecha (más recientes primero)
                    pedidosActivos.sort((p1, p2) -> p2.getFecha().compareTo(p1.getFecha()));
                    pedidosCompletados.sort((p1, p2) -> p2.getFecha().compareTo(p1.getFecha()));

                    // Actualizar la vista
                    actualizarVista();

                    Log.d(TAG, "Pedidos cargados: " + pedidosActivos.size() + " activos, " + pedidosCompletados.size() + " completados");
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    Log.e(TAG, "Error al cargar pedidos: " + e.getMessage());
                    // En caso de error, usar datos de ejemplo
                    inicializarDatosEjemplo();
                    actualizarVista();
                });
    }
    
    private void cargarNombreRestaurante() {
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }
        
        db.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        restauranteName = documentSnapshot.getString("name");
                        if (restauranteName != null && !restauranteName.isEmpty()) {
                            cargarPedidosRestaurante();
                        } else {
                            Log.e(TAG, "Nombre de restaurante no encontrado");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar nombre del restaurante: " + e.getMessage());
                });
    }
    
    private void cargarPedidosRestaurante() {
        if (restauranteName == null || restauranteName.isEmpty()) {
            Log.e(TAG, "Nombre de restaurante no disponible");
            return;
        }
        
        isLoading = true;
        
        // Limpiar listas antes de cargar nuevos datos
        pedidosActivos.clear();
        pedidosCompletados.clear();
        
        // Usar un mapa para evitar duplicados
        Map<String, Pedido> pedidosPendientesMap = new HashMap<>();
        Map<String, Pedido> pedidosEnPreparacionMap = new HashMap<>();
        
        // Cargar pedidos pendientes (estado "pendiente")
        db.collection("restaurantes").document(restauranteName)
                .collection("pedidos_pendientes")
                .whereEqualTo("estado", "pendiente")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Pedido pedido = convertirDocumentoAPedidoRestaurante(document);
                            if (pedido != null && pedido.getId() != null) {
                                String pedidoId = pedido.getId();
                                if (!pedidosPendientesMap.containsKey(pedidoId)) {
                                    pedidosPendientesMap.put(pedidoId, pedido);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al convertir documento a pedido: " + e.getMessage());
                        }
                    }
                    
                    // Cargar pedidos en preparación y completados desde la colección del restaurante
                    // (donde se guarda la copia para seguimiento)
                    if (userEmail != null && !userEmail.isEmpty()) {
                        db.collection("users").document(userEmail)
                                .collection("pedidos")
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                    isLoading = false;
                                    
                                    Map<String, Pedido> pedidosCompletadosMap = new HashMap<>();
                                    
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots2) {
                                        try {
                                            Pedido pedido = convertirDocumentoAPedido(document);
                                            if (pedido != null && pedido.getId() != null) {
                                                String pedidoId = pedido.getId();
                                                String estado = pedido.getEstado();
                                                
                                                if ("preparacion".equals(estado) || "entregado_repartidor".equals(estado)) {
                                                    // Agregar a pedidos en preparación o entregados al repartidor
                                                    if (!pedidosEnPreparacionMap.containsKey(pedidoId)) {
                                                        pedidosEnPreparacionMap.put(pedidoId, pedido);
                                                    }
                                                } else if ("completado".equals(estado) || "rechazado".equals(estado) || "entregado".equals(estado)) {
                                                    // Agregar a pedidos completados (incluye entregado)
                                                    if (!pedidosCompletadosMap.containsKey(pedidoId)) {
                                                        pedidosCompletadosMap.put(pedidoId, pedido);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error al convertir documento a pedido: " + e.getMessage());
                                        }
                                    }
                                    
                                    // Combinar pedidos pendientes y en preparación en activos
                                    pedidosActivos = new ArrayList<>(pedidosPendientesMap.values());
                                    pedidosActivos.addAll(pedidosEnPreparacionMap.values());
                                    
                                    // Agregar pedidos completados
                                    pedidosCompletados = new ArrayList<>(pedidosCompletadosMap.values());
                                    
                                    // Ordenar por fecha (más recientes primero)
                                    pedidosActivos.sort((p1, p2) -> p2.getFecha().compareTo(p1.getFecha()));
                                    pedidosCompletados.sort((p1, p2) -> p2.getFecha().compareTo(p1.getFecha()));
                                    
                                    // Actualizar la vista
                                    actualizarVista();
                                    
                                    Log.d(TAG, "Pedidos restaurante cargados: " + pedidosActivos.size() + " activos, " + pedidosCompletados.size() + " completados");
                                })
                                .addOnFailureListener(e -> {
                                    isLoading = false;
                                    Log.e(TAG, "Error al cargar pedidos del restaurante: " + e.getMessage());
                                    actualizarVista();
                                });
                    } else {
                        isLoading = false;
                        actualizarVista();
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    Log.e(TAG, "Error al cargar pedidos pendientes: " + e.getMessage());
                    actualizarVista();
                });
    }
    
    private Pedido convertirDocumentoAPedidoRestaurante(QueryDocumentSnapshot document) {
        // Similar a convertirDocumentoAPedido pero adaptado para pedidos de restaurante
        return convertirDocumentoAPedido(document);
    }

    private Pedido convertirDocumentoAPedido(QueryDocumentSnapshot document) {
        try {
            String id = document.getString("id");
            if (id == null) id = document.getId();

            String restaurante = document.getString("restaurante");
            if (restaurante == null) restaurante = "Restaurante";

            String direccion = document.getString("direccion");
            if (direccion == null) direccion = "Dirección no especificada";

            Double totalDouble = document.getDouble("total");
            double total = totalDouble != null ? totalDouble : 0.0;

            String estado = document.getString("estado");
            if (estado == null) estado = "activo";

            // Obtener fecha
            Date fecha;
            if (document.getTimestamp("fecha") != null) {
                fecha = document.getTimestamp("fecha").toDate();
            } else {
                fecha = new Date();
            }

            // Obtener productos
            List<Producto> productos = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productosData = (List<Map<String, Object>>) document.get("productos");
            if (productosData != null) {
                for (Map<String, Object> prodData : productosData) {
                    String nombre = (String) prodData.get("nombre");
                    String descripcion = (String) prodData.get("descripcion");
                    Double precio = ((Number) prodData.get("precio")).doubleValue();
                    Long cantidadLong = ((Number) prodData.get("cantidad")).longValue();
                    int cantidad = cantidadLong != null ? cantidadLong.intValue() : 1;
                    Long imagenResIdLong = prodData.get("imagenResId") != null ? ((Number) prodData.get("imagenResId")).longValue() : null;
                    int imagenResId = imagenResIdLong != null ? imagenResIdLong.intValue() : R.drawable.ic_profile;

                    if (nombre != null && precio != null) {
                        Producto producto = new Producto(nombre, descripcion != null ? descripcion : "", imagenResId, precio, cantidad);
                        productos.add(producto);
                    }
                }
            }

            Pedido pedido = new Pedido(id, restaurante, direccion, total, estado, fecha, productos);

            // Obtener tiempo estimado
            Long tiempoEstimadoLong = document.getLong("tiempoEstimado");
            if (tiempoEstimadoLong != null) {
                pedido.setTiempoEstimado(tiempoEstimadoLong.intValue());
            } else {
                pedido.setTiempoEstimado(30); // Valor por defecto
            }

            // Obtener información del repartidor si está disponible
            String repartidorNombre = document.getString("repartidorNombre");
            String repartidorTelefono = document.getString("repartidorTelefono");
            if (repartidorNombre != null) pedido.setRepartidorNombre(repartidorNombre);
            if (repartidorTelefono != null) pedido.setRepartidorTelefono(repartidorTelefono);
            
            // Obtener email del cliente si está disponible (para pedidos de restaurante)
            String clienteEmail = document.getString("clienteEmail");
            if (clienteEmail != null) pedido.setClienteEmail(clienteEmail);

            return pedido;
        } catch (Exception e) {
            Log.e(TAG, "Error al convertir documento: " + e.getMessage());
            return null;
        }
    }

    private void actualizarVista() {
        if (adapter != null && tabLayout != null) {
            int selectedTab = tabLayout.getSelectedTabPosition();
            // Si no hay tab seleccionado o la posición es -1, usar el primero (En curso)
            if (selectedTab == -1 || selectedTab < 0) {
                selectedTab = 0;
            }
            cambiarTab(selectedTab);
        }
    }

    private void inicializarDatosEjemplo() {
        pedidosActivos = new ArrayList<>();
        pedidosCompletados = new ArrayList<>();

        // Pedido activo de ejemplo
        List<Producto> productos1 = new ArrayList<>();
        productos1.add(new Producto("Hamburguesa Doble", "Hamburguesa con doble carne", R.drawable.ic_profile, 12.50, 1));
        productos1.add(new Producto("Papas Fritas", "Papas fritas grandes", R.drawable.ic_profile, 5.00, 1));

        Pedido pedido1 = new Pedido("PED001", "Burger King - Santa Ana",
                "Calle Principal #123, San Salvador", 17.50, "activo", new Date(), productos1);
        pedido1.setRepartidorNombre("Samuel Rodriguez");
        pedido1.setRepartidorTelefono("70204560");
        pedido1.setTiempoEstimado(30);
        pedidosActivos.add(pedido1);

        // Otro pedido activo
        List<Producto> productos2 = new ArrayList<>();
        productos2.add(new Producto("Pizza Familiar", "Pizza grande con todos los ingredientes", R.drawable.ic_profile, 18.00, 1));

        Pedido pedido2 = new Pedido("PED002", "Pizza Hut - Metrocentro",
                "Boulevard de los Héroes, San Salvador", 18.00, "activo", new Date(), productos2);
        pedido2.setRepartidorNombre("Carlos Méndez");
        pedido2.setRepartidorTelefono("22345678");
        pedido2.setTiempoEstimado(25);
        pedidosActivos.add(pedido2);

        // Pedidos completados de ejemplo
        List<Producto> productos3 = new ArrayList<>();
        productos3.add(new Producto("Combo Super Campero", "Tres piezas de pollo más ensalada", R.drawable.ic_profile, 7.50, 1));

        Pedido pedido3 = new Pedido("PED003", "Pollo Campero - Centro",
                "Avenida España #456, San Salvador", 7.50, "completado", new Date(), productos3);
        pedidosCompletados.add(pedido3);

        pedidosFiltrados = new ArrayList<>(pedidosActivos);
    }

    private void cambiarTab(int position) {
        // Asegurar que las listas base no sean null
        if (pedidosActivos == null) {
            pedidosActivos = new ArrayList<>();
        }
        if (pedidosCompletados == null) {
            pedidosCompletados = new ArrayList<>();
        }

        if (position == 0) {
            // Tab "En curso"
            pedidosFiltrados = new ArrayList<>(pedidosActivos);
        } else {
            // Tab "Historial"
            pedidosFiltrados = new ArrayList<>(pedidosCompletados);
        }

        // Si no hay pedidos, inicializar lista vacía
        if (pedidosFiltrados == null) {
            pedidosFiltrados = new ArrayList<>();
        }

        filtrarPedidos(etBuscar != null && etBuscar.getText() != null ? etBuscar.getText().toString() : "");
    }

    private void filtrarPedidos(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.updateList(pedidosFiltrados);
        } else {
            List<Pedido> filtrados = new ArrayList<>();
            String queryLower = query.toLowerCase(Locale.getDefault());
            for (Pedido pedido : pedidosFiltrados) {
                if (pedido.getRestaurante().toLowerCase(Locale.getDefault()).contains(queryLower) ||
                        pedido.getId().toLowerCase(Locale.getDefault()).contains(queryLower) ||
                        pedido.getDireccion().toLowerCase(Locale.getDefault()).contains(queryLower)) {
                    filtrados.add(pedido);
                }
            }
            adapter.updateList(filtrados);
        }
    }
}