package com.example.sivareats.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sivareats.R;

public class UbicationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ubication);

        // Referencia correcta del botón
        Button btnEditUbication = findViewById(R.id.irAEditLocation);

        // Acción al presionar el botón
        btnEditUbication.setOnClickListener(v -> {
            Intent intent = new Intent(UbicationActivity.this, LocationEditActivity.class);
            startActivity(intent);
        });

        // Configurar Toolbar (usa androidx.appcompat.widget.Toolbar, no android.widget.Toolbar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Acción de navegación (flecha atrás)
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}
