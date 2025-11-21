package com.example.sivareats.data.cart;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.sivareats.data.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CartRepository {

    private final CartDao cartDao;
    private final ExecutorService executor;

    public CartRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.cartDao = db.cartDao();   // ← NOMBRE CORRECTO
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void addItem(CartItem item) {
        executor.execute(() -> cartDao.insert(item));
    }

    public void updateItem(CartItem item) {
        executor.execute(() -> cartDao.update(item));
    }

    public void deleteItem(CartItem item) {
        executor.execute(() -> cartDao.delete(item));
    }

    public void clearCart() {
        executor.execute(cartDao::clearCart);
    }

    public LiveData<List<CartItem>> getAllLive() {
        return cartDao.getAllLive();
    }

    public void getAll(Callback<List<CartItem>> callback) {
        executor.execute(() -> {
            List<CartItem> items = cartDao.getAll();   // ← AQUÍ ESTABA EL ERROR
            callback.onResult(items);
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}