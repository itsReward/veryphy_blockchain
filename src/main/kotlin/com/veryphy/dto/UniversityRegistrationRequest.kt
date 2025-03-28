package com.veryphy.dto

/**
 * University registration request
 */
data class UniversityRegistrationRequest(
    val name: String,
    val email: String,
    val address: String? = null,
    val stakeAmount: Double
)