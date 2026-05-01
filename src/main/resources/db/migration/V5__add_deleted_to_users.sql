ALTER TABLE bizcord_users ADD COLUMN IF NOT EXISTS deleted boolean NOT NULL DEFAULT false;
