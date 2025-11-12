package com.example.sivareats.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.sivareats.data.PaymentMethod;


import java.util.List;

@Dao
public interface PaymentMethodDao {
    
    @Insert
    long insert(PaymentMethod paymentMethod);
    
    @Update
    void update(PaymentMethod paymentMethod);
    
    @Delete
    void delete(PaymentMethod paymentMethod);
    
    @Query("SELECT * FROM payment_methods")
    List<PaymentMethod> getAll();
    
    @Query("SELECT * FROM payment_methods WHERE id = :id LIMIT 1")
    PaymentMethod getById(long id);
    
    @Query("SELECT * FROM payment_methods WHERE is_default = 1 LIMIT 1")
    PaymentMethod getDefault();
    
    @Query("UPDATE payment_methods SET is_default = 0")
    void clearDefault();
    
    @Query("UPDATE payment_methods SET is_default = 1 WHERE id = :id")
    void setDefault(long id);
}

