package com.veryphy.dto

import com.tukio.veryphy.model.UniversityStatus

/**
 * University response for admin API
 */
data class UniversityResponse(
    val id: String,
    val name: String,
    val email: String,
    val address: String? = null,
    val stakeAmount: Double,
    val status: UniversityStatus,
    val joinDate: String
)