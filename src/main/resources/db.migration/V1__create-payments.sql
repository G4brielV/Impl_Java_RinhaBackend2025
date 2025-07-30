CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    correlation_id VARCHAR(100),
    amount DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_default BOOLEAN DEFAULT TRUE
);
