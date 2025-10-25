package com.example.sivareats.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "USUARIOS")
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String email;

    // Constructor principal que Room va a usar
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Constructor auxiliar para tu función saveUserLocal
    @Ignore
    public User(String email) {
        this.name = "";
        this.email = email;
    }
}
