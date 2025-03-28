package com.veryphy.dto

import java.util.*

/**
 * System statistics response
 */
data class SystemStatsResponse(
    val registeredUniversities: Int,
    val activeUniversities: Int,
    val totalDegrees: Int,
    val verificationCount: Int,
    val successRate: Double,
    val timestamp: Date
)