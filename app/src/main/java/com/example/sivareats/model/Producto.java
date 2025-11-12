package com.example.sivareats.model;

public class Producto {
    private String nombre;
    private String descripcion;
    private int imagenResId; 
    private double precio;
    private int cantidad;

    public Producto(String nombre, String descripcion, int imagenResId, double precio, int cantidad) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagenResId = imagenResId;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    // Getters
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getImagenResId() { return imagenResId; }
    public double getPrecio() { return precio; }
    public int getCantidad() { return cantidad; }

    // Setters si quieres modificar cantidad
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}

