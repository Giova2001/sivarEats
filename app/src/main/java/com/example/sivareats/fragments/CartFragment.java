package com.example.sivareats.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.sivareats.R;
import com.example.sivareats.adapters.CarritoAdapter;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.cart.CartItem;
import com.example.sivareats.data.cart.CartDao;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private RecyclerView recyclerView;
    private CarritoAdapter adapter;
    private TextView tvSubtotal, tvDelivery, tvTotal;

    private CartDao cartDao;

    private Button btnConfirmar;

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

        btnConfirmar = view.findViewById(R.id.btnConfirmar);

        cartDao = AppDatabase.getInstance(requireContext()).cartDao();

        adapter = new CarritoAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnCantidadChangeListener(() -> actualizarTotales(adapter.getLista()));

        cartDao.getAllLive().observe(getViewLifecycleOwner(), new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                adapter.updateList(cartItems);
                actualizarTotales(cartItems);
            }
        });

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

    private void actualizarTotales(List<CartItem> cartItems) {
        double subtotal = 0;

        for (CartItem p : cartItems) {
            subtotal += p.getPrecio() * p.getCantidad();
        }

        double delivery = 1.50;
        double total = subtotal + delivery;

        tvSubtotal.setText("Subtotal: $" + String.format("%.2f", subtotal));
        tvDelivery.setText("Delivery: $" + String.format("%.2f", delivery));
        tvTotal.setText("Total: $" + String.format("%.2f", total));
    }
}
