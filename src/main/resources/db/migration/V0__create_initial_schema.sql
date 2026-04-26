CREATE TABLE bizcord_users (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    email varchar(255) UNIQUE,
    full_name varchar(255),
    image_url varchar(255),
    password varchar(255),
    status varchar(255) NOT NULL DEFAULT 'OFFLINE',
    updated_at timestamp(6),
    username varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_servers (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    image_url varchar(255),
    invite_code varchar(255) UNIQUE,
    invite_expires_at timestamp(6),
    name varchar(255),
    updated_at timestamp(6),
    user_id varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_channel_categories (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    default_category boolean NOT NULL,
    name varchar(255),
    position integer NOT NULL,
    updated_at timestamp(6),
    server_id varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_channels (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    name varchar(255),
    position integer NOT NULL,
    type varchar(255) CHECK (type IN ('TEXT', 'VOICE')),
    updated_at timestamp(6),
    category_id varchar(255),
    server_id varchar(255),
    user_id varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_conversations (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    updated_at timestamp(6),
    user1_id varchar(255),
    user2_id varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_conversation_deleted_by (
    conversation_id varchar(255) NOT NULL,
    user_id varchar(255)
);

CREATE TABLE bizcord_direct_messages (
    id varchar(255) NOT NULL,
    attachments varchar(255) array,
    content varchar(255),
    created_at timestamp(6),
    deleted boolean NOT NULL,
    pinned_at timestamp(6),
    updated_at timestamp(6),
    conversation_id varchar(255),
    parent_message_id varchar(255),
    pinned_by_id varchar(255),
    user_id varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_members (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    name varchar(255),
    role varchar(255) CHECK (role IN ('ADMIN', 'MODERATOR', 'GUEST')),
    updated_at timestamp(6),
    server_id varchar(255),
    user_id varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_messages (
    id varchar(255) NOT NULL,
    attachments varchar(255) array,
    content varchar(255),
    created_at timestamp(6),
    deleted boolean NOT NULL,
    pinned_at timestamp(6),
    updated_at timestamp(6),
    channel_id varchar(255),
    member_id varchar(255),
    parent_message_id varchar(255),
    pinned_by_id varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_events (
    id varchar(255) NOT NULL,
    color varchar(255),
    created_at timestamp(6),
    description text,
    end_time timestamp(6),
    location varchar(255),
    start_time timestamp(6),
    title varchar(255),
    updated_at timestamp(6),
    creator_id varchar(255) NOT NULL,
    server_id varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_event_attendees (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    status varchar(255) NOT NULL CHECK (status IN ('GOING', 'MAYBE', 'NOT_GOING')),
    event_id varchar(255) NOT NULL,
    user_id varchar(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (event_id, user_id)
);

CREATE TABLE bizcord_notifications (
    id varchar(255) NOT NULL,
    body text,
    channel_id varchar(255),
    created_at timestamp(6),
    is_read boolean NOT NULL,
    reference_id varchar(255),
    reference_type varchar(255) CHECK (reference_type IN ('MESSAGE', 'EVENT', 'SERVER', 'CHANNEL')),
    server_id varchar(255),
    title varchar(255) NOT NULL,
    type varchar(255) NOT NULL CHECK (type IN ('MENTION', 'DIRECT_MESSAGE', 'EVENT_CREATED', 'EVENT_REMINDER', 'RSVP_UPDATE')),
    user_id varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE bizcord_reactions (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    emoji varchar(255) NOT NULL,
    direct_message_id varchar(255),
    message_id varchar(255),
    user_id varchar(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (emoji, user_id, message_id),
    UNIQUE (emoji, user_id, direct_message_id)
);

CREATE TABLE bizcord_banned_users (
    id varchar(255) NOT NULL,
    created_at timestamp(6),
    banned_by_id varchar(255),
    server_id varchar(255) NOT NULL,
    user_id varchar(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (server_id, user_id)
);

CREATE TABLE refresh_tokens (
    id varchar(255) NOT NULL,
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone NOT NULL,
    revoked boolean NOT NULL,
    token varchar(255) NOT NULL UNIQUE,
    user_id varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS bizcord_servers
    ADD CONSTRAINT fk_bizcord_servers_user FOREIGN KEY (user_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_channel_categories
    ADD CONSTRAINT fk_bizcord_channel_categories_server FOREIGN KEY (server_id) REFERENCES bizcord_servers;

ALTER TABLE IF EXISTS bizcord_channels
    ADD CONSTRAINT fk_bizcord_channels_category FOREIGN KEY (category_id) REFERENCES bizcord_channel_categories;

ALTER TABLE IF EXISTS bizcord_channels
    ADD CONSTRAINT fk_bizcord_channels_server FOREIGN KEY (server_id) REFERENCES bizcord_servers;

ALTER TABLE IF EXISTS bizcord_channels
    ADD CONSTRAINT fk_bizcord_channels_user FOREIGN KEY (user_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_conversations
    ADD CONSTRAINT fk_bizcord_conversations_user1 FOREIGN KEY (user1_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_conversations
    ADD CONSTRAINT fk_bizcord_conversations_user2 FOREIGN KEY (user2_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_conversation_deleted_by
    ADD CONSTRAINT fk_bizcord_conversation_deleted_by_conversation FOREIGN KEY (conversation_id) REFERENCES bizcord_conversations;

ALTER TABLE IF EXISTS bizcord_direct_messages
    ADD CONSTRAINT fk_bizcord_direct_messages_conversation FOREIGN KEY (conversation_id) REFERENCES bizcord_conversations;

ALTER TABLE IF EXISTS bizcord_direct_messages
    ADD CONSTRAINT fk_bizcord_direct_messages_parent FOREIGN KEY (parent_message_id) REFERENCES bizcord_direct_messages;

ALTER TABLE IF EXISTS bizcord_direct_messages
    ADD CONSTRAINT fk_bizcord_direct_messages_pinned_by FOREIGN KEY (pinned_by_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_direct_messages
    ADD CONSTRAINT fk_bizcord_direct_messages_user FOREIGN KEY (user_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_members
    ADD CONSTRAINT fk_bizcord_members_server FOREIGN KEY (server_id) REFERENCES bizcord_servers;

ALTER TABLE IF EXISTS bizcord_members
    ADD CONSTRAINT fk_bizcord_members_user FOREIGN KEY (user_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_messages
    ADD CONSTRAINT fk_bizcord_messages_channel FOREIGN KEY (channel_id) REFERENCES bizcord_channels;

ALTER TABLE IF EXISTS bizcord_messages
    ADD CONSTRAINT fk_bizcord_messages_member FOREIGN KEY (member_id) REFERENCES bizcord_members;

ALTER TABLE IF EXISTS bizcord_messages
    ADD CONSTRAINT fk_bizcord_messages_parent FOREIGN KEY (parent_message_id) REFERENCES bizcord_messages;

ALTER TABLE IF EXISTS bizcord_messages
    ADD CONSTRAINT fk_bizcord_messages_pinned_by FOREIGN KEY (pinned_by_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_events
    ADD CONSTRAINT fk_bizcord_events_creator FOREIGN KEY (creator_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_events
    ADD CONSTRAINT fk_bizcord_events_server FOREIGN KEY (server_id) REFERENCES bizcord_servers;

ALTER TABLE IF EXISTS bizcord_event_attendees
    ADD CONSTRAINT fk_bizcord_event_attendees_event FOREIGN KEY (event_id) REFERENCES bizcord_events;

ALTER TABLE IF EXISTS bizcord_event_attendees
    ADD CONSTRAINT fk_bizcord_event_attendees_user FOREIGN KEY (user_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_notifications
    ADD CONSTRAINT fk_bizcord_notifications_user FOREIGN KEY (user_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_reactions
    ADD CONSTRAINT fk_bizcord_reactions_direct_message FOREIGN KEY (direct_message_id) REFERENCES bizcord_direct_messages;

ALTER TABLE IF EXISTS bizcord_reactions
    ADD CONSTRAINT fk_bizcord_reactions_message FOREIGN KEY (message_id) REFERENCES bizcord_messages;

ALTER TABLE IF EXISTS bizcord_reactions
    ADD CONSTRAINT fk_bizcord_reactions_user FOREIGN KEY (user_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_banned_users
    ADD CONSTRAINT fk_bizcord_banned_users_banned_by FOREIGN KEY (banned_by_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS bizcord_banned_users
    ADD CONSTRAINT fk_bizcord_banned_users_server FOREIGN KEY (server_id) REFERENCES bizcord_servers;

ALTER TABLE IF EXISTS bizcord_banned_users
    ADD CONSTRAINT fk_bizcord_banned_users_user FOREIGN KEY (user_id) REFERENCES bizcord_users;

ALTER TABLE IF EXISTS refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES bizcord_users;
