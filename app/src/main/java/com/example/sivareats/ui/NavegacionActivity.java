package com.example.sivareats.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.sivareats.R;
import com.example.sivareats.fragments.*;
import com.example.sivareats.model.UserType;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class NavegacionActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment activeFragment;
    private UserType currentUserType = UserType.USUARIO_NORMAL; // Cambiar según el usuario logueado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navegacion);

        // Obtener tipo de usuario (esto vendría del login)
        String userTypeFromIntent = getIntent().getStringExtra("USER_TYPE");
        if (userTypeFromIntent != null) {
            currentUserType = UserType.fromString(userTypeFromIntent);
        }

        initializeViews();
        setupBottomNavigation();
        setupMenuForUserType();

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupMenuForUserType() {
        // Ocultar todos los items primero
        bottomNavigationView.getMenu().findItem(R.id.nav_cart).setVisible(false);
        bottomNavigationView.getMenu().findItem(R.id.nav_edit_platillo).setVisible(false);
        bottomNavigationView.getMenu().findItem(R.id.nav_assign_order).setVisible(false);

        // Mostrar items según el tipo de usuario
        switch (currentUserType) {
            case USUARIO_NORMAL:
                setupNormalUserMenu();
                break;
            case REPARTIDOR:
                setupDeliveryUserMenu();
                break;
            case RESTAURANTE:
                setupRestaurantUserMenu();
                break;
            case ADMIN:
                setupAdminMenu();
                break;
        }
    }

    private void setupNormalUserMenu() {
        // Usuario normal: Home, Carrito, Pedidos, Perfil
        bottomNavigationView.getMenu().findItem(R.id.nav_cart).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_orders).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);

        // Seleccionar Home por defecto
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupDeliveryUserMenu() {
        // Repartidor: Home, Asignar Pedido, Pedidos, Perfil
        bottomNavigationView.getMenu().findItem(R.id.nav_assign_order).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_orders).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupRestaurantUserMenu() {
        // Restaurante: Home, Editar Platillo, Pedidos, Perfil
        bottomNavigationView.getMenu().findItem(R.id.nav_edit_platillo).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_orders).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupAdminMenu() {
        // Admin: Todos los items visibles
        bottomNavigationView.getMenu().findItem(R.id.nav_cart).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_edit_platillo).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_assign_order).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_orders).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_cart) {
                    selectedFragment = new CartFragment();
                } else if (itemId == R.id.nav_edit_platillo) {
                    selectedFragment = new EditPlatilloFragment();
                } else if (itemId == R.id.nav_assign_order) {
                    selectedFragment = new AssignOrderFragment();
                } else if (itemId == R.id.nav_orders) {
                    selectedFragment = new OrdersFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        String fragmentTag = fragment.getClass().getSimpleName();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Ocultar TODOS los fragmentos visibles en el contenedor
        // Esto asegura que fragmentos secundarios como EnvioFragment también se oculten
        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            if (frag != null && frag.isVisible()) {
                transaction.hide(frag);
            }
        }

        // Buscar si el fragment ya existe
        Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);

        if (existingFragment != null) {
            // Si existe, mostrarlo
            transaction.show(existingFragment);
            activeFragment = existingFragment;
        } else {
            // Si no existe, añadirlo
            transaction.add(R.id.fragment_container, fragment, fragmentTag);
            activeFragment = fragment;
        }

        transaction.commit();
    }


    public void OnBackPressed() {
        if (bottomNavigationView.getSelectedItemId() == R.id.nav_home) {
            super.onBackPressed();
        } else {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }
}