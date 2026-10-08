package com.financeapp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base abstract User entity representing an authenticated platform account.
 */
public abstract class User {

    private Long id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean active = true;
    private int failedLogins = 0;
    private LocalDateTime lockedUntil;
    private Long advisorId;
    private LocalDateTime createdAt;

    protected User() {
    }

    protected User(Long id, String name, String email, String passwordHash, Role role,
                   boolean active, int failedLogins, LocalDateTime lockedUntil,
                   Long advisorId, LocalDateTime createdAt) {
        setId(id);
        setName(name);
        setEmail(email);
        setPasswordHash(passwordHash);
        setRole(role);
        setActive(active);
        setFailedLogins(failedLogins);
        setLockedUntil(lockedUntil);
        setAdvisorId(advisorId);
        setCreatedAt(createdAt);
    }

    /**
     * Resolves the role-specific landing dashboard URL path.
     *
     * @return the dashboard path string
     */
    public abstract String getDashboardPath();

    /**
     * Factory helper to instantiate concrete User subtype based on Role.
     */
    public static User createWithRole(Role role) {
        if (role == null) {
            return new RegularUser();
        }
        return switch (role) {
            case ADMIN -> new Admin();
            case ADVISOR -> new Advisor();
            case USER -> new RegularUser();
        };
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be empty");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.email = cleanEmail;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        this.passwordHash = passwordHash.trim();
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = Objects.requireNonNullElse(role, Role.USER);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getFailedLogins() {
        return failedLogins;
    }

    public void setFailedLogins(int failedLogins) {
        if (failedLogins < 0) {
            throw new IllegalArgumentException("Failed logins count cannot be negative");
        }
        this.failedLogins = failedLogins;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Long getAdvisorId() {
        return advisorId;
    }

    public void setAdvisorId(Long advisorId) {
        this.advisorId = advisorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id) || Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : email);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
