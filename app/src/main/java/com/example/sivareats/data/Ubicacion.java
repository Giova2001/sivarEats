package com.example.sivareats.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "ubicaciones")
public class Ubicacion implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String nombreLugar;
    private String direccion;
    private String tipo;
    private String depto;
    private String descripcion;
    private boolean preferida;
    private String coordenadas;
    private String idRemoto; // ID del documento en Firestore

    // Constructor vacío (requerido por Room)
    public Ubicacion() {}

    // Constructor completo
    public Ubicacion(String nombreLugar, String direccion, String tipo, String depto,
                     String descripcion, boolean preferida) {
        this.nombreLugar = nombreLugar;
        this.direccion = direccion;
        this.tipo = tipo;
        this.depto = depto;
        this.descripcion = descripcion;
        this.preferida = preferida;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreLugar() { return nombreLugar; }
    public void setNombreLugar(String nombreLugar) { this.nombreLugar = nombreLugar; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDepto() { return depto; }
    public void setDepto(String depto) { this.depto = depto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isPreferida() { return preferida; }
    public void setPreferida(boolean preferida) { this.preferida = preferida; }

    public String getCoordenadas() { return coordenadas; }
    public void setCoordenadas(String coordenadas) { this.coordenadas = coordenadas; }

    public String getIdRemoto() { return idRemoto; }
    public void setIdRemoto(String idRemoto) { this.idRemoto = idRemoto; }
}
