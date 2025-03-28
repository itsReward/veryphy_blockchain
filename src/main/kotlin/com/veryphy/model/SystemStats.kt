package com.veryphy.model

/**
 * System statistics
 */
data class SystemStats(
    val registeredUniversities: Int,
    val activeUniversities: Int,
    val totalDegrees: Int,
    val verificationCount: Int,
    val timestamp: String
)