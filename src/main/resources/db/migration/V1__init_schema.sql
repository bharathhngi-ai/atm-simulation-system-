-- ==============================================================================
-- FLYWAY V1: INITIAL DATABASE SCHEMA FOR ATM SIMULATION & SMART BANKING
-- ==============================================================================

-- 1. Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(200),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    lock_time DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. User Roles Join Table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bank Accounts Table
CREATE TABLE IF NOT EXISTS bank_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    atm_pin VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    daily_withdrawal_limit DECIMAL(19, 2) NOT NULL DEFAULT 50000.00,
    daily_transfer_limit DECIMAL(19, 2) NOT NULL DEFAULT 100000.00,
    frozen_reason VARCHAR(255),
    frozen_at DATETIME,
    user_id BIGINT NOT NULL UNIQUE,
    version BIGINT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_account_number (account_number),
    INDEX idx_account_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_number VARCHAR(36) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL,
    target_account_id BIGINT,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    balance_before DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    description VARCHAR(255),
    atm_location VARCHAR(100),
    ip_address VARCHAR(50),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_txn_account FOREIGN KEY (account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_txn_target_account FOREIGN KEY (target_account_id) REFERENCES bank_accounts (id) ON DELETE SET NULL,
    INDEX idx_tx_ref (reference_number),
    INDEX idx_tx_account (account_id),
    INDEX idx_tx_created_at (created_at),
    INDEX idx_tx_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Emergency Requests Table
CREATE TABLE IF NOT EXISTS emergency_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_code VARCHAR(36) NOT NULL UNIQUE,
    requester_account_id BIGINT NOT NULL,
    target_account_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_note VARCHAR(255),
    transaction_reference VARCHAR(36),
    expires_at DATETIME NOT NULL,
    decided_at DATETIME,
    version BIGINT DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_er_requester FOREIGN KEY (requester_account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_er_target FOREIGN KEY (target_account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    INDEX idx_er_requester (requester_account_id),
    INDEX idx_er_target (target_account_id),
    INDEX idx_er_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Trusted Contacts Table
CREATE TABLE IF NOT EXISTS trusted_contacts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_account_id BIGINT NOT NULL,
    contact_account_id BIGINT NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    relationship VARCHAR(50),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_tc_owner FOREIGN KEY (owner_account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_tc_contact FOREIGN KEY (contact_account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT uk_owner_contact_account UNIQUE (owner_account_id, contact_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Fraud Alerts Table
CREATE TABLE IF NOT EXISTS fraud_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_code VARCHAR(36) NOT NULL UNIQUE,
    account_id BIGINT,
    user_id BIGINT,
    rule_triggered VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution_notes VARCHAR(500),
    resolved_by VARCHAR(50),
    resolved_at DATETIME,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_fa_account FOREIGN KEY (account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_fa_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_fa_account (account_id),
    INDEX idx_fa_status (status),
    INDEX idx_fa_severity (severity),
    INDEX idx_fa_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(20) DEFAULT 'INFO',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    link VARCHAR(150),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    action VARCHAR(40) NOT NULL,
    entity_name VARCHAR(50),
    entity_id VARCHAR(50),
    details VARCHAR(1000),
    ip_address VARCHAR(50),
    user_agent VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    created_at DATETIME NOT NULL,
    INDEX idx_audit_username (username),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Idempotency Records Table
CREATE TABLE IF NOT EXISTS idempotency_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    operation_type VARCHAR(50) NOT NULL,
    user_identifier VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    result_message VARCHAR(255),
    created_at DATETIME NOT NULL,
    INDEX idx_idem_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
