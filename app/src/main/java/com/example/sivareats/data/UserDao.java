package com.example.sivareats.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean existsByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email AND password = :password)")
    boolean validateLogin(String email, String password);

    @Query("UPDATE users SET password = :newPassword WHERE email = :email")
    void updatePassword(String email, String newPassword);

    @Query("UPDATE users SET alias = :alias, telefono = :telefono, name = :name WHERE email = :email")
    void updateProfileDetails(String email, String name, String alias, String telefono);

    @Query("UPDATE users SET profile_image_url = :profileUrl WHERE email = :email")
    void updateProfileUrl(String email, String profileUrl);

    @Query("UPDATE users SET rol = :rol WHERE email = :email")
    void updateRol(String email, String rol);
}
