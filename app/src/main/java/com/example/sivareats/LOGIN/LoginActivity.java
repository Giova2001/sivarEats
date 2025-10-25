package com.example.sivareats.LOGIN;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.User;
import com.example.sivareats.data.UserDao;
import com.example.sivareats.ui.NavegacionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox checkBox;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "loginPrefs";

    private FirebaseAuth mAuth;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        checkBox = findViewById(R.id.checkBox);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegistrate = findViewById(R.id.tvRegistrate);// Botón para ir a registro

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();

        // Inicializar Room DB
        AppDatabase db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Cargar sesión si existe
        loadLoginData();

        // Login con Firebase
        btnLogin.setOnClickListener(v -> loginUser());

        // Ir a pantalla de registro
        tvRegistrate.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, LoginRegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deshabilitar botón mientras se procesa (opcional)
        findViewById(R.id.btnLogin).setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    findViewById(R.id.btnLogin).setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Guardar en SQLite local si quieres
                            saveUserLocal(email);

                            // Guardar sesión si está marcado
                            if (checkBox.isChecked()) {
                                saveLoginData(email);
                            } else {
                                clearLoginData();
                            }

                            Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                            goToMainScreen();
                        }
                    } else {
                        String errorMsg = "Error al iniciar sesión";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMsg = task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Guardar solo email en SharedPreferences
    private void saveLoginData(String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putBoolean("remember", true);
        editor.apply();
    }

    private void loadLoginData() {
        boolean remember = sharedPreferences.getBoolean("remember", false);
        if (remember) {
            etEmail.setText(sharedPreferences.getString("email", ""));
            checkBox.setChecked(true);
        }
    }

    private void clearLoginData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    // Guardar en SQLite/Room (local)
    private void saveUserLocal(String email) {
        new Thread(() -> {
            try {
                userDao.insertUser(new User(email));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void goToMainScreen() {
        Intent intent = new Intent(LoginActivity.this, NavegacionActivity.class);
        startActivity(intent);
        finish();
    }
}
