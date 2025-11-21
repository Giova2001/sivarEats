package com.example.sivareats.data.cart;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CartDao {

    @Insert
    void insert(CartItem item);

    @Update
    void update(CartItem item);

    @Delete
    void delete(CartItem item);

    @Query("DELETE FROM cart_items")
    void clearCart();

    @Query("SELECT * FROM cart_items")
    List<CartItem> getAll();

    @Query("SELECT * FROM cart_items")
    LiveData<List<CartItem>> getAllLive();

    @Query("SELECT SUM(precio * cantidad) FROM cart_items")
    Double getTotal();
}



