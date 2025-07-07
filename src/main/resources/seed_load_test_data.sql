-- Seed test data to be ready for load testing.

-- Insert 100 users
CREATE TABLE IF NOT EXISTS tmp_user_ids (id UUID);

INSERT INTO user_pls (user_id, name, email, role, status, timezone)
SELECT
    random_uuid() AS user_id,
    'User ' || i,
    'user' || i || '@example.com',
    CASE MOD(i, 3)
        WHEN 0 THEN 'ADMIN'
        WHEN 1 THEN 'AGENT'
        ELSE 'USER'
        END,
    CASE MOD(i, 10)
        WHEN 0 THEN 'SUSPENDED'
        ELSE 'ACTIVE'
        END,
    'Europe/Tirane'
FROM SYSTEM_RANGE(1, 100) AS T(i);

-- Store generated user IDs for project owner use
INSERT INTO tmp_user_ids (id)
SELECT user_id FROM user_pls;

-- Insert 100 projects using random user IDs as owners
INSERT INTO project (
    project_id,
    title,
    description,
    created_at,
    updated_at,
    status,
    visibility,
    project_owner_id
)
SELECT
    random_uuid(),
    'Project ' || i,
    'Description for project ' || i,
    CURRENT_TIMESTAMP - i * INTERVAL '1' DAY,
    CURRENT_TIMESTAMP,
    CASE MOD(i, 5)
        WHEN 0 THEN 'ARCHIVED'
        ELSE 'ACTIVE'
        END,
    CASE MOD(i, 2)
        WHEN 0 THEN 'PUBLIC'
        ELSE 'PRIVATE'
        END,
    (SELECT id FROM tmp_user_ids ORDER BY RAND() LIMIT 1)
FROM SYSTEM_RANGE(1, 100) AS T(i);

-- Cleanup temp table
DROP TABLE tmp_user_ids;
