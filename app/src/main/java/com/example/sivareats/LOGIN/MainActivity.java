package com.example.sivareats.LOGIN;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.example.sivareats.ui.NavegacionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 segundos de espera

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
}