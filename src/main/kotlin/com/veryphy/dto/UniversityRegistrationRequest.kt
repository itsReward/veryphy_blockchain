package com.veryphy.dto

import java.math.BigDecimal

/**
 * University registration request
 */
data class UniversityRegistrationRequest(
    val name: String,
    val email: String,
    val address: String? = null,
    val stakeAmount: BigDecimal
)