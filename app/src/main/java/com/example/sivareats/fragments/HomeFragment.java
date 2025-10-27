package com.example.sivareats.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.sivareats.R;
import com.example.sivareats.model.Producto;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    public HomeFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_home, container, false);

            // Referencias principales
            LinearLayout layoutOfertas = view.findViewById(R.id.layoutOfertas);
            LinearLayout layoutRecomendados = view.findViewById(R.id.layoutRecomendados);
            LinearLayout layoutBusqueda = view.findViewById(R.id.layoutBusqueda);
            View layoutPrincipal = view.findViewById(R.id.layoutPrincipalHome);
            EditText barraBusqueda = view.findViewById(R.id.etBuscar);

            // Mostrar/Ocultar búsqueda
            barraBusqueda.setOnClickListener(v -> {
                layoutBusqueda.setVisibility(View.VISIBLE);
                layoutPrincipal.setVisibility(View.GONE);
            });

            // Limpia duplicados si el fragment se recrea
            if (layoutOfertas != null) layoutOfertas.removeAllViews();
            if (layoutRecomendados != null) layoutRecomendados.removeAllViews();

            // Cargar productos
            List<Producto> ofertas = obtenerOfertas();
            List<Producto> recomendados = obtenerRecomendados();

            agregarProductos(layoutOfertas, ofertas, inflater);
            agregarProductos(layoutRecomendados, recomendados, inflater);

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error en onCreateView: ", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error al cargar Home: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return new View(requireContext());
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

    private void agregarProductos(LinearLayout contenedor, List<Producto> productos, LayoutInflater inflater) {
        if (contenedor == null) {
            Log.w(TAG, "Contenedor null, no se pueden agregar productos.");
            return;
        }

        for (Producto p : productos) {
            try {
                View itemView = inflater.inflate(R.layout.item_oferta, contenedor, false);

                ImageView img = itemView.findViewById(R.id.imgProductoOferta);
                TextView nombre = itemView.findViewById(R.id.tvNombreProductoOferta);
                TextView descripcion = itemView.findViewById(R.id.txtDescripcionProducto);
                TextView precio = itemView.findViewById(R.id.tvPrecioProductoOferta);

                if (img != null) img.setImageResource(p.getImagenResId());
                if (nombre != null) nombre.setText(p.getNombre());
                if (descripcion != null) descripcion.setText(p.getDescripcion());
                if (precio != null) precio.setText("$" + String.format("%.2f", p.getPrecio()));

                contenedor.addView(itemView);
            } catch (Exception e) {
                Log.e(TAG, "Error al agregar producto " + p.getNombre(), e);
            }
        }
    }
}

