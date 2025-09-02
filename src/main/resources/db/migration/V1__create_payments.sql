CREATE UNLOGGED TABLE payments (
    correlation_id VARCHAR(100) PRIMARY KEY,
    amount DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_default BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_payments_created_at ON payments(created_at);