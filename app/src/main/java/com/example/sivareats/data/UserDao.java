package com.example.sivareats.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDao {

    // Insertar un usuario
    @Insert
    void insertUser(User user);

    // Obtener todos los usuarios
    @Query("SELECT * FROM usuarios")
    List<User> getAllUsers();
}
