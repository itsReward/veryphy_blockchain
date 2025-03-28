package com.veryphy.dto

import com.veryphy.model.UserRole

/**
 * Login request
 */
data class LoginRequest(
    val username: String,
    val password: String,
    val role: UserRole
)