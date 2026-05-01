ALTER TABLE bizcord_users ADD COLUMN IF NOT EXISTS enabled boolean NOT NULL DEFAULT true;
