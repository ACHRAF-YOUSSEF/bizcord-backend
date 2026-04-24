ALTER TABLE bizcord_users
    ADD COLUMN IF NOT EXISTS preferred_status varchar(255) NOT NULL DEFAULT 'ONLINE';

ALTER TABLE bizcord_users
    ADD CONSTRAINT bizcord_users_preferred_status_check
        CHECK (preferred_status IN ('ONLINE', 'IDLE', 'DO_NOT_DISTURB', 'INVISIBLE', 'OFFLINE'));
