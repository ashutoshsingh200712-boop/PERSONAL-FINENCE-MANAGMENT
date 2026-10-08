package com.financeapp.dao;

import com.financeapp.model.Category;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for spending category classification.
 */
public interface CategoryDAO {

    long create(Category category);
    long create(Connection conn, Category category);

    Optional<Category> findById(long id);
    Optional<Category> findByName(String name);
    List<Category> findAll();

    boolean update(Category category);
    boolean update(Connection conn, Category category);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
