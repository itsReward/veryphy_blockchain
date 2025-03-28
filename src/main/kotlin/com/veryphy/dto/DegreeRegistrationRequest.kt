package com.veryphy.dto

/**
 * Degree registration request
 */
data class DegreeRegistrationRequest(
    val studentId: String,
    val studentName: String,
    val degreeName: String,
    val universityId: String,
    val issueDate: String,
    val transcript: Map<String, Any>? = null
)