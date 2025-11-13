package com.example.sivareats.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.sivareats.ui.ordenes.RastreandoPedidoActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerViewPedidos;
    private OrdenesAdapter adapter;
    private TabLayout tabLayout;
    private TextInputEditText etBuscar;
    
    private List<Pedido> pedidosActivos;
    private List<Pedido> pedidosCompletados;
    private List<Pedido> pedidosFiltrados;

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
        
        // Inicializar datos de ejemplo
        inicializarDatosEjemplo();
        
        // Configurar adapter
        adapter = new OrdenesAdapter(pedidosActivos);
        adapter.setOnPedidoClickListener(new OrdenesAdapter.OnPedidoClickListener() {
            @Override
            public void onRastrearClick(Pedido pedido) {
                Intent intent = new Intent(getContext(), RastreandoPedidoActivity.class);
                intent.putExtra("pedido_id", pedido.getId());
                intent.putExtra("restaurante", pedido.getRestaurante());
                intent.putExtra("repartidor_nombre", pedido.getRepartidorNombre());
                intent.putExtra("repartidor_telefono", pedido.getRepartidorTelefono());
                intent.putExtra("tiempo_estimado", pedido.getTiempoEstimado());
                startActivity(intent);
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
        
        return view;
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
        if (position == 0) {
            // Tab "En curso"
            pedidosFiltrados = new ArrayList<>(pedidosActivos);
        } else {
            // Tab "Historial"
            pedidosFiltrados = new ArrayList<>(pedidosCompletados);
        }
        filtrarPedidos(etBuscar.getText().toString());
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