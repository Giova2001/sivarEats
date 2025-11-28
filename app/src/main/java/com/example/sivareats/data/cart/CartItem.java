package com.example.sivareats.data.cart;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "cart_items")
public class CartItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String nombre;
    private String descripcion;
    private int imageResId;
    private double precio;
    private int cantidad;
    private String restaurante; // Nombre del restaurante

    // Constructor principal para Room (con restaurante)
    public CartItem(String nombre, String descripcion, int imageResId, double precio, int cantidad, String restaurante) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imageResId = imageResId;
        this.precio = precio;
        this.cantidad = cantidad;
        this.restaurante = restaurante != null ? restaurante : "";
    }
    
    // Constructor sin restaurante (marcado como @Ignore para Room)
    @Ignore
    public CartItem(String nombre, String descripcion, int imageResId, double precio, int cantidad) {
        this(nombre, descripcion, imageResId, precio, cantidad, "");
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    
    public String getRestaurante() { return restaurante; }
    public void setRestaurante(String restaurante) { this.restaurante = restaurante != null ? restaurante : ""; }
}
