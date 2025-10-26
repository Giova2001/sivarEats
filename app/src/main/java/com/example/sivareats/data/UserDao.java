package com.example.sivareats.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User user); // Falla si el email ya existe (por índice único)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
   User findByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean existsByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email AND password = :password)")
    boolean validateLogin(String email, String password);
}
