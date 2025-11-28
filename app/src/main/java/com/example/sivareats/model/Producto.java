package com.example.sivareats.model;

public class Producto {
    private String nombre;
    private String descripcion;
    private int imagenResId;
    private String imagenUrl; // URL de imagen desde Firestore/Cloudinary
    private double precio;
    private int cantidad;
    private String categoria; // "restaurantes", "china", "pizza", "favoritos", "todos"
    private String restaurante; // "Pollo Campero", "Pizza Hut", "Burger King", "China Wok"

    public Producto(String nombre, String descripcion, int imagenResId, double precio, int cantidad) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagenResId = imagenResId;
        this.precio = precio;
        this.cantidad = cantidad;
        this.categoria = "todos";
        this.restaurante = "";
    }

    public Producto(String nombre, String descripcion, int imagenResId, double precio, int cantidad, String categoria) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagenResId = imagenResId;
        this.precio = precio;
        this.cantidad = cantidad;
        this.categoria = categoria != null ? categoria : "todos";
        this.restaurante = ""; // Por defecto
    }

    public Producto(String nombre, String descripcion, int imagenResId, double precio, int cantidad, String categoria, String restaurante) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagenResId = imagenResId;
        this.imagenUrl = null;
        this.precio = precio;
        this.cantidad = cantidad;
        this.categoria = categoria != null ? categoria : "todos";
        this.restaurante = restaurante != null ? restaurante : "";
    }
    
    // Constructor para productos desde Firestore con URL de imagen
    public Producto(String nombre, String descripcion, String imagenUrl, double precio, int cantidad, String categoria, String restaurante) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagenResId = 0; // Sin resource ID
        this.imagenUrl = imagenUrl;
        this.precio = precio;
        this.cantidad = cantidad;
        this.categoria = categoria != null ? categoria : "todos";
        this.restaurante = restaurante != null ? restaurante : "";
    }

    // Getters
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getImagenResId() { return imagenResId; }
    public String getImagenUrl() { return imagenUrl; }
    public double getPrecio() { return precio; }
    public int getCantidad() { return cantidad; }
    public String getCategoria() { return categoria; }
    public String getRestaurante() { return restaurante; }

    // Setters
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setRestaurante(String restaurante) { this.restaurante = restaurante; }
}