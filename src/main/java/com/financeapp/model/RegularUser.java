package com.financeapp.model;

import java.time.LocalDateTime;

/**
 * Standard platform user with personal budget, expense tracking, and planner access.
 */
public class RegularUser extends User {

    public RegularUser() {
        setRole(Role.USER);
    }

    public RegularUser(Long id, String name, String email, String passwordHash,
                       boolean active, int failedLogins, LocalDateTime lockedUntil,
                       Long advisorId, LocalDateTime createdAt) {
        super(id, name, email, passwordHash, Role.USER, active, failedLogins, lockedUntil, advisorId, createdAt);
    }

    @Override
    public String getDashboardPath() {
        return "/user/dashboard";
    }
}
