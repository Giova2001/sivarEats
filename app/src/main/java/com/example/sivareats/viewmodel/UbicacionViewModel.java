package com.example.sivareats.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.sivareats.data.Ubicacion;
import com.example.sivareats.data.UbicacionRepository;

import java.util.List;

public class UbicacionViewModel extends AndroidViewModel {

    private final UbicacionRepository repository;
    private final LiveData<List<Ubicacion>> ubicaciones;

    public UbicacionViewModel(@NonNull Application application) {
        super(application);
        repository = new UbicacionRepository(application);
        ubicaciones = repository.obtenerTodas();
    }

    public LiveData<List<Ubicacion>> obtenerTodas() {
        return ubicaciones;
    }

    public void insertar(Ubicacion ubicacion) {
        repository.insertar(getApplication().getApplicationContext(), ubicacion);
    }

    public void actualizar(Ubicacion ubicacion) {
        repository.actualizar(ubicacion);
    }

    public void eliminar(Ubicacion ubicacion) {
        repository.eliminar(ubicacion);
    }

    public void desmarcarTodasPreferidas() {
        repository.desmarcarTodasPreferidas();
    }

    public void marcarComoPreferida(Ubicacion ubicacion) {
        repository.marcarComoPreferida(ubicacion);
    }
}
