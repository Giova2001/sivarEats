package com.example.sivareats.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfilePhone, tvProfileEmail;
    private ImageView imgPerfil;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private SharedPreferences sharedPreferences;
    private AppDatabase roomDb;
    private static final String PREF_NAME = "loginPrefs";

    public ProfileFragment() {
        // Constructor vacío
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Inicialización
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        roomDb = AppDatabase.getInstance(requireContext());

        // Referencias UI
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        imgPerfil = view.findViewById(R.id.imgPerfil);

        // Configurar botones
        setupButtons(view);

        // Cargar perfil
        loadUserProfile();

        return view;
    }

    private void setupButtons(View view) {
        view.findViewById(R.id.btnEditarPerfil).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

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

        view.findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        String userEmail = null;

        if (currentUser != null && currentUser.getEmail() != null) {
            userEmail = currentUser.getEmail();
        } else {
            userEmail = sharedPreferences.getString("email", null);
            if (userEmail == null) {
                userEmail = sharedPreferences.getString("last_logged_email", null);
            }
        }

        if (userEmail == null || userEmail.isEmpty()) {
            showDefaultProfile("Usuario no encontrado", "No disponible", "No disponible");
            return;
        }

        final String finalEmail = userEmail;
        DocumentReference docRef = db.collection("users").document(finalEmail);

        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded()) {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("telefono");
                            String imageUrl = documentSnapshot.getString("profile_image_url");

                            updateUI(name, email, phone, imageUrl);
                        } else {
                            loadFromRoom(finalEmail);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error al cargar Firestore", e);
                    loadFromRoom(finalEmail);
                });
    }

    private void loadFromRoom(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) return;

        new Thread(() -> {
            User localUser = roomDb.userDao().findByEmail(userEmail);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (localUser != null) {
                        updateUI(localUser.getName(), localUser.getEmail(), localUser.getTelefono(), localUser.getProfileImageUrl());
                    } else {
                        showDefaultProfile("Usuario no encontrado", "No disponible", "No disponible");
                    }
                });
            }
        }).start();
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
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (currentUser != null && currentUser.getEmail() != null) {
            editor.putBoolean("biometric_enabled_" + currentUser.getEmail(), false);
        }
        editor.clear();
        editor.apply();

        if (mAuth != null) mAuth.signOut();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finish();
    }
}
