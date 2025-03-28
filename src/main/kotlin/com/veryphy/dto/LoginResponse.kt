package com.veryphy.dto

import com.veryphy.model.User

/**
 * Login response
 */
data class LoginResponse(
    val token: String,
    val user: User
)