package com.example.blackjack

fun shouldRequestUsername(
    username: String?,
    score: Int,
    registered: Boolean = true
): Boolean = score > 0 && (username.isNullOrBlank() || !registered)

fun shouldRequestUsernameAfterRemoteLookup(
    score: Int,
    remoteProfileExists: Boolean
): Boolean = score > 0 && !remoteProfileExists

fun registrationStatusAfterWrite(
    remoteNewRecord: Boolean,
    writeSucceeded: Boolean
): Boolean? = if (writeSucceeded) remoteNewRecord else null
