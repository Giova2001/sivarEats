package com.example.sivareats.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritosManager {

    private static final String PREF_NAME = "favoritos_prefs";
    private static final String KEY_FAVORITOS = "favoritos_list";

    private final SharedPreferences prefs;

    public FavoritosManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Agregar un producto a favoritos (por nombre)
    public void agregarFavorito(String nombreProducto) {
        Set<String> favoritos = getFavoritosSet();
        favoritos.add(nombreProducto);
        guardarFavoritos(favoritos);
    }

    // Remover un producto de favoritos
    public void removerFavorito(String nombreProducto) {
        Set<String> favoritos = getFavoritosSet();
        favoritos.remove(nombreProducto);
        guardarFavoritos(favoritos);
    }

    // Verificar si un producto es favorito
    public boolean esFavorito(String nombreProducto) {
        Set<String> favoritos = getFavoritosSet();
        return favoritos.contains(nombreProducto);
    }

    // Obtener lista de favoritos
    public List<String> getFavoritos() {
        Set<String> favoritosSet = getFavoritosSet();
        return new ArrayList<>(favoritosSet);
    }

    // Obtener Set de favoritos desde SharedPreferences
    private Set<String> getFavoritosSet() {
        Set<String> favoritos = prefs.getStringSet(KEY_FAVORITOS, null);
        if (favoritos == null) {
            return new HashSet<>();
        }
        // Crear una copia mutable
        return new HashSet<>(favoritos);
    }

    // Guardar favoritos en SharedPreferences
    private void guardarFavoritos(Set<String> favoritos) {
        prefs.edit().putStringSet(KEY_FAVORITOS, favoritos).apply();
    }

    // Limpiar todos los favoritos
    public void limpiarFavoritos() {
        prefs.edit().remove(KEY_FAVORITOS).apply();
    }
}