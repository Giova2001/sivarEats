package com.example.sivareats.model;

public enum UserType {
    USUARIO_NORMAL("user"),
    REPARTIDOR("delivery"),
    RESTAURANTE("restaurant"),
    ADMIN("admin");

    private final String value;

    UserType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return USUARIO_NORMAL;
        }
        
        // Mapear valores de Firestore a UserType
        String normalizedValue = value.toUpperCase();
        if (normalizedValue.equals("USUARIO_NORMAL") || normalizedValue.equals("USER")) {
            return USUARIO_NORMAL;
        } else if (normalizedValue.equals("REPARTIDOR") || normalizedValue.equals("DELIVERY")) {
            return REPARTIDOR;
        } else if (normalizedValue.equals("RESTAURANTE") || normalizedValue.equals("RESTAURANT")) {
            return RESTAURANTE;
        } else if (normalizedValue.equals("ADMIN")) {
            return ADMIN;
        }
        
        // Intentar mapeo directo
        for (UserType type : UserType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return USUARIO_NORMAL; // Default
    }
}