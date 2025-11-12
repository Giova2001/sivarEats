package com.example.sivareats.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UbicacionDao {
    @Insert
    void insertar(Ubicacion ubicacion);

    @Update
    void actualizar(Ubicacion ubicacion);

    @Delete
    void eliminar(Ubicacion ubicacion);

    @Query("SELECT * FROM ubicaciones")
    LiveData<List<Ubicacion>> obtenerTodas();

    @Query("SELECT * FROM ubicaciones WHERE id = :id LIMIT 1")
    Ubicacion obtenerPorId(int id);
}