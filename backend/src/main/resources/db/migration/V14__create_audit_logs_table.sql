CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID,
    actor_email VARCHAR(255),
    action_type VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255),
    entity_type VARCHAR(255),
    details TEXT,
    ip_address VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
