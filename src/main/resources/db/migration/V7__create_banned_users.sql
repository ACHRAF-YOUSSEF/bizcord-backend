CREATE TABLE IF NOT EXISTS bizcord_banned_users (
    id          varchar(255) NOT NULL,
    created_at  timestamp(6),
    banned_by_id varchar(255),
    server_id   varchar(255) NOT NULL,
    user_id     varchar(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (server_id, user_id)
);

ALTER TABLE bizcord_banned_users
    DROP CONSTRAINT IF EXISTS fk_bizcord_banned_users_server,
    DROP CONSTRAINT IF EXISTS fk_bizcord_banned_users_user,
    DROP CONSTRAINT IF EXISTS fk_bizcord_banned_users_banned_by;

ALTER TABLE bizcord_banned_users
    ADD CONSTRAINT fk_bizcord_banned_users_server   FOREIGN KEY (server_id)    REFERENCES bizcord_servers,
    ADD CONSTRAINT fk_bizcord_banned_users_user     FOREIGN KEY (user_id)      REFERENCES bizcord_users,
    ADD CONSTRAINT fk_bizcord_banned_users_banned_by FOREIGN KEY (banned_by_id) REFERENCES bizcord_users;
