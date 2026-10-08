package com.financeapp.model;

import java.time.LocalDateTime;

/**
 * System administrator with full access to user management, system settings, and audit logs.
 */
public class Admin extends User {

    public Admin() {
        setRole(Role.ADMIN);
    }

    public Admin(Long id, String name, String email, String passwordHash,
                 boolean active, int failedLogins, LocalDateTime lockedUntil,
                 LocalDateTime createdAt) {
        super(id, name, email, passwordHash, Role.ADMIN, active, failedLogins, lockedUntil, null, createdAt);
    }

    @Override
    public String getDashboardPath() {
        return "/admin/dashboard";
    }
}
