package com.example.sivareats.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchHistoryManager {

    private static final String PREF_NAME = "search_history_prefs";
    private static final String KEY_HISTORY = "search_history";

    private final SharedPreferences prefs;

    public SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Guardar una nueva búsqueda
    public void saveSearch(String query) {
        List<String> currentHistory = getHistory();

        // Evita duplicados
        if (currentHistory.contains(query)) {
            currentHistory.remove(query);
        }

        // Agregar arriba
        currentHistory.add(0, query);

        // Limitar a 10 elementos
        if (currentHistory.size() > 10) {
            currentHistory = currentHistory.subList(0, 10);
        }

        // Guardar
        prefs.edit().putString(KEY_HISTORY, String.join(";", currentHistory)).apply();
    }

    // Obtener historial
    public List<String> getHistory() {
        String saved = prefs.getString(KEY_HISTORY, "");
        if (saved.isEmpty()) return new ArrayList<>();

        return new ArrayList<>(Arrays.asList(saved.split(";")));
    }

    // Borrar historial
    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
