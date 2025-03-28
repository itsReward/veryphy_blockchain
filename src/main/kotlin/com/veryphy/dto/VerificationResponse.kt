package com.veryphy.dto

import java.util.*

/**
 * Verification response
 */
data class VerificationResponse(
    val id: String,
    val degreeHash: String,
    val studentId: String,
    val degreeName: String,
    val universityName: String,
    val issueDate: String,
    val verified: Boolean,
    val message: String,
    val timestamp: Date
)