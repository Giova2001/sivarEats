package com.example.sivareats.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CuponDao {

    /**
     * Inserta un cupón. Si ya existe (mismo 'codigo'), lo reemplaza.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Cupon cupon);

    /**
     * Obtiene todos los cupones guardados, ordenados por validez.
     * (En un futuro, puedes filtrar por fecha de expiración aquí)
     */
    @Query("SELECT * FROM cupones_activos ORDER BY fecha_validez DESC")
    List<Cupon> getAllCupones();

    /**
     * Borra un cupón usando su código (ej. después de usarlo).
     */
    @Query("DELETE FROM cupones_activos WHERE codigo = :codigo")
    void deleteByCodigo(String codigo);
}