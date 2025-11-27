package com.example.sivareats.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.sivareats.R;
import com.example.sivareats.data.AppDatabase;
import com.example.sivareats.data.User;
import com.example.sivareats.ui.profile.EditProfileActivity;
import com.example.sivareats.ui.profile.InvitationCodeActivity;
import com.example.sivareats.ui.profile.PaymentMethodActivity;
import com.example.sivareats.ui.profile.SupportActivity;
import com.example.sivareats.ui.profile.SobreAppActivity;
import com.example.sivareats.ui.profile.UbicationActivity;
import com.example.sivareats.LOGIN.LoginActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfilePhone, tvProfileEmail;
    private ImageView imgPerfil;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private AppDatabase roomDb;
    private static final String PREF_NAME = "loginPrefs";

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = FirebaseFirestore.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        roomDb = AppDatabase.getInstance(requireContext());

        // Referencias UI
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        imgPerfil = view.findViewById(R.id.imgPerfil);

        setupButtons(view);
        loadUserProfile();

        return view;
    }

    private void setupButtons(View view) {
        // Editar perfil
        view.findViewById(R.id.btnEditarPerfil).setOnClickListener(v -> {
            SharedPreferences sessionPrefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
            String userEmail = sessionPrefs.getString("CURRENT_USER_EMAIL", null);

            if (userEmail != null && !userEmail.isEmpty()) {
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                intent.putExtra("user_email", userEmail);
                startActivity(intent);
            } else {
                Log.e("ProfileFragment", "No se encontró correo de usuario para editar perfil");
            }
        });

        view.findViewById(R.id.btnUbicacion).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), UbicationActivity.class)));

        view.findViewById(R.id.btnMetodoPago).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PaymentMethodActivity.class)));

        view.findViewById(R.id.btnCodigoInvitacion).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), InvitationCodeActivity.class)));

        view.findViewById(R.id.btnSoporte).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SupportActivity.class)));

        view.findViewById(R.id.btnTerminos).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SobreAppActivity.class)));

        // Configurar botón de tema
        actualizarTextoBotonTema(view);
        view.findViewById(R.id.btnCambiarTema).setOnClickListener(v -> cambiarTema());

        view.findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> logout());
    }

    private void actualizarTextoBotonTema(View view) {
        com.google.android.material.button.MaterialButton btnCambiarTema = view.findViewById(R.id.btnCambiarTema);
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            btnCambiarTema.setText("Cambiar a modo claro");
        } else {
            btnCambiarTema.setText("Cambiar a modo oscuro");
        }
    }

    private void loadUserProfile() {
        SharedPreferences sessionPrefs = requireContext().getSharedPreferences("SivarEatsPrefs", Context.MODE_PRIVATE);
        String userEmail = sessionPrefs.getString("CURRENT_USER_EMAIL", null);

        if (userEmail == null || userEmail.isEmpty()) {
            showDefaultProfile("Usuario no encontrado", "No disponible", "No disponible");
            return;
        }

        db.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded()) {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("telefono");
                            String imageUrl = documentSnapshot.getString("profile_image_url");
                            updateUI(name, email, phone, imageUrl);
                        } else {
                            loadFromRoomAndSync(userEmail);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error al cargar Firestore", e);
                    loadFromRoomAndSync(userEmail);
                });
    }

    private void loadFromRoomAndSync(String userEmail) {
        new Thread(() -> {
            User localUser = roomDb.userDao().findUserByEmail(userEmail);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (localUser != null) {
                        updateUI(localUser.getName(), localUser.getEmail(), localUser.getTelefono(), localUser.getProfileImageUrl());
                        syncUserToFirestore(localUser);
                    } else {
                        showDefaultProfile("Usuario no encontrado", "No disponible", "No disponible");
                    }
                });
            }
        }).start();
    }

    private void syncUserToFirestore(User localUser) {
        if (localUser == null || localUser.getEmail() == null) return;

        db.collection("users").document(localUser.getEmail()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", localUser.getName());
                        userData.put("email", localUser.getEmail());
                        userData.put("alias", localUser.getAlias());
                        userData.put("telefono", localUser.getTelefono());
                        userData.put("profile_image_url", localUser.getProfileImageUrl());
                        userData.put("rol", localUser.getRol() != null ? localUser.getRol() : "USUARIO_NORMAL");
                        userData.put("createdAt", FieldValue.serverTimestamp());
                        userData.put("lastLoginAt", FieldValue.serverTimestamp());

                        db.collection("users").document(localUser.getEmail())
                                .set(userData, SetOptions.merge());
                    } else {
                        updateMissingFields(documentSnapshot, localUser);
                    }
                });
    }

    private void updateMissingFields(DocumentSnapshot document, User localUser) {
        Map<String, Object> updates = new HashMap<>();
        boolean needsUpdate = false;

        if (!document.contains("alias") || document.getString("alias") == null) {
            updates.put("alias", localUser.getAlias());
            needsUpdate = true;
        }
        if (!document.contains("telefono") || document.getString("telefono") == null) {
            updates.put("telefono", localUser.getTelefono());
            needsUpdate = true;
        }
        if (!document.contains("profile_image_url") || document.getString("profile_image_url") == null) {
            updates.put("profile_image_url", localUser.getProfileImageUrl());
            needsUpdate = true;
        }

        if (needsUpdate) {
            db.collection("users").document(localUser.getEmail()).update(updates);
        }
    }

    private void updateUI(String name, String email, String phone, String imageUrl) {
        if (!isAdded()) return;
        tvProfileName.setText(name != null ? name : "Nombre no disponible");
        tvProfileEmail.setText(email != null ? "Correo: " + email : "Correo no disponible");
        tvProfilePhone.setText(phone != null ? "Teléfono: " + phone : "Teléfono no disponible");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imgPerfil);
        } else {
            imgPerfil.setImageResource(R.drawable.ic_profile);
        }
    }

    private void showDefaultProfile(String name, String email, String phone) {
        if (!isAdded()) return;
        tvProfileName.setText(name);
        tvProfileEmail.setText("Correo: " + email);
        tvProfilePhone.setText("Teléfono: " + phone);
        imgPerfil.setImageResource(R.drawable.ic_profile);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        // Actualizar texto del botón de tema
        if (getView() != null) {
            actualizarTextoBotonTema(getView());
        }
    }

    private void cambiarTema() {
        SharedPreferences themePrefs = requireContext().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        
        int newMode;
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Cambiar a modo claro
            newMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            // Cambiar a modo oscuro
            newMode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        
        // Guardar preferencia
        themePrefs.edit().putInt("night_mode", newMode).apply();
        
        // Aplicar el nuevo tema
        AppCompatDelegate.setDefaultNightMode(newMode);
        
        // Actualizar texto del botón
        if (getView() != null) {
            actualizarTextoBotonTema(getView());
        }
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finish();
    }
}
