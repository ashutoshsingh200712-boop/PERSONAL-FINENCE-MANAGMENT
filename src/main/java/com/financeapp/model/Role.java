package com.financeapp.model;

/**
 * Platform user security roles.
 */
public enum Role {
    USER,
    ADVISOR,
    ADMIN;

    public static Role fromString(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return USER;
        }
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
