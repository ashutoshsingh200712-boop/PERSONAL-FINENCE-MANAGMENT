package com.financeapp.dao.jdbc;

import com.financeapp.dao.CategoryDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.Category;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of CategoryDAO.
 */
public class JdbcCategoryDAO implements CategoryDAO {

    private static final String SQL_INSERT =
            "INSERT INTO categories (name, description, is_system, created_at) VALUES (?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, description, is_system, created_at FROM categories WHERE id = ?";

    private static final String SQL_FIND_BY_NAME =
            "SELECT id, name, description, is_system, created_at FROM categories WHERE LOWER(name) = ?";

    private static final String SQL_FIND_ALL =
            "SELECT id, name, description, is_system, created_at FROM categories ORDER BY id ASC";

    private static final String SQL_UPDATE =
            "UPDATE categories SET name = ?, description = ?, is_system = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM categories WHERE id = ?";

    @Override
    public long create(Category category) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, category);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create category: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Category category) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setBoolean(3, category.isSystem());
            LocalDateTime createdAt = category.getCreatedAt() != null ? category.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(4, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating category failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    category.setId(id);
                    category.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating category failed, no ID returned.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert category: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Category> findById(long id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find category by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Category> findByName(String name) {
        if (name == null) return Optional.empty();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_NAME)) {
            stmt.setString(1, name.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find category by name: " + name, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find all categories", e);
        }
        return list;
    }

    @Override
    public boolean update(Category category) {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, category);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update category: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Connection conn, Category category) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setBoolean(3, category.isSystem());
            stmt.setLong(4, category.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update category with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete category: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete category with connection: " + id, e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setSystem(rs.getBoolean("is_system"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            c.setCreatedAt(ts.toLocalDateTime());
        }
        return c;
    }
}
