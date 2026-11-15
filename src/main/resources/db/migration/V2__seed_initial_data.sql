-- ==============================================================================
-- FLYWAY V2: INITIAL ROLES
-- ==============================================================================

INSERT INTO roles (id, name) VALUES (1, 'ROLE_CUSTOMER')
    ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO roles (id, name) VALUES (2, 'ROLE_ADMIN')
    ON DUPLICATE KEY UPDATE name = VALUES(name);
