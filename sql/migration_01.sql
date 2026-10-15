-- ============================================================
-- Personal Finance Management Platform - Database Migration 01
-- ============================================================
-- Adds: goals, advice, messages, feedback tables and budget source column
-- ============================================================

USE `financeapp`;

-- 1. Goals Table
CREATE TABLE IF NOT EXISTS `goals` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `target_amount` DECIMAL(12,2) NOT NULL,
    `current_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `target_date` DATE NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_goals_user` FOREIGN KEY (`user_id`)
        REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Advice Table
CREATE TABLE IF NOT EXISTS `advice` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `plan_id` BIGINT NULL,
    `rule_name` VARCHAR(100) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `explanation` TEXT NOT NULL,
    `status` ENUM('NEW', 'ACCEPTED', 'IGNORED') NOT NULL DEFAULT 'NEW',
    `advisor_comment` TEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_advice_user` FOREIGN KEY (`user_id`)
        REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_advice_plan` FOREIGN KEY (`plan_id`)
        REFERENCES `spending_plans` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Messages Table
CREATE TABLE IF NOT EXISTS `messages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` BIGINT NOT NULL,
    `receiver_id` BIGINT NOT NULL,
    `body` TEXT NOT NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_messages_sender` FOREIGN KEY (`sender_id`)
        REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_messages_receiver` FOREIGN KEY (`receiver_id`)
        REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Feedback Table
CREATE TABLE IF NOT EXISTS `feedback` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `subject` VARCHAR(200) NOT NULL,
    `body` TEXT NOT NULL,
    `status` ENUM('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    `admin_reply` TEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`)
        REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Add source column to budgets table (AI_PLAN or MANUAL)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'source');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE `budgets` ADD COLUMN `source` VARCHAR(20) NOT NULL DEFAULT \'MANUAL\'', 'SELECT 1');
PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
