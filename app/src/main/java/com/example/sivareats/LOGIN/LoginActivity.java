package com.example.sivareats.LOGIN;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.UserDao;
import com.example.sivareats.ui.NavegacionActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox checkBox;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "loginPrefs";

    private UserDao userDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        checkBox = findViewById(R.id.checkBox);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegistrate = findViewById(R.id.tvRegistrate);

        // Room
        AppDatabase db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadLoginData();

        // Login local
        btnLogin.setOnClickListener(v -> loginUserLocal());

        // Ir a registro
        tvRegistrate.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, LoginRegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUserLocal() {
        String email = safeText(etEmail);
        String password = safeText(etPassword);

        // Validaciones UI
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo");
            etEmail.requestFocus();
            toast("Completa todos los campos");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Formato de correo inválido");
            etEmail.requestFocus();
            toast("Formato de email inválido");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa tu contraseña");
            etPassword.requestFocus();
            toast("Completa todos los campos");
            return;
        }

        findViewById(R.id.btnLogin).setEnabled(false);

        // Validación en BD local (Room)
        ioExecutor.execute(() -> {
            boolean ok = false;
            try {
                ok = userDao.validateLogin(email, password);
            } catch (Exception ignored) {}

            boolean finalOk = ok;
            runOnUiThread(() -> {
                findViewById(R.id.btnLogin).setEnabled(true);
                if (finalOk) {
                    if (checkBox.isChecked()) {
                        saveLoginData(email);
                    } else {
                        clearLoginData();
                    }
                    toast("Inicio de sesión exitoso (local)");
                    goToMainScreen();
                } else {
                    toast("Correo o contraseña inválidos");
                }
            });
        });
    }

    // Utils
    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

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

    private void goToMainScreen() {
        Intent intent = new Intent(LoginActivity.this, NavegacionActivity.class);
        startActivity(intent);
        finish();
    }
}
