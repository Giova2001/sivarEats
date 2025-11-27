package com.example.sivareats.LOGIN;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.sivareats.R;
import com.example.sivareats.ui.NavegacionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 segundos de espera

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cargar preferencia de tema antes de setContentView
        cargarTema();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Verificar el estado de autenticación de Firebase
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            // Decidir a qué actividad ir
            if (currentUser != null) {
                // Si el usuario ya ha iniciado sesión, ir a la pantalla principal
                goToNavegacionActivity();
            } else {
                // Si no hay sesión, ir a la pantalla de login
                goToLoginActivity();
            }

        }, SPLASH_DELAY);
    }

    private void goToNavegacionActivity() {
        Intent intent = new Intent(MainActivity.this, NavegacionActivity.class);
        startActivity(intent);
        finish(); // Cierra esta actividad para que el usuario no pueda volver a ella
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Cierra esta actividad
    }

    private void cargarTema() {
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        int nightMode = themePrefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}