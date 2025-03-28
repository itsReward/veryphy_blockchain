package com.veryphy.model

/**
 * User entity for authentication
 */
data class User(
    val id: String,
    val username: String,
    val email: String,
    val role: com.veryphy.model.UserRole,
    val entityId: String? = null // Reference to university, employer or admin entity
)