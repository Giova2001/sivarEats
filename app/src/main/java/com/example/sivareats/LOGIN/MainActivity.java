package com.example.sivareats.LOGIN;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sivareats.R;
import com.example.sivareats.ui.NavegacionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 segundos de espera
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cargar preferencia de tema antes de setContentView
        cargarTema();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificar permisos y servicios de ubicación antes de continuar
        verificarUbicacion();
    }

    private void verificarUbicacion() {
        // Verificar permiso de ubicación
        boolean tienePermiso = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;

        // Verificar si los servicios de ubicación están activos
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean ubicacionActiva = locationManager != null && 
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
                 locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        if (!tienePermiso) {
            // Solicitar permiso de ubicación
            Log.d(TAG, "Permiso de ubicación no concedido, solicitando...");
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else if (!ubicacionActiva) {
            // Mostrar diálogo para activar ubicación
            Log.d(TAG, "Servicios de ubicación desactivados, solicitando activación...");
            mostrarDialogoActivarUbicacion();
        } else {
            // Todo está bien, marcar como verificado y continuar con el flujo normal
            Log.d(TAG, "Permisos y servicios de ubicación verificados correctamente");
            SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("ubicacion_verificada", true).apply();
            continuarConFlujo();
        }
    }

    private void mostrarDialogoActivarUbicacion() {
        new AlertDialog.Builder(this)
                .setTitle("Ubicación desactivada")
                .setMessage("Para usar esta aplicación, necesitas activar los servicios de ubicación. ¿Deseas activarlos ahora?")
                .setPositiveButton("Activar", (dialog, which) -> {
                    // Abrir configuración de ubicación
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                    // Esperar un poco y verificar nuevamente
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        verificarUbicacion();
                    }, 1000);
                })
                .setNegativeButton("Continuar sin ubicación", (dialog, which) -> {
                    // Continuar sin ubicación (aunque algunas funciones no funcionarán)
                    Toast.makeText(this, "Algunas funciones pueden no estar disponibles sin ubicación", Toast.LENGTH_LONG).show();
                    SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("ubicacion_verificada", true).apply();
                    continuarConFlujo();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de ubicación concedido");
                // Verificar si los servicios de ubicación están activos
                verificarUbicacion();
            } else {
                Log.d(TAG, "Permiso de ubicación denegado");
                // Mostrar diálogo explicando la importancia del permiso
                new AlertDialog.Builder(this)
                        .setTitle("Permiso de ubicación necesario")
                        .setMessage("Esta aplicación necesita acceso a tu ubicación para funcionar correctamente. Puedes activarlo desde Configuración.")
                        .setPositiveButton("Continuar", (dialog, which) -> {
                            // Continuar sin permiso (aunque algunas funciones no funcionarán)
                            Toast.makeText(this, "Algunas funciones pueden no estar disponibles sin ubicación", Toast.LENGTH_LONG).show();
                            SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
                            prefs.edit().putBoolean("ubicacion_verificada", true).apply();
                            continuarConFlujo();
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void continuarConFlujo() {
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

    @Override
    protected void onResume() {
        super.onResume();
        // Si el usuario regresa de la configuración, verificar nuevamente
        // Solo verificar si aún no se ha continuado con el flujo
        if (!isFinishing()) {
            // Verificar si ya se verificó antes (evitar loops)
            SharedPreferences prefs = getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
            boolean yaVerificado = prefs.getBoolean("ubicacion_verificada", false);
            
            if (!yaVerificado) {
                verificarUbicacion();
            }
        }
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