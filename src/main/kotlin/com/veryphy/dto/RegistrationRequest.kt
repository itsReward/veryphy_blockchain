package com.veryphy.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * User registration request DTO
 */
data class RegistrationRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    val username: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
    )
    val password: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String,

    @field:NotBlank(message = "Full name is required")
    val fullName: String,

    // Optional fields based on user role
    val universityId: Long? = null,
    val employerId: String? = null
)