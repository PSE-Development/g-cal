package com.gcal.ui.screens.profile

/**
 * FriendRequest — Legacy data class for friend request display.
 *
 *
 * @param id              Unique identifier for the friend request.
 * @param fromUsername     The sender's username.
 * @param fromDisplayName  The sender's display name.
 * @param timestamp        When the request was sent (epoch millis).
 */
data class FriendRequest(
    val id: String,
    val fromUsername: String,
    val fromDisplayName: String,
    val timestamp: Long = System.currentTimeMillis()
)