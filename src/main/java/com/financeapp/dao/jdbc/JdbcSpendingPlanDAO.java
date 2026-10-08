package com.financeapp.dao.jdbc;

import com.financeapp.dao.SpendingPlanDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.PlanItem;
import com.financeapp.model.SpendingPlan;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of SpendingPlanDAO, managing master spending plans and their related plan items.
 */
public class JdbcSpendingPlanDAO implements SpendingPlanDAO {

    private static final String SQL_INSERT_PLAN =
            "INSERT INTO spending_plans (user_id, plan_month, total_income, total_fixed_expenses, disposable_income, status, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_ITEM =
            "INSERT INTO plan_items (plan_id, category_id, allocated_amount, weight_percentage, notes, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, user_id, plan_month, total_income, total_fixed_expenses, disposable_income, status, created_at, updated_at " +
            "FROM spending_plans WHERE id = ?";

    private static final String SQL_FIND_BY_USER_MONTH =
            "SELECT id, user_id, plan_month, total_income, total_fixed_expenses, disposable_income, status, created_at, updated_at " +
            "FROM spending_plans WHERE user_id = ? AND plan_month = ?";

    private static final String SQL_FIND_BY_USER =
            "SELECT id, user_id, plan_month, total_income, total_fixed_expenses, disposable_income, status, created_at, updated_at " +
            "FROM spending_plans WHERE user_id = ? ORDER BY plan_month DESC";

    private static final String SQL_FIND_ITEMS_BY_PLAN =
            "SELECT pi.id, pi.plan_id, pi.category_id, c.name AS category_name, pi.allocated_amount, pi.weight_percentage, pi.notes, pi.created_at " +
            "FROM plan_items pi LEFT JOIN categories c ON pi.category_id = c.id WHERE pi.plan_id = ? ORDER BY pi.allocated_amount DESC";

    private static final String SQL_UPDATE_PLAN =
            "UPDATE spending_plans SET total_income = ?, total_fixed_expenses = ?, disposable_income = ?, status = ?, updated_at = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE_ITEMS_BY_PLAN =
            "DELETE FROM plan_items WHERE plan_id = ?";

    private static final String SQL_DELETE_PLAN =
            "DELETE FROM spending_plans WHERE id = ?";

