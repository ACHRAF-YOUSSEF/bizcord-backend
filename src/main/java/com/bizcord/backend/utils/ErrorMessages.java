package com.bizcord.backend.utils;

public final class ErrorMessages {
    private ErrorMessages() {
    }

    // User
    public static final String USER_NOT_FOUND = "User not found";

    // Auth
    public static final String AUTH_INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String AUTH_REFRESH_TOKEN_REVOKED = "Refresh token has been revoked";
    public static final String AUTH_REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
    public static final String AUTH_ACCOUNT_NOT_ACTIVATED = "ACCOUNT_NOT_ACTIVATED";
    public static final String AUTH_INVALID_VERIFICATION_TOKEN = "Invalid or expired verification token";
    public static final String AUTH_VERIFICATION_TOKEN_EXPIRED = "Verification link has expired. Please request a new one.";
    public static final String AUTH_VERIFICATION_TOKEN_USED = "This verification link has already been used.";
    public static final String AUTH_ALREADY_VERIFIED = "Account is already verified";
    public static final String AUTH_INVALID_RESET_TOKEN = "Invalid or expired password reset token";

    // Server
    public static final String SERVER_NOT_FOUND = "Server not found";
    public static final String SERVER_UPDATE_FORBIDDEN = "Only server owner can update this server";
    public static final String SERVER_DELETE_FORBIDDEN = "Only server owner can delete this server";
    public static final String SERVER_INVITE_REGEN_FORBIDDEN = "Only server owner can regenerate the invite code";
    public static final String SERVER_INVALID_INVITE_CODE = "Invalid invite code";
    public static final String SERVER_ALREADY_MEMBER = "Already a member of this server";
    public static final String SERVER_INVITE_EXPIRED = "This invite link has expired";
    public static final String SERVER_OWNER_LEAVE_FORBIDDEN = "Server owner cannot leave their own server";
    public static final String SERVER_INVALID_IMAGE_URL = "Invalid server image URL";

    // Member
    public static final String NOT_A_MEMBER = "You are not a member of this server";
    public static final String MEMBER_NOT_FOUND = "Member not found";
    public static final String MEMBER_NOT_IN_SERVER = "Member not found in this server";
    public static final String MEMBER_SELF_ROLE_CHANGE = "You cannot change your own role";
    public static final String MEMBER_ROLE_NO_PERMISSION = "You do not have permission to change roles";
    public static final String MEMBER_MODERATOR_CANNOT_CHANGE_ADMIN = "Moderators can only change the role of guests";
    public static final String MEMBER_CANNOT_ASSIGN_ADMIN = "Cannot assign the ADMIN role";
    public static final String MEMBER_SELF_KICK = "You cannot kick yourself";
    public static final String MEMBER_KICK_NO_PERMISSION = "You do not have permission to kick members";
    public static final String MEMBER_MODERATOR_CANNOT_KICK_ADMIN = "Moderators cannot kick the admin";
    public static final String MEMBER_SELF_BAN = "You cannot ban yourself";
    public static final String MEMBER_BAN_NO_PERMISSION = "You do not have permission to ban members";
    public static final String MEMBER_MODERATOR_CANNOT_BAN_ADMIN = "Moderators cannot ban the admin";
    public static final String MEMBER_ALREADY_BANNED = "This user is already banned";
    public static final String MEMBER_NOT_BANNED = "This user is not banned";
    public static final String MEMBER_UNBAN_NO_PERMISSION = "You do not have permission to unban members";
    public static final String MEMBER_BANNED_FROM_SERVER = "You are banned from this server";

    // Conversation
    public static final String CONVERSATION_NOT_FOUND = "Conversation not found";
    public static final String CONVERSATION_NOT_A_PARTICIPANT = "You are not a participant of this conversation";
    public static final String CONVERSATION_SELF_NOT_ALLOWED = "You cannot start a conversation with yourself";

    // Direct Message
    public static final String DIRECT_MESSAGE_NOT_FOUND = "Message not found";
    public static final String DIRECT_MESSAGE_NOT_OWNER = "You can only modify your own messages";
    public static final String DIRECT_MESSAGE_EMPTY = "Message must have content or attachments";

    // Message (channel)
    public static final String MESSAGE_NOT_FOUND = "Message not found";
    public static final String MESSAGE_EMPTY = "Message must have content or attachments";
    public static final String MESSAGE_NOT_OWNER = "You can only edit your own messages";
    public static final String MESSAGE_DELETE_FORBIDDEN = "Only the message author, an admin, or a moderator can delete messages";

    // Channel
    public static final String CHANNEL_NOT_FOUND = "Channel not found";
    public static final String CHANNEL_WRONG_SERVER = "Channel does not belong to this server";
    public static final String CHANNEL_GENERAL_DELETE = "The 'general' channel cannot be deleted";
    public static final String CHANNEL_GENERAL_EDIT = "The 'general' channel cannot be edited";
    public static final String CHANNEL_GENERAL_RENAME = "Cannot rename a channel to 'general'";
    public static final String CHANNEL_CREATE_FORBIDDEN = "You do not have permission to create channels";
    public static final String CHANNEL_EDIT_FORBIDDEN = "You do not have permission to edit channels";
    public static final String CHANNEL_DELETE_FORBIDDEN = "You do not have permission to delete channels";

    // Category
    public static final String CATEGORY_NOT_FOUND = "Category not found";
    public static final String CATEGORY_WRONG_SERVER = "Category does not belong to this server";
    public static final String CATEGORY_CREATE_FORBIDDEN = "You do not have permission to create categories";
    public static final String CATEGORY_EDIT_FORBIDDEN = "You do not have permission to edit categories";
    public static final String CATEGORY_DELETE_FORBIDDEN = "You do not have permission to delete categories";
    public static final String CATEGORY_DEFAULT_DELETE = "Cannot delete the default category";
    public static final String CATEGORY_LAST_DELETE = "Cannot delete the last category";
    public static final String CATEGORY_REORDER_FORBIDDEN = "You do not have permission to reorder categories";

    // Event
    public static final String EVENT_NOT_FOUND = "Event not found";
    public static final String EVENT_WRONG_SERVER = "Event does not belong to this server";
    public static final String EVENT_CREATE_FORBIDDEN = "You do not have permission to create events";
    public static final String EVENT_EDIT_FORBIDDEN = "Only the event creator or an admin can edit events";
    public static final String EVENT_DELETE_FORBIDDEN = "Only the event creator or an admin can delete events";
    public static final String EVENT_INVALID_TIME = "End time must be after start time";

    // Notification
    public static final String NOTIFICATION_NOT_FOUND = "Notification not found";
    public static final String NOTIFICATION_FORBIDDEN = "You do not have access to this notification";
}
