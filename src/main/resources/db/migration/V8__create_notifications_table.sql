CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    type VARCHAR(10) NOT NULL,
    event VARCHAR(50) NOT NULL,
    recipient VARCHAR(200) NOT NULL,
    subject VARCHAR(500),
    body TEXT,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
