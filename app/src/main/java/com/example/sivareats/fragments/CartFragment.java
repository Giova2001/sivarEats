package com.example.sivareats.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sivareats.R;
import com.example.sivareats.adapters.CarritoAdapter;
import com.example.sivareats.model.Producto;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    public CartFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        RecyclerView recyclerCarrito = view.findViewById(R.id.recyclerCarrito);
        recyclerCarrito.setLayoutManager(new LinearLayoutManager(getContext()));

        // Crear lista de productos
        List<Producto> productos = new ArrayList<>();
        productos.add(new Producto("Combo Super Campero",
                "Tres piezas de pollo más una ensalada y un pan",
                R.drawable.campero1,
                7.50,
                1));
        productos.add(new Producto("Pizza Pepperoni",
                "Pizza grande con extra de pepperoni",
                R.drawable.pizza1,
                12.00,
                1));

        CarritoAdapter adapter = new CarritoAdapter(productos);
        recyclerCarrito.setAdapter(adapter);

        // Listener para actualizar totales
        adapter.setOnCantidadChangeListener(() -> actualizarTotales(productos, view));

        // Llamada inicial para mostrar totales al cargar el fragment
        actualizarTotales(productos, view);

        return view;
    }
    private void actualizarTotales(List<Producto> productos, View view) {
        double subtotal = 0;
        for (Producto p : productos) {
            subtotal += p.getPrecio() * p.getCantidad();
        }

        double delivery = 2.50; // calcular dinámicamente o dejar fijo
        double total = subtotal + delivery;

        TextView tvSubtotal = view.findViewById(R.id.tvSubtotal);
        TextView tvDelivery = view.findViewById(R.id.tvDelivery);
        TextView tvTotal = view.findViewById(R.id.tvTotal);

        tvSubtotal.setText("Subtotal: $" + String.format("%.2f", subtotal));
        tvDelivery.setText("Delivery: $" + String.format("%.2f", delivery));
        tvTotal.setText("Total: $" + String.format("%.2f", total));
    }

}