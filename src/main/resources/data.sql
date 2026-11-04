-- ---------------------------------------------------------------------------
-- ROLES (capabilities are data, not code)
--   override_allowed : may act on ANY step; the action finalises the workflow
--   view_all_allowed : may view requests created by other users
-- ---------------------------------------------------------------------------
INSERT INTO roles (name, override_allowed, view_all_allowed) VALUES ('REQUESTER', FALSE, FALSE);
INSERT INTO roles (name, override_allowed, view_all_allowed) VALUES ('APPROVER',  FALSE, TRUE);
INSERT INTO roles (name, override_allowed, view_all_allowed) VALUES ('ADMIN',     TRUE,  TRUE);

-- ---------------------------------------------------------------------------
-- DEMO USERS (every password is: password123, stored as a BCrypt hash)
-- ---------------------------------------------------------------------------
INSERT INTO users (username, password, role_name) VALUES ('alice', '{bcrypt}$2a$10$ZVHsz8F6T6ijhn5zxP0PV.7EHvq.l6WZWQ6aai4nPsz1ToC/iN37W', 'REQUESTER');
INSERT INTO users (username, password, role_name) VALUES ('bob',   '{bcrypt}$2a$10$ZVHsz8F6T6ijhn5zxP0PV.7EHvq.l6WZWQ6aai4nPsz1ToC/iN37W', 'REQUESTER');
INSERT INTO users (username, password, role_name) VALUES ('carol', '{bcrypt}$2a$10$ZVHsz8F6T6ijhn5zxP0PV.7EHvq.l6WZWQ6aai4nPsz1ToC/iN37W', 'APPROVER');
INSERT INTO users (username, password, role_name) VALUES ('dave',  '{bcrypt}$2a$10$ZVHsz8F6T6ijhn5zxP0PV.7EHvq.l6WZWQ6aai4nPsz1ToC/iN37W', 'APPROVER');
INSERT INTO users (username, password, role_name) VALUES ('admin', '{bcrypt}$2a$10$ZVHsz8F6T6ijhn5zxP0PV.7EHvq.l6WZWQ6aai4nPsz1ToC/iN37W', 'ADMIN');

-- ---------------------------------------------------------------------------
-- WORKFLOW CONFIGURATION  (request_type -> ordered list of approving roles)
-- ---------------------------------------------------------------------------
-- LEAVE:   line manager (APPROVER) -> HR (ADMIN)
INSERT INTO approval_steps (request_type, step_order, role_name) VALUES ('LEAVE',   1, 'APPROVER');
INSERT INTO approval_steps (request_type, step_order, role_name) VALUES ('LEAVE',   2, 'ADMIN');

-- EXPENSE: manager (APPROVER) -> finance (APPROVER) -> admin sign-off (ADMIN)
INSERT INTO approval_steps (request_type, step_order, role_name) VALUES ('EXPENSE', 1, 'APPROVER');
INSERT INTO approval_steps (request_type, step_order, role_name) VALUES ('EXPENSE', 2, 'APPROVER');
INSERT INTO approval_steps (request_type, step_order, role_name) VALUES ('EXPENSE', 3, 'ADMIN');
