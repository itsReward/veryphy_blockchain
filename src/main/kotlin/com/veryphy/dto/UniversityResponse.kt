package com.veryphy.dto

import com.veryphy.model.UniversityStatus
import java.math.BigDecimal

/**
 * University response for admin API
 */
data class UniversityResponse(
    val id: String,
    val name: String,
    val email: String,
    val address: String? = null,
    val stakeAmount: BigDecimal,
    val status: UniversityStatus,
    val joinDate: String
)