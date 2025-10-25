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
        for (UserType type : UserType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return USUARIO_NORMAL; // Default
    }
}