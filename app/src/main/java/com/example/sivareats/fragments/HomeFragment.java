package com.example.sivareats.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
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

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sivareats.R;
import com.example.sivareats.model.CartViewModel;
import com.example.sivareats.model.Producto;
import com.example.sivareats.utils.SearchHistoryManager;
import com.example.sivareats.utils.FavoritosManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private SearchHistoryManager searchHistoryManager;
    private FavoritosManager favoritosManager;
    private OnBackPressedCallback onBackPressedCallback;

    private FrameLayout overlayBusqueda;
    private LinearLayout overlayHistorial;
    private LinearLayout layoutResultadosBusqueda;
    private EditText etBuscarOverlay;
    private TextView tvResultadosTitulo;
    private TextView tvHistorialTitulo;
    private CartViewModel cartViewModel;

    // Botones de categorías
    private MaterialButton btnRestaurantes;
    private MaterialButton btnFavoritos;
    private MaterialButton btnChina;
    private MaterialButton btnPizza;

    // Lista completa de todos los productos para búsqueda
    private List<Producto> todosLosProductos;

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

    // Firebase
    private FirebaseFirestore db;

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
            layoutResultadosBusqueda = view.findViewById(R.id.layoutResultadosBusqueda);
            etBuscarOverlay = view.findViewById(R.id.etBuscarOverlay);
            tvResultadosTitulo = view.findViewById(R.id.tvResultadosTitulo);
            tvHistorialTitulo = view.findViewById(R.id.tvHistorialTitulo);

            searchHistoryManager = new SearchHistoryManager(requireContext());

            // Inicializar Firebase
            db = FirebaseFirestore.getInstance();
            favoritosManager = new FavoritosManager(requireContext());

            // Cargar todas las listas de productos PRIMERO
            todasLasOfertas = obtenerTodasLasOfertas();
            todosLosRecomendados = obtenerTodosLosRecomendados();

            // Crear lista completa de productos para búsqueda DESPUÉS de cargar las listas
            todosLosProductos = new ArrayList<>();
            todosLosProductos.addAll(todasLasOfertas);
            todosLosProductos.addAll(todosLosRecomendados);

            // Cargar platillos desde Firestore y agregarlos al menú
            cargarPlatillosDesdeFirestore(inflater);

            // Mostrar overlay al hacer clic en la barra de búsqueda
            barraBusqueda.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    mostrarOverlay(inflater);
                }
            });
            barraBusqueda.setOnClickListener(v -> {
                barraBusqueda.requestFocus();
                mostrarOverlay(inflater);
            });

            // Configurar búsqueda en el overlay
            configurarBusquedaOverlay(inflater);

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

            // Ocultar overlay al presionar en el fondo
            overlayBusqueda.setOnClickListener(v -> {
                // Solo cerrar si se hace clic directamente en el FrameLayout (fondo)
                if (v.getId() == R.id.overlayBusqueda) {
                    ocultarOverlay();
                }
            });

            // Evitar que el clic se propague desde el contenedor
            View contenedorOverlay = view.findViewById(R.id.contenedorOverlay);
            if (contenedorOverlay != null) {
                contenedorOverlay.setOnClickListener(v -> {
                    // Consumir el evento para que no se propague al overlay
                });
            }

            // Los listeners de categorías se configuran cuando se muestra el overlay
            // No se configuran aquí porque las vistas del overlay pueden no estar disponibles

            // Configurar listeners de botones de categorías
            setupCategoriaButtons(inflater);

            // Configurar listener del botón Todo
            setupBotonTodo(inflater);

            // Mostrar productos iniciales (todos)
            actualizarProductos(inflater);

            // Mostrar restaurantes en sección Todo
            mostrarRestaurantes(inflater);

            // Configurar manejo del botón atrás para cerrar el overlay
            configurarManejoBotonAtras();

            return view;

        } catch (Exception e) {
            Log.e(TAG, "Error en onCreateView: ", e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar platillos cuando el fragment se vuelve visible
        // Esto asegura que los nuevos platillos agregados se muestren
        // Solo recargar si el fragment está visible y hay un LayoutInflater disponible
        if (db != null && isAdded() && getView() != null) {
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            // Limpiar platillos de Firestore antes de recargar para evitar duplicados
            limpiarPlatillosFirestore();
            cargarPlatillosDesdeFirestore(inflater);
        }
    }

    /**
     * Limpia los platillos que fueron cargados desde Firestore de las listas.
     * Esto evita duplicados al recargar.
     */
    private void limpiarPlatillosFirestore() {
        // Identificar platillos de Firestore por tener imagenUrl o restaurante no estándar
        // Por ahora, simplemente recargamos todo desde cero
        // Una mejor solución sería mantener una lista separada de platillos de Firestore
        // Por simplicidad, recargamos los productos estáticos y luego agregamos los de Firestore
        todasLasOfertas.clear();
        todosLosRecomendados.clear();
        todosLosProductos.clear();

        // Recargar productos estáticos
        todasLasOfertas.addAll(obtenerTodasLasOfertas());
        todosLosRecomendados.addAll(obtenerTodosLosRecomendados());
        todosLosProductos.addAll(todasLasOfertas);
        todosLosProductos.addAll(todosLosRecomendados);
    }


    private void mostrarOverlay(LayoutInflater inflater) {
        if (overlayBusqueda == null) {
            Log.e(TAG, "overlayBusqueda es null, no se puede mostrar");
            return;
        }

        overlayBusqueda.setVisibility(View.VISIBLE);

        // Habilitar el callback del botón atrás cuando se muestra el overlay
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(true);
        }

        // Limpiar resultados anteriores
        if (layoutResultadosBusqueda != null) {
            layoutResultadosBusqueda.removeAllViews();
            layoutResultadosBusqueda.setVisibility(View.GONE);
        }
        if (tvResultadosTitulo != null) {
            tvResultadosTitulo.setVisibility(View.GONE);
        }

        // Mostrar historial y configurar categorías
        cargarHistorialBusqueda(inflater);
        cargarCategoriasBuscadas(inflater);

        // Asegurar que el historial y las categorías estén visibles
        if (tvHistorialTitulo != null) {
            tvHistorialTitulo.setVisibility(View.VISIBLE);
        }
        if (overlayHistorial != null) {
            overlayHistorial.setVisibility(View.VISIBLE);
        }
    }

    private void ocultarOverlay() {
        if (overlayBusqueda != null) {
            overlayBusqueda.setVisibility(View.GONE);
        }
        if (etBuscarOverlay != null) {
            etBuscarOverlay.setText("");
        }
        if (layoutResultadosBusqueda != null) {
            layoutResultadosBusqueda.setVisibility(View.GONE);
        }
        if (tvResultadosTitulo != null) {
            tvResultadosTitulo.setVisibility(View.GONE);
        }

        // Deshabilitar el callback del botón atrás cuando se oculta el overlay
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(false);
        }

        // Ocultar el teclado
        if (etBuscarOverlay != null && getActivity() != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etBuscarOverlay.getWindowToken(), 0);
            }
        }
    }

    private void configurarManejoBotonAtras() {
        // Crear callback para manejar el botón atrás
        // Inicialmente deshabilitado, se habilitará cuando se muestre el overlay
        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                // Cerrar el overlay cuando se presiona el botón atrás
                ocultarOverlay();
            }
        };

        // Registrar el callback
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
    }

    private void cargarCategoriasBuscadas(LayoutInflater inflater) {
        // Configurar listeners de categorías cuando se muestra el overlay
        // Buscar las vistas dentro del overlay directamente
        if (overlayBusqueda == null) {
            Log.e(TAG, "overlayBusqueda es null en cargarCategoriasBuscadas");
            return;
        }

        View cardPollo = overlayBusqueda.findViewById(R.id.cardPollo);
        View cardPizza = overlayBusqueda.findViewById(R.id.cardPizza);
        View cardHamburguesa = overlayBusqueda.findViewById(R.id.cardHamburguesa);
        View cardChina = overlayBusqueda.findViewById(R.id.cardChina);

        Log.d(TAG, "Configurando listeners de categorías. Pollo: " + (cardPollo != null) +
                ", Pizza: " + (cardPizza != null) +
                ", Hamburguesa: " + (cardHamburguesa != null) +
                ", China: " + (cardChina != null));

        if (cardPollo != null) {
            cardPollo.setOnClickListener(v -> {
                Log.d(TAG, "Categoría Pollo presionada");
                buscarPorCategoria("pollo", inflater);
            });
        } else {
            Log.e(TAG, "ERROR: cardPollo no encontrado en overlay");
        }

        if (cardPizza != null) {
            cardPizza.setOnClickListener(v -> {
                Log.d(TAG, "Categoría Pizza presionada");
                buscarPorCategoria("pizza", inflater);
            });
        } else {
            Log.e(TAG, "ERROR: cardPizza no encontrado en overlay");
        }

        if (cardHamburguesa != null) {
            cardHamburguesa.setOnClickListener(v -> {
                Log.d(TAG, "Categoría Hamburguesa presionada");
                buscarPorCategoria("hamburguesa", inflater);
            });
        } else {
            Log.e(TAG, "ERROR: cardHamburguesa no encontrado en overlay");
        }

        if (cardChina != null) {
            cardChina.setOnClickListener(v -> {
                Log.d(TAG, "Categoría China presionada");
                buscarPorCategoria("china", inflater);
            });
        } else {
            Log.e(TAG, "ERROR: cardChina no encontrado en overlay");
        }
    }

    private void cargarHistorialBusqueda(LayoutInflater inflater) {
        overlayHistorial.removeAllViews();
        List<String> historial = searchHistoryManager.getHistory();

        if (historial.isEmpty()) {
            TextView t = new TextView(getContext());
            t.setText("Sin búsquedas recientes");
            t.setTextSize(14);
            t.setTextColor(0xFF888888);
            t.setPadding(0, 8, 0, 8);
            overlayHistorial.addView(t);
            return;
        }

        for (String h : historial) {
            View row = inflater.inflate(R.layout.item_historial_busqueda, overlayHistorial, false);
            TextView txt = row.findViewById(R.id.txtHistorialItem);
            txt.setText(h);

            row.setOnClickListener(v -> {
                etBuscarOverlay.setText(h);
                buscarProductos(h, inflater);
            });

            overlayHistorial.addView(row);
        }
    }

    private void configurarBusquedaOverlay(LayoutInflater inflater) {
        // Búsqueda en tiempo real mientras se escribe
        etBuscarOverlay.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // Si está vacío, mostrar categorías y historial
                    layoutResultadosBusqueda.setVisibility(View.GONE);
                    tvResultadosTitulo.setVisibility(View.GONE);
                    tvHistorialTitulo.setVisibility(View.VISIBLE);
                } else {
                    // Buscar mientras escribe
                    buscarProductos(query, inflater);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Buscar al presionar Enter
        etBuscarOverlay.setOnEditorActionListener((v, actionId, event) -> {
            String query = etBuscarOverlay.getText().toString().trim();
            if (!query.isEmpty()) {
                searchHistoryManager.saveSearch(query);
                buscarProductos(query, inflater);
                return true;
            }
            return false;
        });
    }

    private void buscarPorCategoria(String categoria, LayoutInflater inflater) {
        Log.d(TAG, "buscarPorCategoria llamada con: " + categoria);

        // Asegurar que el overlay esté visible y configurado
        if (overlayBusqueda == null) {
            Log.e(TAG, "overlayBusqueda es null");
            return;
        }

        if (overlayBusqueda.getVisibility() != View.VISIBLE) {
            mostrarOverlay(inflater);
        }

        String queryBuscar = "";
        String categoriaFiltro = "";

        switch (categoria.toLowerCase()) {
            case "pollo":
                queryBuscar = "Pollo";
                categoriaFiltro = ""; // Buscar por nombre y restaurante, no por categoría
                break;
            case "pizza":
                queryBuscar = "Pizza";
                categoriaFiltro = "pizza";
                break;
            case "hamburguesa":
                queryBuscar = "Hamburguesa";
                categoriaFiltro = ""; // Buscar por nombre y restaurante
                break;
            case "china":
                queryBuscar = "Comida China";
                categoriaFiltro = "china";
                break;
            default:
                Log.w(TAG, "Categoría desconocida: " + categoria);
                return;
        }

        Log.d(TAG, "Buscando: " + queryBuscar + " con filtro de categoría: " + categoriaFiltro);

        // Actualizar el campo de búsqueda
        if (etBuscarOverlay != null) {
            etBuscarOverlay.setText(queryBuscar);
        }

        // Realizar la búsqueda
        buscarProductosPorCategoria(queryBuscar, categoriaFiltro, inflater);

        // Guardar en historial
        if (searchHistoryManager != null) {
            searchHistoryManager.saveSearch(queryBuscar);
        }

        Log.d(TAG, "Búsqueda de categoría completada");
    }

    private void buscarProductosPorCategoria(String queryNombre, String categoriaFiltro, LayoutInflater inflater) {
        if (queryNombre == null || queryNombre.trim().isEmpty()) {
            if (layoutResultadosBusqueda != null) {
                layoutResultadosBusqueda.setVisibility(View.GONE);
            }
            if (tvResultadosTitulo != null) {
                tvResultadosTitulo.setVisibility(View.GONE);
            }
            if (tvHistorialTitulo != null) {
                tvHistorialTitulo.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (todosLosProductos == null || todosLosProductos.isEmpty()) {
            Log.e(TAG, "todosLosProductos está vacío o es null");
            return;
        }

        String queryLower = queryNombre.toLowerCase().trim();
        List<Producto> resultados = new ArrayList<>();

        Log.d(TAG, "Buscando productos. Query: " + queryLower + ", Total productos: " + todosLosProductos.size());

        // Buscar en todos los productos
        for (Producto producto : todosLosProductos) {
            String nombre = producto.getNombre() != null ? producto.getNombre().toLowerCase() : "";
            String descripcion = producto.getDescripcion() != null ? producto.getDescripcion().toLowerCase() : "";
            String categoria = producto.getCategoria() != null ? producto.getCategoria().toLowerCase() : "";
            String restaurante = producto.getRestaurante() != null ? producto.getRestaurante().toLowerCase() : "";

            boolean coincide = false;

            // Aplicar filtros específicos según la búsqueda
            if (queryLower.equals("pollo")) {
                // Buscar productos de pollo - cualquier producto con "pollo" en nombre o Pollo Campero
                coincide = nombre.contains("pollo") || restaurante.contains("campero");
            } else if (queryLower.equals("pizza")) {
                // Mostrar todos los productos de pizza - categoría "pizza" o nombre contiene pizza
                coincide = categoria.equals("pizza") || nombre.contains("pizza");
            } else if (queryLower.equals("hamburguesa")) {
                // Buscar hamburguesas - productos de Burger King o con "hamburguesa"/"burger"
                coincide = restaurante.contains("burger") ||
                        nombre.contains("hamburguesa") ||
                        nombre.contains("burger") ||
                        nombre.contains("whopper");
            } else if (queryLower.equals("comida china") || queryLower.equals("china")) {
                // Mostrar todos los productos de comida china - categoría "china" o restaurante contiene china
                coincide = categoria.equals("china") || restaurante.contains("china");
            } else if (categoriaFiltro != null && !categoriaFiltro.isEmpty()) {
                // Si hay filtro de categoría, usar solo ese filtro
                coincide = categoria.equals(categoriaFiltro.toLowerCase());
            } else {
                // Búsqueda normal sin filtro específico
                coincide = nombre.contains(queryLower) ||
                        descripcion.contains(queryLower) ||
                        categoria.contains(queryLower) ||
                        restaurante.contains(queryLower);
            }

            if (coincide) {
                resultados.add(producto);
            }
        }

        Log.d(TAG, "Resultados encontrados: " + resultados.size());

        // Mostrar resultados
        mostrarResultadosBusqueda(resultados, inflater);

        // Ocultar historial cuando hay búsqueda activa
        if (tvHistorialTitulo != null) {
            tvHistorialTitulo.setVisibility(View.GONE);
        }
        if (overlayHistorial != null) {
            overlayHistorial.setVisibility(View.GONE);
        }
    }

    private void buscarProductos(String query, LayoutInflater inflater) {
        if (query == null || query.trim().isEmpty()) {
            layoutResultadosBusqueda.setVisibility(View.GONE);
            tvResultadosTitulo.setVisibility(View.GONE);
            tvHistorialTitulo.setVisibility(View.VISIBLE);
            return;
        }

        String queryLower = query.toLowerCase().trim();
        List<Producto> resultados = new ArrayList<>();

        // Buscar en todos los productos
        for (Producto producto : todosLosProductos) {
            String nombre = producto.getNombre() != null ? producto.getNombre().toLowerCase() : "";
            String descripcion = producto.getDescripcion() != null ? producto.getDescripcion().toLowerCase() : "";
            String categoria = producto.getCategoria() != null ? producto.getCategoria().toLowerCase() : "";
            String restaurante = producto.getRestaurante() != null ? producto.getRestaurante().toLowerCase() : "";

            // Buscar coincidencias en nombre, descripción, categoría o restaurante
            if (nombre.contains(queryLower) ||
                    descripcion.contains(queryLower) ||
                    categoria.contains(queryLower) ||
                    restaurante.contains(queryLower)) {
                resultados.add(producto);
            }
        }

        // Mostrar resultados
        mostrarResultadosBusqueda(resultados, inflater);

        // Ocultar historial cuando hay búsqueda activa
        if (!query.trim().isEmpty()) {
            tvHistorialTitulo.setVisibility(View.GONE);
            overlayHistorial.setVisibility(View.GONE);
        } else {
            tvHistorialTitulo.setVisibility(View.VISIBLE);
            overlayHistorial.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarResultadosBusqueda(List<Producto> resultados, LayoutInflater inflater) {
        if (layoutResultadosBusqueda == null) {
            Log.e(TAG, "layoutResultadosBusqueda es null");
            return;
        }

        layoutResultadosBusqueda.removeAllViews();

        Log.d(TAG, "Mostrando " + resultados.size() + " resultados");

        if (resultados.isEmpty()) {
            TextView noResults = new TextView(getContext());
            noResults.setText("No se encontraron resultados");
            noResults.setTextSize(14);
            noResults.setTextColor(0xFF888888);
            noResults.setPadding(0, 16, 0, 16);
            layoutResultadosBusqueda.addView(noResults);
        } else {
            if (tvResultadosTitulo != null) {
                tvResultadosTitulo.setText("Resultados (" + resultados.size() + ")");
                tvResultadosTitulo.setVisibility(View.VISIBLE);
            }

            // Crear contenedor horizontal scroll para los resultados
            android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(getContext());
            scrollView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            scrollView.setHorizontalScrollBarEnabled(false);

            LinearLayout containerHorizontal = new LinearLayout(getContext());
            containerHorizontal.setOrientation(LinearLayout.HORIZONTAL);
            containerHorizontal.setPadding(4, 0, 4, 0);

            // Agregar productos a los resultados
            agregarProductos(containerHorizontal, resultados, inflater);

            scrollView.addView(containerHorizontal);
            layoutResultadosBusqueda.addView(scrollView);
        }

        layoutResultadosBusqueda.setVisibility(View.VISIBLE);
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

    private void setupBotonTodo(LayoutInflater inflater) {
        btnTodo.setOnClickListener(v -> {
            // Alternar visibilidad del layoutTodo
            if (layoutTodo.getVisibility() == View.VISIBLE) {
                layoutTodo.setVisibility(View.GONE);
            } else {
                layoutTodo.setVisibility(View.VISIBLE);
                // Cargar todos los platillos (estáticos y de Firebase) usando la misma tarjeta
                mostrarTodosLosPlatillos(inflater);
            }
        });
    }

    /**
     * Muestra todos los platillos (estáticos y de Firebase) en el layoutTodo usando la misma tarjeta item_oferta.
     */
    private void mostrarTodosLosPlatillos(LayoutInflater inflater) {
        layoutTodo.removeAllViews();

        // Obtener todos los productos (estáticos y de Firebase)
        List<Producto> todosLosProductos = new ArrayList<>();
        todosLosProductos.addAll(todasLasOfertas);
        todosLosProductos.addAll(todosLosRecomendados);

        // Crear un contenedor horizontal scrollable para los platillos
        HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        scrollView.setHorizontalScrollBarEnabled(false);

        LinearLayout layoutProductos = new LinearLayout(getContext());
        layoutProductos.setOrientation(LinearLayout.HORIZONTAL);
        layoutProductos.setPadding(4, 0, 4, 0);

        // Agregar todos los productos usando la misma tarjeta item_oferta
        agregarProductos(layoutProductos, todosLosProductos, inflater);

        scrollView.addView(layoutProductos);
        layoutTodo.addView(scrollView);
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

        // Obtener todos los productos sin duplicados usando un Set
        Set<String> productosVistos = new HashSet<>();
        List<Producto> todosLosProductos = new ArrayList<>();

        // Agregar productos de todasLasOfertas
        for (Producto p : todasLasOfertas) {
            String key = (p.getRestaurante() != null ? p.getRestaurante() : "") + "|" + p.getNombre();
            if (!productosVistos.contains(key)) {
                productosVistos.add(key);
                todosLosProductos.add(p);
            }
        }

        // Agregar productos de todosLosRecomendados que no estén ya en la lista
        for (Producto p : todosLosRecomendados) {
            String key = (p.getRestaurante() != null ? p.getRestaurante() : "") + "|" + p.getNombre();
            if (!productosVistos.contains(key)) {
                productosVistos.add(key);
                todosLosProductos.add(p);
            }
        }

        // Agrupar por restaurante (solo restaurantes con al menos un platillo visible)
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

        // Obtener todos los nombres de restaurantes únicos (incluyendo los de Firestore)
        java.util.Set<String> todosLosRestaurantes = new java.util.HashSet<>();
        todosLosRestaurantes.addAll(restaurantesMap.keySet());

        // Agregar restaurantes estáticos conocidos al conjunto
        String[] restaurantesEstaticos = {"Pollo Campero", "Pizza Hut", "China Wok", "Burger King"};
        for (String restaurante : restaurantesEstaticos) {
            if (restaurantesMap.containsKey(restaurante)) {
                todosLosRestaurantes.add(restaurante);
            }
        }

        // Orden de restaurantes específico (solo los que tienen productos)
        String[] ordenRestaurantes = {"Pollo Campero", "Pizza Hut", "China Wok", "Burger King"};

        // Primero mostrar restaurantes en el orden especificado
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

        // Luego mostrar restaurantes de Firestore que no están en el orden estático
        for (String nombreRestaurante : todosLosRestaurantes) {
            // Solo agregar si no está en el orden estático
            boolean yaAgregado = false;
            for (String restauranteEstatico : ordenRestaurantes) {
                if (nombreRestaurante.equals(restauranteEstatico)) {
                    yaAgregado = true;
                    break;
                }
            }

            if (!yaAgregado && restaurantesMap.containsKey(nombreRestaurante)) {
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

        // Título del restaurante (clickeable para abrir detalles)
        TextView tituloRestaurante = new TextView(getContext());
        tituloRestaurante.setText(nombreRestaurante);
        tituloRestaurante.setTextSize(22);
        tituloRestaurante.setTypeface(null, android.graphics.Typeface.BOLD);
        // Usar color que se adapta al tema (claro/oscuro)
        // Obtener el color del tema actual
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
            typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            tituloRestaurante.setTextColor(typedValue.data);
        } else {
            // Fallback: usar text_primary que se adapta al tema
            tituloRestaurante.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        }
        tituloRestaurante.setPadding(8, 16, 8, 12);
        tituloRestaurante.setClickable(true);
        tituloRestaurante.setFocusable(true);
        tituloRestaurante.setOnClickListener(v -> {
            // Navegar a la vista de detalles del restaurante
            android.content.Intent intent = new android.content.Intent(getContext(),
                    com.example.sivareats.ui.restaurant.RestaurantDetailActivity.class);
            intent.putExtra("RESTAURANT_NAME", nombreRestaurante);
            startActivity(intent);
        });
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

        // Si se selecciona "favoritos", filtrar por favoritos
        if ("favoritos".equals(categoriaSeleccionada)) {
            List<Producto> filtrados = new ArrayList<>();
            List<String> favoritos = favoritosManager.getFavoritos();
            for (Producto producto : productos) {
                if (favoritos.contains(producto.getNombre())) {
                    filtrados.add(producto);
                }
            }
            return filtrados;
        }

        // Para otras categorías, filtrar por categoría
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
                ImageView btnFavorito = itemView.findViewById(R.id.btnFavorito);

                // Asignar imagen del producto (desde URL o resource ID)
                if (p.getImagenUrl() != null && !p.getImagenUrl().isEmpty()) {
                    // Cargar imagen desde URL usando Glide
                    Glide.with(requireContext())
                            .load(p.getImagenUrl())
                            .placeholder(R.drawable.campero1) // Imagen por defecto mientras carga
                            .error(R.drawable.campero1) // Imagen de error si falla
                            .into(imgProducto);
                } else {
                    // Usar resource ID
                    imgProducto.setImageResource(p.getImagenResId());
                }

                nombre.setText(p.getNombre());
                descripcion.setText(p.getDescripcion());
                precio.setText("$" + String.format("%.2f", p.getPrecio()));

                // Configurar estado inicial del corazón (favorito o no)
                boolean esFavorito = favoritosManager.esFavorito(p.getNombre());
                actualizarIconoFavorito(btnFavorito, esFavorito);

                // Listener para el botón de favorito
                btnFavorito.setOnClickListener(v -> {
                    boolean esFav = favoritosManager.esFavorito(p.getNombre());
                    if (esFav) {
                        favoritosManager.removerFavorito(p.getNombre());
                        actualizarIconoFavorito(btnFavorito, false);
                        Toast.makeText(getContext(), "Removido de favoritos", Toast.LENGTH_SHORT).show();
                    } else {
                        favoritosManager.agregarFavorito(p.getNombre());
                        actualizarIconoFavorito(btnFavorito, true);
                        Toast.makeText(getContext(), "Agregado a favoritos", Toast.LENGTH_SHORT).show();
                    }
                });

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

    // Método auxiliar para actualizar el icono del corazón
    private void actualizarIconoFavorito(ImageView btnFavorito, boolean esFavorito) {
        if (esFavorito) {
            btnFavorito.setImageResource(R.drawable.ic_corazon_filled);
            btnFavorito.setColorFilter(0xFF1F41EC);
        } else {
            btnFavorito.setImageResource(R.drawable.ic_corazon);
            btnFavorito.setColorFilter(0xFF1F41EC);
        }
    }

    /**
     * Carga los platillos desde Firestore y los agrega al menú principal.
     */
    private void cargarPlatillosDesdeFirestore(LayoutInflater inflater) {
        if (db == null) {
            Log.e(TAG, "FirebaseFirestore no inicializado");
            return;
        }

        // Usar un Set para rastrear platillos ya agregados y evitar duplicados
        Set<String> platillosAgregados = new HashSet<>();

        db.collection("restaurantes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Producto> platillosFirestore = new ArrayList<>();
                    int totalRestaurantes = queryDocumentSnapshots.size();
                    final int[] restaurantesProcesados = {0};

                    if (totalRestaurantes == 0) {
                        Log.d(TAG, "No hay restaurantes en Firestore");
                        return;
                    }

                    for (QueryDocumentSnapshot restauranteDoc : queryDocumentSnapshots) {
                        String nombreRestaurante = restauranteDoc.getId();

                        // Obtener platillos directamente del documento del restaurante
                        Map<String, Object> data = restauranteDoc.getData();
                        if (data != null) {
                            // Set local para evitar duplicados dentro del mismo documento
                            Set<String> platillosEnDocumento = new HashSet<>();

                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                // Verificar si es un platillo (tiene la estructura esperada)
                                Object value = entry.getValue();
                                if (value instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> platilloData = (Map<String, Object>) value;
                                    // Verificar que tenga los campos de un platillo y que sea visible
                                    if (platilloData.containsKey("nombrePlatillo")) {
                                        String nombrePlatillo = (String) platilloData.get("nombrePlatillo");

                                        // Verificar duplicados dentro del mismo documento
                                        String keyLocal = nombreRestaurante + "|" + nombrePlatillo;
                                        if (platillosEnDocumento.contains(keyLocal)) {
                                            Log.d(TAG, "Platillo duplicado ignorado (dentro del mismo documento): " + keyLocal);
                                            continue;
                                        }
                                        platillosEnDocumento.add(keyLocal);

                                        Boolean visible = (Boolean) platilloData.get("visible");
                                        if (visible == null || visible) { // Solo mostrar platillos visibles
                                            try {
                                                String descripcion = (String) platilloData.get("Descripcion");
                                                String categoria = (String) platilloData.get("categoria");
                                                String imagenUrl = (String) platilloData.get("URL_imagen_platillo");
                                                Double precio = (Double) platilloData.get("precio");

                                                if (nombrePlatillo != null && precio != null) {
                                                    // Crear clave única para verificar duplicados
                                                    String productoKey = nombreRestaurante + "|" + nombrePlatillo;

                                                    // Verificar si ya fue agregado en esta ejecución
                                                    if (platillosAgregados.contains(productoKey)) {
                                                        Log.d(TAG, "Platillo duplicado ignorado (ya en esta carga): " + productoKey);
                                                        continue;
                                                    }

                                                    // Verificar si ya existe en las listas actuales (productos estáticos o de Firestore)
                                                    // Si un platillo de Firestore tiene el mismo nombre y restaurante que uno existente,
                                                    // no lo agregamos para evitar duplicados
                                                    boolean yaExiste = false;
                                                    for (Producto p : todasLasOfertas) {
                                                        if (p.getNombre().equals(nombrePlatillo) &&
                                                            p.getRestaurante() != null &&
                                                            p.getRestaurante().equals(nombreRestaurante)) {
                                                            yaExiste = true;
                                                            Log.d(TAG, "Platillo duplicado ignorado (ya existe en listas): " + productoKey);
                                                            break;
                                                        }
                                                    }

                                                    if (yaExiste) {
                                                        continue;
                                                    }

                                                    // Crear Producto desde Firestore con URL de imagen
                                                    Producto producto;
                                                    if (imagenUrl != null && !imagenUrl.isEmpty()) {
                                                        // Usar constructor con URL de imagen
                                                        producto = new Producto(
                                                                nombrePlatillo,
                                                                descripcion != null ? descripcion : "",
                                                                imagenUrl,
                                                                precio,
                                                                0,
                                                                categoria != null ? categoria.toLowerCase() : "restaurantes",
                                                                nombreRestaurante
                                                        );
                                                    } else {
                                                        // Usar constructor con resource ID por defecto
                                                        producto = new Producto(
                                                                nombrePlatillo,
                                                                descripcion != null ? descripcion : "",
                                                                R.drawable.campero1,
                                                                precio,
                                                                0,
                                                                categoria != null ? categoria.toLowerCase() : "restaurantes",
                                                                nombreRestaurante
                                                        );
                                                    }

                                                    // Agregar a las listas y marcar como agregado
                                                    platillosFirestore.add(producto);
                                                    platillosAgregados.add(productoKey);

                                                    // Agregar solo a todasLasOfertas para evitar duplicados
                                                    // (no agregar a todosLosRecomendados porque causaría duplicación)
                                                    todasLasOfertas.add(producto);
                                                    todosLosProductos.add(producto);

                                                    Log.d(TAG, "Platillo agregado desde Firestore: " + productoKey);
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error al procesar platillo: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Incrementar contador de restaurantes procesados
                        restaurantesProcesados[0]++;

                        // Si todos los restaurantes han sido procesados, actualizar la UI
                        if (restaurantesProcesados[0] >= totalRestaurantes) {
                            if (getActivity() != null && isAdded()) {
                                getActivity().runOnUiThread(() -> {
                                    actualizarProductos(inflater);
                                    mostrarRestaurantes(inflater);
                                    if (categoriaSeleccionada.equals("restaurantes")) {
                                        mostrarRestaurantesAgrupados(inflater);
                                    }
                                    Log.d(TAG, "Platillos cargados desde Firestore: " + platillosFirestore.size());
                                });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar restaurantes desde Firestore: " + e.getMessage());
                });
    }
}