-- ============================================================
-- Personal Finance Management Platform - MySQL 8 Seed Data
-- ============================================================

USE `finance_db`;

-- Clear existing data in reverse dependency order
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `audit_logs`;
TRUNCATE TABLE `notifications`;
TRUNCATE TABLE `budgets`;
TRUNCATE TABLE `plan_items`;
TRUNCATE TABLE `spending_plans`;
TRUNCATE TABLE `expenses`;
TRUNCATE TABLE `fixed_expenses`;
TRUNCATE TABLE `income`;
TRUNCATE TABLE `categories`;
TRUNCATE TABLE `system_settings`;
TRUNCATE TABLE `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Seed Users (1 Admin, 1 Advisor, 2 Users with BCrypt hashes)
-- Passwords:
-- Admin:    AdminPassword123!   -> $2a$10$htdjpO4It8oNPfOGPnzXe.ebyQ2znSERLUqhfzw.4SqvHberNARiu
-- Advisor:  AdvisorPassword123! -> $2a$10$g/y/wX0NbW39d5uSXJOkvesj.DDddQsK9AmEfRCM5FETSSJXQYZDy
-- Users:    UserPassword123!    -> $2a$10$jdJCA.ME491j/egT51eX5.Cfncuer6h5rdOXoQARAczZb3F7Q/mBa
INSERT INTO `users` (`id`, `name`, `email`, `password_hash`, `role`, `active`, `failed_logins`, `locked_until`, `advisor_id`, `created_at`)
VALUES
(1, 'System Administrator', 'admin@financeapp.com', '$2a$10$htdjpO4It8oNPfOGPnzXe.ebyQ2znSERLUqhfzw.4SqvHberNARiu', 'ADMIN', TRUE, 0, NULL, NULL, NOW()),
(2, 'Senior Financial Advisor', 'advisor@financeapp.com', '$2a$10$g/y/wX0NbW39d5uSXJOkvesj.DDddQsK9AmEfRCM5FETSSJXQYZDy', 'ADVISOR', TRUE, 0, NULL, NULL, NOW()),
(3, 'John Doe', 'john@example.com', '$2a$10$jdJCA.ME491j/egT51eX5.Cfncuer6h5rdOXoQARAczZb3F7Q/mBa', 'USER', TRUE, 0, NULL, 2, NOW()),
(4, 'Jane Smith', 'jane@example.com', '$2a$10$jdJCA.ME491j/egT51eX5.Cfncuer6h5rdOXoQARAczZb3F7Q/mBa', 'USER', TRUE, 0, NULL, NULL, NOW());

-- 2. Seed Categories
INSERT INTO `categories` (`id`, `name`, `description`, `is_system`, `created_at`)
VALUES
(1, 'Food', 'Groceries, supermarkets, restaurants, and daily dining', TRUE, NOW()),
(2, 'Savings', 'Long-term savings, wealth management, and mutual investments', TRUE, NOW()),
(3, 'Emergency', 'Liquid contingency reserves for urgent medical or unforeseen situations', TRUE, NOW()),
(4, 'Entertainment', 'Streaming subscriptions, leisure, games, dining out, and events', TRUE, NOW()),
(5, 'Other', 'General miscellaneous items, personal care, and discretionary costs', TRUE, NOW());

-- 3. Seed System Settings (Including default AI planner weights: 40/20/15/10/15)
INSERT INTO `system_settings` (`setting_key`, `setting_value`, `description`, `updated_at`)
VALUES
('weight_Food', '40', 'Default AI Smart Planner allocation percentage for Food', NOW()),
('weight_Savings', '20', 'Default AI Smart Planner allocation percentage for Savings', NOW()),
('weight_Emergency', '15', 'Default AI Smart Planner allocation percentage for Emergency fund', NOW()),
('weight_Entertainment', '10', 'Default AI Smart Planner allocation percentage for Entertainment', NOW()),
('weight_Other', '15', 'Default AI Smart Planner allocation percentage for Other expenses', NOW()),
('planner_default_weights', '{"Food":40,"Savings":20,"Emergency":15,"Entertainment":10,"Other":15}', 'Combined JSON mapping of category weights', NOW()),
('max_failed_logins', '5', 'Number of failed attempts before account lockout occurs', NOW()),
('lockout_duration_minutes', '15', 'Duration in minutes for account lockout period', NOW()),
('default_alert_threshold', '80.00', 'Default percentage threshold to trigger budget alert notifications', NOW());

-- 4. Sample Income for John Doe (User 3)
INSERT INTO `income` (`id`, `user_id`, `source`, `amount`, `frequency`, `income_date`, `notes`, `created_at`)
VALUES
(1, 3, 'Primary Salary - Software Engineering', 5000.00, 'MONTHLY', '2026-10-01', 'Direct deposit paycheck', NOW()),
(2, 3, 'Freelance Tech Consulting', 800.00, 'ONE_TIME', '2026-10-05', 'Website maintenance contract', NOW());

-- 5. Sample Fixed Expenses for John Doe (User 3)
INSERT INTO `fixed_expenses` (`id`, `user_id`, `category_id`, `title`, `amount`, `due_day`, `is_active`, `notes`, `created_at`)
VALUES
(1, 3, 5, 'Apartment Rent', 1500.00, 1, TRUE, 'Monthly apartment lease', NOW()),
(2, 3, 5, 'Utilities & Fiber Internet', 200.00, 15, TRUE, 'Electric, water, 1Gbps internet', NOW()),
(3, 3, 4, 'Health & Dental Insurance', 300.00, 10, TRUE, 'Monthly health cover', NOW());

-- 6. Sample Daily Expenses for John Doe (User 3)
INSERT INTO `expenses` (`id`, `user_id`, `category_id`, `amount`, `expense_date`, `description`, `payment_method`, `created_at`)
VALUES
(1, 3, 1, 142.50, '2026-10-02', 'Weekly Organic Supermarket Groceries', 'DEBIT_CARD', NOW()),
(2, 3, 1, 35.80, '2026-10-04', 'Team Lunch at Downtown Bistro', 'CREDIT_CARD', NOW()),
(3, 3, 4, 18.99, '2026-10-05', 'Streaming & Music Subscription', 'CREDIT_CARD', NOW()),
(4, 3, 5, 45.00, '2026-10-06', 'Public Transit Commuter Card Reload', 'CASH', NOW());

-- 7. Sample Spending Plan for John Doe for 2026-10
-- Total Income = 5800.00, Total Fixed = 2000.00, Disposable Income = 3800.00
-- Distribution with 40/20/15/10/15:
-- Food: 40% = 1520.00
-- Savings: 20% = 760.00
-- Emergency: 15% = 570.00
-- Entertainment: 10% = 380.00
-- Other: 15% = 570.00
INSERT INTO `spending_plans` (`id`, `user_id`, `plan_month`, `total_income`, `total_fixed_expenses`, `disposable_income`, `status`, `created_at`, `updated_at`)
VALUES
(1, 3, '2026-10', 5800.00, 2000.00, 3800.00, 'ACTIVE', NOW(), NOW());

INSERT INTO `plan_items` (`id`, `plan_id`, `category_id`, `allocated_amount`, `weight_percentage`, `notes`, `created_at`)
VALUES
(1, 1, 1, 1520.00, 40.00, 'AI Plan: 40% allocated for Food & Groceries', NOW()),
(2, 1, 2, 760.00, 20.00, 'AI Plan: 20% allocated for Wealth & Savings', NOW()),
(3, 1, 3, 570.00, 15.00, 'AI Plan: 15% allocated for Emergency Reserve', NOW()),
(4, 1, 4, 380.00, 10.00, 'AI Plan: 10% allocated for Entertainment', NOW()),
(5, 1, 5, 570.00, 15.00, 'AI Plan: 15% allocated for Discretionary Other', NOW());

-- 8. Sample Budgets for John Doe (User 3)
INSERT INTO `budgets` (`id`, `user_id`, `category_id`, `budget_month`, `budget_limit`, `alert_threshold`, `created_at`)
VALUES
(1, 3, 1, '2026-10', 1500.00, 80.00, NOW()),
(2, 3, 4, '2026-10', 350.00, 80.00, NOW());

-- 9. Sample Notifications
INSERT INTO `notifications` (`id`, `user_id`, `title`, `message`, `type`, `is_read`, `created_at`)
VALUES
(1, 3, 'Welcome to AI Smart Spending Planner', 'Your account has been initialized. Complete your income and fixed expenses to activate automated spending planning.', 'INFO', FALSE, NOW()),
(2, 3, 'Advisor Connected', 'Senior Financial Advisor has been assigned to assist you with your financial goals.', 'ADVICE', FALSE, NOW());

-- 10. Sample Audit Log
INSERT INTO `audit_logs` (`id`, `user_id`, `action`, `entity_type`, `entity_id`, `details`, `ip_address`, `created_at`)
VALUES
(1, 1, 'SYSTEM_INITIALIZATION', 'SYSTEM', NULL, 'Database seeded with default categories and administrative accounts', '127.0.0.1', NOW());
