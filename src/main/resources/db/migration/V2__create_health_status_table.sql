CREATE TABLE IF NOT EXISTS processor_health_status (
    lock_id INT PRIMARY KEY,
    preferred_processor VARCHAR(10) NOT NULL,
    last_checked_at TIMESTAMPTZ NOT NULL
);


INSERT INTO processor_health_status (lock_id, preferred_processor, last_checked_at)
VALUES (1, 'DEFAULT', NOW())
ON CONFLICT (lock_id) DO NOTHING;