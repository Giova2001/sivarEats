package com.example.sivareats.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.sivareats.R;
import com.example.sivareats.model.CartViewModel;
import com.example.sivareats.model.Producto;
import com.example.sivareats.utils.SearchHistoryManager;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private SearchHistoryManager searchHistoryManager;

    private FrameLayout overlayBusqueda;
    private LinearLayout overlayHistorial;
    private CartViewModel cartViewModel;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            // Referencias
            LinearLayout layoutOfertas = view.findViewById(R.id.layoutOfertas);
            LinearLayout layoutRecomendados = view.findViewById(R.id.layoutRecomendados);
            EditText barraBusqueda = view.findViewById(R.id.etBuscar);

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

            // Cargar productos
            layoutOfertas.removeAllViews();
            layoutRecomendados.removeAllViews();
            agregarProductos(layoutOfertas, obtenerOfertas(), inflater);
            agregarProductos(layoutRecomendados, obtenerRecomendados(), inflater);

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

    private List<Producto> obtenerOfertas() {
        List<Producto> lista = new ArrayList<>();
        lista.add(new Producto("Combo Campero", "Delicioso combo de pollo", R.drawable.campero1, 7.50, 0));
        lista.add(new Producto("Pizza Familiar", "Ideal para compartir", R.drawable.pizza1, 12.00, 0));
        lista.add(new Producto("Hamburguesa Doble", "Jugosa y con queso", R.drawable.hamburguesa1, 4.75, 0));
        return lista;
    }

    private List<Producto> obtenerRecomendados() {
        List<Producto> lista = new ArrayList<>();
        lista.add(new Producto("Pollo Broaster", "Crujiente y sabroso", R.drawable.campero1, 6.25, 0));
        lista.add(new Producto("Pizza Pequeña", "Perfecta para ti solo", R.drawable.pizza1, 3.99, 0));
        lista.add(new Producto("Combo Junior", "Ideal para niños", R.drawable.hamburguesa1, 4.50, 0));
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

                TextView nombre = itemView.findViewById(R.id.tvNombreProductoOferta);
                TextView descripcion = itemView.findViewById(R.id.txtDescripcionProducto);
                TextView precio = itemView.findViewById(R.id.tvPrecioProductoOferta);
                View btnAgregar = itemView.findViewById(R.id.btnAgregarCarrito);

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