    @Override
    public long create(SpendingPlan plan) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                long id = create(conn, plan);
                conn.commit();
                return id;
            } catch (Exception e) {
                DBConnection.rollbackQuietly(conn);
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create spending plan: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, SpendingPlan plan) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_PLAN, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, plan.getUserId());
            stmt.setString(2, plan.getPlanMonth());
            stmt.setBigDecimal(3, plan.getTotalIncome());
            stmt.setBigDecimal(4, plan.getTotalFixedExpenses());
            stmt.setBigDecimal(5, plan.getDisposableIncome());
            stmt.setString(6, plan.getStatus().name());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime createdAt = plan.getCreatedAt() != null ? plan.getCreatedAt() : now;
            stmt.setTimestamp(7, Timestamp.valueOf(createdAt));
            stmt.setTimestamp(8, Timestamp.valueOf(now));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating spending plan failed, no rows affected.");
            }

            long planId;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    planId = keys.getLong(1);
                    plan.setId(planId);
                    plan.setCreatedAt(createdAt);
                    plan.setUpdatedAt(now);
                } else {
                    throw new DataAccessException("Creating spending plan failed, no ID obtained.");
                }
            }

            // Insert plan items
            if (plan.getItems() != null && !plan.getItems().isEmpty()) {
                try (PreparedStatement itemStmt = conn.prepareStatement(SQL_INSERT_ITEM, Statement.RETURN_GENERATED_KEYS)) {
                    for (PlanItem item : plan.getItems()) {
                        itemStmt.setLong(1, planId);
                        itemStmt.setLong(2, item.getCategoryId());
                        itemStmt.setBigDecimal(3, item.getAllocatedAmount());
                        itemStmt.setBigDecimal(4, item.getWeightPercentage());
                        itemStmt.setString(5, item.getNotes());
                        LocalDateTime itemCreatedAt = item.getCreatedAt() != null ? item.getCreatedAt() : now;
                        itemStmt.setTimestamp(6, Timestamp.valueOf(itemCreatedAt));
                        itemStmt.executeUpdate();

                        try (ResultSet itemKeys = itemStmt.getGeneratedKeys()) {
                            if (itemKeys.next()) {
                                item.setId(itemKeys.getLong(1));
                                item.setPlanId(planId);
                                item.setCreatedAt(itemCreatedAt);
                            }
                        }
                    }
                }
            }

            return planId;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert spending plan with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<SpendingPlan> findById(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find spending plan by id: " + id, e);
        }
    }

    public Optional<SpendingPlan> findById(Connection conn, long id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SpendingPlan plan = mapRowToPlan(rs);
                    plan.setItems(findItemsForPlan(conn, plan.getId()));
                    return Optional.of(plan);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SpendingPlan> findByUserAndMonth(long userId, String month) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_MONTH)) {
            stmt.setLong(1, userId);
            stmt.setString(2, month);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SpendingPlan plan = mapRowToPlan(rs);
                    plan.setItems(findItemsForPlan(conn, plan.getId()));
                    return Optional.of(plan);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find spending plan for month: " + month, e);
        }
        return Optional.empty();
    }

    @Override
    public List<SpendingPlan> findByUserId(long userId) {
        List<SpendingPlan> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SpendingPlan plan = mapRowToPlan(rs);
                    plan.setItems(findItemsForPlan(conn, plan.getId()));
                    list.add(plan);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find spending plans for user: " + userId, e);
        }
        return list;
    }

    @Override
    public boolean update(SpendingPlan plan) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean success = update(conn, plan);
                conn.commit();
                return success;
            } catch (Exception e) {
                DBConnection.rollbackQuietly(conn);
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update spending plan: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Connection conn, SpendingPlan plan) {
        try {
            LocalDateTime now = LocalDateTime.now();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_PLAN)) {
                stmt.setBigDecimal(1, plan.getTotalIncome());
                stmt.setBigDecimal(2, plan.getTotalFixedExpenses());
                stmt.setBigDecimal(3, plan.getDisposableIncome());
                stmt.setString(4, plan.getStatus().name());
                stmt.setTimestamp(5, Timestamp.valueOf(now));
                stmt.setLong(6, plan.getId());

                int affected = stmt.executeUpdate();
                if (affected == 0) {
                    return false;
                }
            }

            // Replace items
            try (PreparedStatement deleteItems = conn.prepareStatement(SQL_DELETE_ITEMS_BY_PLAN)) {
                deleteItems.setLong(1, plan.getId());
                deleteItems.executeUpdate();
            }

            if (plan.getItems() != null && !plan.getItems().isEmpty()) {
                try (PreparedStatement itemStmt = conn.prepareStatement(SQL_INSERT_ITEM, Statement.RETURN_GENERATED_KEYS)) {
                    for (PlanItem item : plan.getItems()) {
                        itemStmt.setLong(1, plan.getId());
                        itemStmt.setLong(2, item.getCategoryId());
                        itemStmt.setBigDecimal(3, item.getAllocatedAmount());
                        itemStmt.setBigDecimal(4, item.getWeightPercentage());
                        itemStmt.setString(5, item.getNotes());
                        itemStmt.setTimestamp(6, Timestamp.valueOf(now));
                        itemStmt.executeUpdate();

                        try (ResultSet itemKeys = itemStmt.getGeneratedKeys()) {
                            if (itemKeys.next()) {
                                item.setId(itemKeys.getLong(1));
                                item.setPlanId(plan.getId());
                            }
                        }
                    }
                }
            }

            plan.setUpdatedAt(now);
            return true;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update spending plan with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete spending plan: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try {
            try (PreparedStatement delItems = conn.prepareStatement(SQL_DELETE_ITEMS_BY_PLAN)) {
                delItems.setLong(1, id);
                delItems.executeUpdate();
            }
            try (PreparedStatement delPlan = conn.prepareStatement(SQL_DELETE_PLAN)) {
                delPlan.setLong(1, id);
                return delPlan.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete spending plan with connection: " + id, e);
        }
    }

    private List<PlanItem> findItemsForPlan(Connection conn, long planId) throws SQLException {
        List<PlanItem> items = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ITEMS_BY_PLAN)) {
            stmt.setLong(1, planId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PlanItem item = new PlanItem();
                    item.setId(rs.getLong("id"));
                    item.setPlanId(rs.getLong("plan_id"));
                    item.setCategoryId(rs.getLong("category_id"));
                    item.setCategoryName(rs.getString("category_name"));
                    item.setAllocatedAmount(rs.getBigDecimal("allocated_amount"));
                    item.setWeightPercentage(rs.getBigDecimal("weight_percentage"));
                    item.setNotes(rs.getString("notes"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        item.setCreatedAt(ts.toLocalDateTime());
                    }
                    items.add(item);
                }
            }
        }
        return items;
    }

    private SpendingPlan mapRowToPlan(ResultSet rs) throws SQLException {
        SpendingPlan plan = new SpendingPlan();
        plan.setId(rs.getLong("id"));
        plan.setUserId(rs.getLong("user_id"));
        plan.setPlanMonth(rs.getString("plan_month"));
        plan.setTotalIncome(rs.getBigDecimal("total_income"));
        plan.setTotalFixedExpenses(rs.getBigDecimal("total_fixed_expenses"));
        plan.setDisposableIncome(rs.getBigDecimal("disposable_income"));
        plan.setStatus(SpendingPlan.Status.fromString(rs.getString("status")));

        Timestamp cTs = rs.getTimestamp("created_at");
        if (cTs != null) {
            plan.setCreatedAt(cTs.toLocalDateTime());
        }

        Timestamp uTs = rs.getTimestamp("updated_at");
        if (uTs != null) {
            plan.setUpdatedAt(uTs.toLocalDateTime());
        }

        return plan;
    }
}
