ALTER TABLE bizcord_conversations
    ADD COLUMN IF NOT EXISTS request_status varchar(255) NOT NULL DEFAULT 'ACCEPTED';

ALTER TABLE bizcord_conversations
    ADD COLUMN IF NOT EXISTS requester_id varchar(255);

CREATE TABLE IF NOT EXISTS bizcord_friendships (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    updated_at timestamp(6),
    requester_id varchar(255) NOT NULL,
    addressee_id varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bizcord_friendships_pair UNIQUE (requester_id, addressee_id),
    CONSTRAINT fk_bizcord_friendships_requester FOREIGN KEY (requester_id) REFERENCES bizcord_users,
    CONSTRAINT fk_bizcord_friendships_addressee FOREIGN KEY (addressee_id) REFERENCES bizcord_users
);

ALTER TABLE IF EXISTS bizcord_conversations
    ADD CONSTRAINT fk_bizcord_conversations_requester FOREIGN KEY (requester_id) REFERENCES bizcord_users;
