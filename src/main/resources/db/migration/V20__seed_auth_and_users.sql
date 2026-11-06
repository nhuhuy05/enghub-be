-- Migration: V20__seed_auth_and_users.sql
-- Description: Seed default admin and test user accounts

-- Table: users (3 rows)
INSERT INTO users (id, email, phone, full_name, avatar_url, provider, provider_id, created_at, password, is_active)
VALUES
  (1, 'admin@gmail.com', NULL, 'Administrator', NULL, NULL, NULL, '2026-05-31T01:39:44.653405', '$2a$10$3EeZonePbOKyEJnhCwEE2eCqqa.6T1NUvhx3u3xC0uszGvk0tCTcG', true),
  (4, 'huy@gmail.com', '0374233466', 'Nguyễn Huy', NULL, NULL, NULL, '2026-05-31T20:07:01.002606', '$2a$10$P.ih3hmOwZJ8Is9rNJsU7u2hWwoKLWmtB8Q4XqVyw1X3trdtg.AAG', true),
  (5, 'phat@gmail.com', NULL, 'Phát', NULL, NULL, NULL, '2026-06-03T21:43:18.532575', '$2a$10$NvEoDwQgSpWCCVJ4aARDgeoFZlTx7ZVLyFSypMN4pdYOwHFeJSKNm', true)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), (SELECT MAX(id) IS NOT NULL FROM users));

-- Table: user_roles (1 rows)
INSERT INTO user_roles (user_id, role_id)
VALUES
  (1, 3)
ON CONFLICT (user_id, role_id) DO NOTHING;

