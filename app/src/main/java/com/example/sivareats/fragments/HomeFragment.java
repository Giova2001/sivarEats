package com.example.sivareats.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.sivareats.R;
import com.example.sivareats.model.CartViewModel;
import com.example.sivareats.model.Producto;
import com.example.sivareats.utils.SearchHistoryManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private SearchHistoryManager searchHistoryManager;

    private FrameLayout overlayBusqueda;
    private LinearLayout overlayHistorial;
    private CartViewModel cartViewModel;

    // Botones de categorías
    private MaterialButton btnRestaurantes;
    private MaterialButton btnFavoritos;
    private MaterialButton btnChina;
    private MaterialButton btnPizza;

    // Contenedores de productos
    private LinearLayout layoutOfertas;
    private LinearLayout layoutRecomendados;
    private LinearLayout layoutTodo;
    private LinearLayout layoutRestaurantesAgrupados;

    // Referencias a las secciones principales
    private View btnOfertasDelDia;
    private View scrollOfertas;
    private View btnRecomendados;
    private View scrollRecomendados;
    private View btnTodo;

    // Categoría seleccionada
    private String categoriaSeleccionada = "todos"; // "todos", "restaurantes", "favoritos", "china", "pizza"

    // Listas completas de productos (sin filtrar)
    private List<Producto> todasLasOfertas;
    private List<Producto> todosLosRecomendados;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            // Referencias
            layoutOfertas = view.findViewById(R.id.layoutOfertas);
            layoutRecomendados = view.findViewById(R.id.layoutRecomendados);
            layoutTodo = view.findViewById(R.id.layoutTodo);
            layoutRestaurantesAgrupados = view.findViewById(R.id.layoutRestaurantesAgrupados);
            btnOfertasDelDia = view.findViewById(R.id.btnOfertasDelDia);
            scrollOfertas = view.findViewById(R.id.scrollOfertas);
            btnRecomendados = view.findViewById(R.id.btnRecomendados);
            scrollRecomendados = view.findViewById(R.id.scrollRecomendados);
            btnTodo = view.findViewById(R.id.btnTodo);
            EditText barraBusqueda = view.findViewById(R.id.etBuscar);

            // Botones de categorías
            btnRestaurantes = view.findViewById(R.id.btnCategoriaRestaurantes);
            btnFavoritos = view.findViewById(R.id.btnCategoriaFavoritos);
            btnChina = view.findViewById(R.id.btnCategoriaChina);
            btnPizza = view.findViewById(R.id.btnCategoriaPizza);

            // Overlay
            overlayBusqueda = view.findViewById(R.id.overlayBusqueda);
            overlayHistorial = view.findViewById(R.id.layoutOverlayHistorial);

            searchHistoryManager = new SearchHistoryManager(requireContext());

            // Mostrar overlay
            barraBusqueda.setOnClickListener(v -> mostrarOverlay(inflater));

            // Guardar historial al presionar enter
            barraBusqueda.setOnEditorActionListener((v, actionId, event) -> {
                String texto = barraBusqueda.getText().toString().trim();

                if (!texto.isEmpty()) {
                    searchHistoryManager.saveSearch(texto);
                    mostrarOverlay(inflater);
                    Toast.makeText(getContext(), "Buscando: " + texto, Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            // Ocultar overlay al presionar fuera
            overlayBusqueda.setOnClickListener(v -> overlayBusqueda.setVisibility(View.GONE));

            // Configurar listeners de botones de categorías
            setupCategoriaButtons(inflater);

            // Cargar todas las listas de productos
            todasLasOfertas = obtenerTodasLasOfertas();
            todosLosRecomendados = obtenerTodosLosRecomendados();

            // Mostrar productos iniciales (todos)
            actualizarProductos(inflater);

            // Mostrar restaurantes en sección Todo
            mostrarRestaurantes(inflater);

            return view;

        } catch (Exception e) {
            Log.e(TAG, "Error en onCreateView: ", e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return view;
        }
    }


    private void mostrarOverlay(LayoutInflater inflater) {
        overlayBusqueda.setVisibility(View.VISIBLE);
        overlayHistorial.removeAllViews();

        List<String> historial = searchHistoryManager.getHistory();

        if (historial.isEmpty()) {
            TextView t = new TextView(getContext());
            t.setText("Sin búsquedas recientes");
            t.setTextSize(16);
            overlayHistorial.addView(t);
            return;
        }

        for (String h : historial) {
            View row = inflater.inflate(R.layout.item_historial_busqueda, overlayHistorial, false);
            TextView txt = row.findViewById(R.id.txtHistorialItem);
            txt.setText(h);

            row.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Buscando: " + h, Toast.LENGTH_SHORT).show();
            });

            overlayHistorial.addView(row);
        }
    }

    private void setupCategoriaButtons(LayoutInflater inflater) {
        // Listener para Restaurantes
        btnRestaurantes.setOnClickListener(v -> {
            seleccionarCategoria("restaurantes", inflater);
        });

        // Listener para Favoritos
        btnFavoritos.setOnClickListener(v -> {
            seleccionarCategoria("favoritos", inflater);
        });

        // Listener para China
        btnChina.setOnClickListener(v -> {
            seleccionarCategoria("china", inflater);
        });

        // Listener para Pizza
        btnPizza.setOnClickListener(v -> {
            seleccionarCategoria("pizza", inflater);
        });
    }

    private void seleccionarCategoria(String categoria, LayoutInflater inflater) {
        categoriaSeleccionada = categoria;
        actualizarEstadoBotones();

        // Si se selecciona "restaurantes", mostrar vista agrupada
        if ("restaurantes".equals(categoria)) {
            mostrarVistaRestaurantes(inflater);
        } else {
            mostrarVistaNormal(inflater);
            actualizarProductos(inflater);
        }
    }

    private void mostrarVistaNormal(LayoutInflater inflater) {
        // Mostrar secciones normales
        btnOfertasDelDia.setVisibility(View.VISIBLE);
        scrollOfertas.setVisibility(View.VISIBLE);
        btnRecomendados.setVisibility(View.VISIBLE);
        scrollRecomendados.setVisibility(View.VISIBLE);
        btnTodo.setVisibility(View.VISIBLE);
        layoutTodo.setVisibility(View.GONE);

        // Ocultar vista de restaurantes agrupados
        layoutRestaurantesAgrupados.setVisibility(View.GONE);
    }

    private void mostrarVistaRestaurantes(LayoutInflater inflater) {
        // Ocultar secciones normales
        btnOfertasDelDia.setVisibility(View.GONE);
        scrollOfertas.setVisibility(View.GONE);
        btnRecomendados.setVisibility(View.GONE);
        scrollRecomendados.setVisibility(View.GONE);
        btnTodo.setVisibility(View.GONE);
        layoutTodo.setVisibility(View.GONE);

        // Mostrar vista de restaurantes agrupados
        layoutRestaurantesAgrupados.setVisibility(View.VISIBLE);
        mostrarRestaurantesAgrupados(inflater);
    }

    private void actualizarEstadoBotones() {
        // Resetear todos los botones al estado normal
        resetearBoton(btnRestaurantes);
        resetearBoton(btnFavoritos);
        resetearBoton(btnChina);
        resetearBoton(btnPizza);

        // Activar el botón seleccionado
        switch (categoriaSeleccionada) {
            case "restaurantes":
                activarBoton(btnRestaurantes);
                break;
            case "favoritos":
                activarBoton(btnFavoritos);
                break;
            case "china":
                activarBoton(btnChina);
                break;
            case "pizza":
                activarBoton(btnPizza);
                break;
            default:
                // "todos" - todos los botones en estado normal
                break;
        }
    }

    private void activarBoton(MaterialButton boton) {
        boton.setBackgroundTintList(null);
        boton.setBackgroundColor(0xFF1F41EC); // Azul #1F41EC
        boton.setTextColor(0xFFFFFFFF); // Blanco
        // Cambiar color del icono a blanco
        android.content.res.ColorStateList whiteColor = android.content.res.ColorStateList.valueOf(0xFFFFFFFF);
        boton.setIconTint(whiteColor);
    }

    private void resetearBoton(MaterialButton boton) {
        boton.setBackgroundTintList(null);
        boton.setBackgroundColor(0xFFF0F0F0); // Gris claro
        boton.setTextColor(0xFF000000); // Negro
        // Cambiar color del icono a negro
        android.content.res.ColorStateList blackColor = android.content.res.ColorStateList.valueOf(0xFF000000);
        boton.setIconTint(blackColor);
    }

    private void actualizarProductos(LayoutInflater inflater) {
        // Limpiar contenedores
        layoutOfertas.removeAllViews();
        layoutRecomendados.removeAllViews();

        // Filtrar productos según la categoría seleccionada
        List<Producto> ofertasFiltradas = filtrarPorCategoria(todasLasOfertas);
        List<Producto> recomendadosFiltrados = filtrarPorCategoria(todosLosRecomendados);

        // Agregar productos filtrados
        agregarProductos(layoutOfertas, ofertasFiltradas, inflater);
        agregarProductos(layoutRecomendados, recomendadosFiltrados, inflater);
    }

    private void mostrarRestaurantes(LayoutInflater inflater) {
        layoutTodo.removeAllViews();

        // Obtener todos los productos para agrupar por restaurante
        List<Producto> todosLosProductos = new ArrayList<>();
        todosLosProductos.addAll(todasLasOfertas);
        todosLosProductos.addAll(todosLosRecomendados);

        // Agrupar por restaurante
        java.util.Map<String, List<Producto>> restaurantesMap = new java.util.HashMap<>();
        for (Producto producto : todosLosProductos) {
            String restaurante = producto.getRestaurante();
            if (restaurante != null && !restaurante.isEmpty()) {
                if (!restaurantesMap.containsKey(restaurante)) {
                    restaurantesMap.put(restaurante, new ArrayList<>());
                }
                restaurantesMap.get(restaurante).add(producto);
            }
        }

        // Crear cardviews para cada restaurante
        for (String nombreRestaurante : restaurantesMap.keySet()) {
            List<Producto> productosRestaurante = restaurantesMap.get(nombreRestaurante);
            if (productosRestaurante != null && !productosRestaurante.isEmpty()) {
                View restauranteView = crearCardRestaurante(nombreRestaurante, productosRestaurante, inflater);
                layoutTodo.addView(restauranteView);
            }
        }
    }

    private void mostrarRestaurantesAgrupados(LayoutInflater inflater) {
        layoutRestaurantesAgrupados.removeAllViews();

        // Obtener todos los productos
        List<Producto> todosLosProductos = new ArrayList<>();
        todosLosProductos.addAll(todasLasOfertas);
        todosLosProductos.addAll(todosLosRecomendados);

        // Agrupar por restaurante
        java.util.Map<String, List<Producto>> restaurantesMap = new java.util.HashMap<>();
        for (Producto producto : todosLosProductos) {
            String restaurante = producto.getRestaurante();
            if (restaurante != null && !restaurante.isEmpty()) {
                if (!restaurantesMap.containsKey(restaurante)) {
                    restaurantesMap.put(restaurante, new ArrayList<>());
                }
                restaurantesMap.get(restaurante).add(producto);
            }
        }

        // Orden de restaurantes específico
        String[] ordenRestaurantes = {"Pollo Campero", "Pizza Hut", "China Wok", "Burger King"};

        // Crear sección para cada restaurante en el orden especificado
        for (String nombreRestaurante : ordenRestaurantes) {
            if (restaurantesMap.containsKey(nombreRestaurante)) {
                List<Producto> productosRestaurante = restaurantesMap.get(nombreRestaurante);
                if (productosRestaurante != null && !productosRestaurante.isEmpty()) {
                    // Crear contenedor para el restaurante
                    View restauranteSection = crearSeccionRestaurante(nombreRestaurante, productosRestaurante, inflater);
                    layoutRestaurantesAgrupados.addView(restauranteSection);
                }
            }
        }
    }

    private View crearSeccionRestaurante(String nombreRestaurante, List<Producto> productos, LayoutInflater inflater) {
        // Crear contenedor principal para el restaurante
        LinearLayout contenedorRestaurante = new LinearLayout(getContext());
        contenedorRestaurante.setOrientation(LinearLayout.VERTICAL);
        contenedorRestaurante.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        contenedorRestaurante.setPadding(0, 0, 0, 24);

        // Título del restaurante
        TextView tituloRestaurante = new TextView(getContext());
        tituloRestaurante.setText(nombreRestaurante);
        tituloRestaurante.setTextSize(22);
        tituloRestaurante.setTypeface(null, android.graphics.Typeface.BOLD);
        tituloRestaurante.setTextColor(0xFF000000);
        tituloRestaurante.setPadding(8, 16, 8, 12);
        contenedorRestaurante.addView(tituloRestaurante);

        // Contenedor horizontal para los productos
        HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        scrollView.setHorizontalScrollBarEnabled(false);

        LinearLayout layoutProductos = new LinearLayout(getContext());
        layoutProductos.setOrientation(LinearLayout.HORIZONTAL);
        layoutProductos.setPadding(4, 0, 4, 0);

        // Agregar productos
        agregarProductos(layoutProductos, productos, inflater);

        scrollView.addView(layoutProductos);
        contenedorRestaurante.addView(scrollView);

        return contenedorRestaurante;
    }

    private View crearCardRestaurante(String nombreRestaurante, List<Producto> productos, LayoutInflater inflater) {
        View cardView = inflater.inflate(R.layout.item_restaurante, layoutTodo, false);

        ImageView imgRestaurante = cardView.findViewById(R.id.imgRestaurante);
        TextView tvNombreRestaurante = cardView.findViewById(R.id.tvNombreRestaurante);
        TextView tvCategoriaRestaurante = cardView.findViewById(R.id.tvCategoriaRestaurante);
        TextView tvProductosDestacados = cardView.findViewById(R.id.tvProductosDestacados);

        // Asignar imagen según el restaurante
        int imagenResId = obtenerImagenRestaurante(nombreRestaurante);
        imgRestaurante.setImageResource(imagenResId);

        tvNombreRestaurante.setText(nombreRestaurante);

        // Determinar categoría del restaurante
        String categoria = productos.get(0).getCategoria();
        String categoriaTexto = obtenerCategoriaTexto(categoria);
        tvCategoriaRestaurante.setText(categoriaTexto);

        // Mostrar productos destacados (máximo 3)
        StringBuilder productosText = new StringBuilder();
        int maxProductos = Math.min(3, productos.size());
        for (int i = 0; i < maxProductos; i++) {
            if (i > 0) productosText.append(", ");
            productosText.append(productos.get(i).getNombre());
        }
        if (productos.size() > maxProductos) {
            productosText.append("...");
        }
        tvProductosDestacados.setText(productosText.toString());

        return cardView;
    }

    private int obtenerImagenRestaurante(String nombreRestaurante) {
        if (nombreRestaurante.contains("Pollo Campero") || nombreRestaurante.contains("Campero")) {
            return R.drawable.campero1;
        } else if (nombreRestaurante.contains("Pizza Hut") || nombreRestaurante.contains("Pizza")) {
            return R.drawable.pizza1;
        } else if (nombreRestaurante.contains("Burger King") || nombreRestaurante.contains("Burger")) {
            return R.drawable.hamburguesa1;
        } else if (nombreRestaurante.contains("China Wok") || nombreRestaurante.contains("China")) {
            return R.drawable.chinawok1;
        }
        return R.drawable.campero1; // Por defecto
    }

    private String obtenerCategoriaTexto(String categoria) {
        switch (categoria) {
            case "restaurantes":
                return "Comida Rápida";
            case "pizza":
                return "Pizza";
            case "china":
                return "Comida China";
            case "favoritos":
                return "Favoritos";
            default:
                return "Restaurante";
        }
    }

    private List<Producto> filtrarPorCategoria(List<Producto> productos) {
        if ("todos".equals(categoriaSeleccionada)) {
            return productos;
        }

        List<Producto> filtrados = new ArrayList<>();
        for (Producto producto : productos) {
            if (categoriaSeleccionada.equals(producto.getCategoria())) {
                filtrados.add(producto);
            }
        }
        return filtrados;
    }

    private List<Producto> obtenerTodasLasOfertas() {
        List<Producto> lista = new ArrayList<>();
        // Pollo Campero
        lista.add(new Producto("Combo Campero", "Delicioso combo de pollo", R.drawable.campero1, 7.50, 0, "restaurantes", "Pollo Campero"));
        lista.add(new Producto("Pollo Broaster Especial", "Pollo crujiente con papas", R.drawable.pollo_broaster_especial, 8.50, 0, "restaurantes", "Pollo Campero"));
        lista.add(new Producto("Alitas Picantes", "Alitas con salsa picante", R.drawable.alitas_picantes, 6.75, 0, "restaurantes", "Pollo Campero"));
        // Pizza Hut
        lista.add(new Producto("Pizza Familiar", "Ideal para compartir", R.drawable.pizza1, 12.00, 0, "pizza", "Pizza Hut"));
        lista.add(new Producto("Pizza Hawaiana Grande", "Piña y jamón", R.drawable.pizza_hawaiana_grande, 14.00, 0, "pizza", "Pizza Hut"));
        lista.add(new Producto("Pizza Pepperoni", "Con extra queso", R.drawable.pizza_peperoni, 13.50, 0, "pizza", "Pizza Hut"));
        // China Wok
        lista.add(new Producto("Arroz Frito Especial", "Con pollo y verduras", R.drawable.arroz_frito_especial, 9.50, 0, "china", "China Wok"));
        lista.add(new Producto("Chop Suey", "Verduras salteadas", R.drawable.chop_suey, 8.75, 0, "china", "China Wok"));
        lista.add(new Producto("Rollitos Primavera", "Crujientes y deliciosos", R.drawable.rollitos_primavera, 5.25, 0, "china", "China Wok"));
        // Burger King
        lista.add(new Producto("Hamburguesa Doble", "Jugosa y con queso", R.drawable.hamburguesa1, 4.75, 0, "restaurantes", "Burger King"));
        lista.add(new Producto("Whopper", "La clásica de Burger King", R.drawable.whopper, 5.50, 0, "restaurantes", "Burger King"));
        lista.add(new Producto("Combo Whopper", "Whopper con papas y bebida", R.drawable.combo_whopper, 8.00, 0, "restaurantes", "Burger King"));
        return lista;
    }

    private List<Producto> obtenerTodosLosRecomendados() {
        List<Producto> lista = new ArrayList<>();
        // Pollo Campero
        lista.add(new Producto("Pollo Broaster", "Crujiente y sabroso", R.drawable.pollo_broaster, 6.25, 0, "restaurantes", "Pollo Campero"));
        lista.add(new Producto("Combo Familiar", "Para toda la familia", R.drawable.combo_familiar, 15.00, 0, "restaurantes", "Pollo Campero"));
        lista.add(new Producto("Pechuga Empanizada", "Pechuga de pollo empanizada", R.drawable.pechuga_empanizada, 7.00, 0, "restaurantes", "Pollo Campero"));
        // Pizza Hut
        lista.add(new Producto("Pizza Pequeña", "Perfecta para ti solo", R.drawable.pizza_pequenia, 3.99, 0, "pizza", "Pizza Hut"));
        lista.add(new Producto("Pizza Margherita", "Clásica italiana", R.drawable.pizza_margarita, 10.50, 0, "pizza", "Pizza Hut"));
        lista.add(new Producto("Pizza Cuatro Quesos", "Con cuatro tipos de queso", R.drawable.pizza_cuatro_quesos, 13.00, 0, "pizza", "Pizza Hut"));
        // China Wok
        lista.add(new Producto("Wonton Frito", "Deliciosos wontons crujientes", R.drawable.wonton_frito, 5.50, 0, "china", "China Wok"));
        lista.add(new Producto("Pollo Agridulce", "Salsa agridulce especial", R.drawable.pollo_agridulce, 11.00, 0, "china", "China Wok"));
        lista.add(new Producto("Sopa Wantan", "Sopa tradicional china", R.drawable.sopa_wantan, 6.50, 0, "china", "China Wok"));
        // Burger King
        lista.add(new Producto("Combo Junior", "Ideal para niños", R.drawable.combo_junior, 4.50, 0, "restaurantes", "Burger King"));
        lista.add(new Producto("Chicken Royale", "Pollo crujiente", R.drawable.chicken_royale, 5.25, 0, "restaurantes", "Burger King"));
        lista.add(new Producto("Nuggets de Pollo", "6 piezas de nuggets", R.drawable.nuggets_de_pollo, 4.00, 0, "restaurantes", "Burger King"));
        return lista;
    }

    // Dentro de HomeFragment (sólo el método agregarProductos actualizado)
    private void agregarProductos(LinearLayout contenedor, List<Producto> productos, LayoutInflater inflater) {
        if (contenedor == null) {
            Log.w(TAG, "Contenedor null, no se pueden agregar productos.");
            return;
        }

        // obtener DAO + executor
        final com.example.sivareats.data.cart.CartDao cartDao =
                com.example.sivareats.data.AppDatabase.getInstance(requireContext()).cartDao();
        final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

        for (Producto p : productos) {
            try {
                View itemView = inflater.inflate(R.layout.item_oferta, contenedor, false);

                ImageView imgProducto = itemView.findViewById(R.id.imgProductoOferta);
                TextView nombre = itemView.findViewById(R.id.tvNombreProductoOferta);
                TextView descripcion = itemView.findViewById(R.id.txtDescripcionProducto);
                TextView precio = itemView.findViewById(R.id.tvPrecioProductoOferta);
                View btnAgregar = itemView.findViewById(R.id.btnAgregarCarrito);

                // Asignar imagen del producto
                imgProducto.setImageResource(p.getImagenResId());

                nombre.setText(p.getNombre());
                descripcion.setText(p.getDescripcion());
                precio.setText("$" + String.format("%.2f", p.getPrecio()));

                btnAgregar.setOnClickListener(v -> {
                    // Crear CartItem a partir de Producto
                    com.example.sivareats.data.cart.CartItem item =
                            new com.example.sivareats.data.cart.CartItem(
                                    p.getNombre(),
                                    p.getDescripcion(),
                                    p.getImagenResId(), // usa resource id
                                    p.getPrecio(),
                                    1 // cantidad inicial
                            );

                    // Insertar en DB en background
                    executor.execute(() -> cartDao.insert(item));

                    // Toast local
                    Toast.makeText(getContext(),
                            "Agregado al carrito: " + p.getNombre(),
                            Toast.LENGTH_SHORT).show();
                });

                contenedor.addView(itemView);

            } catch (Exception e) {
                Log.e(TAG, "Error al agregar producto " + p.getNombre(), e);
            }
        }
    }
}
