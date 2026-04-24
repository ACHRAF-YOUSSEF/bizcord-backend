-- Extend user status check constraint to include DO_NOT_DISTURB and INVISIBLE
ALTER TABLE bizcord_users DROP CONSTRAINT IF EXISTS bizcord_users_status_check;

ALTER TABLE bizcord_users
    ADD CONSTRAINT bizcord_users_status_check
        CHECK (status IN ('ONLINE', 'IDLE', 'DO_NOT_DISTURB', 'INVISIBLE', 'OFFLINE'));
