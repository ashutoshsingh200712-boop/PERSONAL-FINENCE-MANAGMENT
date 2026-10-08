package com.financeapp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Spending and budget classification category.
 */
public class Category {

    private Long id;
    private String name;
    private String description;
    private boolean system = true;
    private LocalDateTime createdAt;

    public Category() {
    }

    public Category(Long id, String name, String description, boolean system, LocalDateTime createdAt) {
        setId(id);
        setName(name);
        setDescription(description);
        setSystem(system);
        setCreatedAt(createdAt);
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
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        this.name = name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
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
        if (!(o instanceof Category category)) return false;
        return Objects.equals(id, category.id) || Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : name);
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", system=" + system +
                '}';
    }
}
