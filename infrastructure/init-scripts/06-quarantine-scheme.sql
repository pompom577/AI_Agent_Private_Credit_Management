-- Create Quarantine Holding Pen Table for Suspicious AI Agent Payloads
--
-- Kept in lockstep with gateway_service/src/main/resources/schema.sql — that
-- file is the one Spring actually validates the JPA entity against
-- (ddl-auto: validate), so this migration mirrors its table name and UUID
-- primary key exactly rather than defining a divergent "quarantine_zone"
-- with a SERIAL id.
CREATE TABLE IF NOT EXISTS public.quarantine (
    quarantine_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    endpoint VARCHAR(500) NOT NULL,
    payload JSONB NOT NULL,
    agent_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'Pending',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for high-performance dashboard queue lookups
CREATE INDEX IF NOT EXISTS idx_quarantine_status ON public.quarantine(status);
