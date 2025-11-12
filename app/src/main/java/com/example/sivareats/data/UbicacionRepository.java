package com.example.sivareats.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.Executors;

public class UbicacionRepository {
    private final UbicacionDao ubicacionDao;
    private final FirebaseFirestore firestore;

    public UbicacionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        ubicacionDao = db.ubicacionDao();
        firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Ubicacion>> obtenerTodas() {
        return ubicacionDao.obtenerTodas();
    }

    public void insertar(Context context, Ubicacion ubicacion) {
        Executors.newSingleThreadExecutor().execute(() -> {
            ubicacionDao.insertar(ubicacion);
            if (tieneInternet(context)) {
                firestore.collection("ubicaciones")
                        .add(ubicacion);
            }
        });
    }

    public void actualizar(Ubicacion ubicacion) {
        Executors.newSingleThreadExecutor().execute(() -> {
            ubicacionDao.actualizar(ubicacion);
            // podrías actualizar Firestore si llevas id remoto (implementar más adelante)
        });
    }

    public void eliminar(Ubicacion ubicacion) {
        Executors.newSingleThreadExecutor().execute(() -> ubicacionDao.eliminar(ubicacion));
    }

    private boolean tieneInternet(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}