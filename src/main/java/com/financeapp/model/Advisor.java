package com.financeapp.model;

import java.time.LocalDateTime;

/**
 * Financial Advisor user with privileges to monitor assigned clients and issue financial advice.
 */
public class Advisor extends User {

    public Advisor() {
        setRole(Role.ADVISOR);
    }

    public Advisor(Long id, String name, String email, String passwordHash,
                   boolean active, int failedLogins, LocalDateTime lockedUntil,
                   LocalDateTime createdAt) {
        super(id, name, email, passwordHash, Role.ADVISOR, active, failedLogins, lockedUntil, null, createdAt);
    }

    @Override
    public String getDashboardPath() {
        return "/advisor/dashboard";
    }
}
