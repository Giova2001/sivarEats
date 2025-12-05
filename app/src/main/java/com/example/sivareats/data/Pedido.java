package com.example.sivareats.data;

import java.util.Date;
import java.util.List;

public class Pedido {
    private String id;
    private String restaurante;
    private String direccion;
    private double total;
    private String estado; // "activo", "completado"
    private Date fecha;
    private List<Producto> productos;
    private String repartidorNombre;
    private String repartidorTelefono;
    private int tiempoEstimado; // en minutos
    private String clienteEmail; // Email del cliente (para pedidos de restaurante)

    public Pedido(String id, String restaurante, String direccion, double total, 
                  String estado, Date fecha, List<Producto> productos) {
        this.id = id;
        this.restaurante = restaurante;
        this.direccion = direccion;
        this.total = total;
        this.estado = estado;
        this.fecha = fecha;
        this.productos = productos;
    }

    // Getters
    public String getId() { return id; }
    public String getRestaurante() { return restaurante; }
    public String getDireccion() { return direccion; }
    public double getTotal() { return total; }
    public String getEstado() { return estado; }
    public Date getFecha() { return fecha; }
    public List<Producto> getProductos() { return productos; }
    public String getRepartidorNombre() { return repartidorNombre; }
    public String getRepartidorTelefono() { return repartidorTelefono; }
    public int getTiempoEstimado() { return tiempoEstimado; }
    public String getClienteEmail() { return clienteEmail; }

    // Setters
    public void setRepartidorNombre(String repartidorNombre) { 
        this.repartidorNombre = repartidorNombre; 
    }
    public void setRepartidorTelefono(String repartidorTelefono) { 
        this.repartidorTelefono = repartidorTelefono; 
    }
    public void setTiempoEstimado(int tiempoEstimado) { 
        this.tiempoEstimado = tiempoEstimado; 
    }
    public void setEstado(String estado) { 
        this.estado = estado; 
    }
    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }
}




