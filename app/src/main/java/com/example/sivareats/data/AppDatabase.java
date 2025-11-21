package com.example.sivareats.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.sivareats.data.cart.CartDao;
import com.example.sivareats.data.cart.CartItem;

@Database(
        entities = {
                User.class,
                Ubicacion.class,
                PaymentMethod.class,
                CartItem.class
        },
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract UbicacionDao ubicacionDao();
    public abstract PaymentMethodDao paymentMethodDao();
    public abstract CartDao cartDao();   // ← ÚNICO DAO DEL CARRITO

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "sivareats.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

