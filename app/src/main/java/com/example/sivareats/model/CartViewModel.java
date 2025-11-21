package com.example.sivareats.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.sivareats.data.cart.CartItem;
import com.example.sivareats.data.cart.CartRepository;

import java.util.List;

public class CartViewModel extends AndroidViewModel {

    private final CartRepository repository;
    private final LiveData<List<CartItem>> carrito;

    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = new CartRepository(application);
        carrito = repository.getAllLive();
    }

    public LiveData<List<CartItem>> getCarrito() {
        return carrito;
    }

    public void agregar(CartItem item) {
        repository.addItem(item);
    }

    public void actualizar(CartItem item) {
        repository.updateItem(item);
    }

    public void eliminar(CartItem item) {
        repository.deleteItem(item);
    }

    public void limpiarCarrito() {
        repository.clearCart();
    }
}
