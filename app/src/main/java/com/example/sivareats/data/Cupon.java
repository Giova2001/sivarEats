package com.example.sivareats.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Date; // Importar java.util.Date

/**
 * Entidad que representa un cupón de descuento activo
 * guardado localmente en la base de datos Room.
 */
@Entity(
        tableName = "cupones_activos",
        indices = {
                // Hacemos el código único para no guardar el mismo cupón dos veces
                @Index(value = {"codigo"}, unique = true)
        }
)
public class Cupon {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "codigo")
    public String codigo; // Ej. "WELCOMESV"

    @NonNull
    @ColumnInfo(name = "beneficio")
    public String beneficio; // Ej. "descuento 30%"

    @NonNull
    @ColumnInfo(name = "fecha_validez")
    // Room sabe cómo convertir Date a Long (milisegundos) y viceversa
    public Date fechaValidez;

    /**
     * Constructor para crear un nuevo cupón.
     */
    public Cupon(@NonNull String codigo, @NonNull String beneficio, @NonNull Date fechaValidez) {
        this.codigo = codigo;
        this.beneficio = beneficio;
        this.fechaValidez = fechaValidez;
    }
}