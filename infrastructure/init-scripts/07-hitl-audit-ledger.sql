-- Sub-Story 3.3a: Append-only ledger recording every HITL Approve/Reject decision.
--
-- Kept in lockstep with gateway_service/src/main/resources/schema.sql, same as
-- 06-quarantine-scheme.sql — that file is what Spring validates the JPA entity
-- against (ddl-auto: validate).
CREATE TABLE IF NOT EXISTS public.hitl_audit_ledger (
    audit_id       BIGSERIAL PRIMARY KEY,
    action_url     VARCHAR(500) NOT NULL,
    timestamp      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    decision       VARCHAR(20) NOT NULL CHECK (decision IN ('Approve', 'Reject')),
    officer_id     UUID NOT NULL,
    quarantine_id  UUID NOT NULL REFERENCES public.quarantine(quarantine_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_hitl_audit_ledger_quarantine_id ON public.hitl_audit_ledger(quarantine_id);

-- Reuses the generic protect_audit_ledger() trigger function already defined in
-- 04-audit-ledger-schema.sql for metric_audit_log — same immutability policy,
-- no need for a second copy of the same RAISE EXCEPTION body.
DROP TRIGGER IF EXISTS block_updates_on_hitl_audit_ledger ON public.hitl_audit_ledger;
DROP TRIGGER IF EXISTS block_deletes_on_hitl_audit_ledger ON public.hitl_audit_ledger;

CREATE TRIGGER block_updates_on_hitl_audit_ledger
BEFORE UPDATE ON public.hitl_audit_ledger
FOR EACH ROW EXECUTE FUNCTION protect_audit_ledger();

CREATE TRIGGER block_deletes_on_hitl_audit_ledger
BEFORE DELETE ON public.hitl_audit_ledger
FOR EACH ROW EXECUTE FUNCTION protect_audit_ledger();

-- Permission Revocation (TC-OPS-02): the standard application role may only
-- INSERT/SELECT — UPDATE/DELETE are revoked at the infrastructure level as a
-- second, independent guard alongside the trigger above.
--
-- Caveat: in PostgreSQL the table owner always retains implicit DML rights
-- regardless of REVOKE — only a non-owner role is truly locked out by this
-- statement. If platform_admin is also the role that ran this migration (and
-- therefore owns the table), the BEFORE UPDATE/DELETE trigger above is the
-- guarantee that actually holds for every role, including the owner; this
-- REVOKE is defense-in-depth for any additional non-owner application roles.
--
-- "platform_admin" matches gateway_service's default ${DB_USER} (application.yml);
-- if a deployment overrides DB_USER to a different role (e.g. docker-compose's
-- neondb_owner against the hosted Neon instance), substitute that role name here.
REVOKE UPDATE, DELETE ON public.hitl_audit_ledger FROM platform_admin;
