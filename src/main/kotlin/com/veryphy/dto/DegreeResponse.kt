package com.veryphy.dto

import com.veryphy.model.DegreeStatus

/**
 * Degree response model
 */
data class DegreeResponse(
    val id: String,
    val studentId: String,
    val studentName: String,
    val degreeName: String,
    val universityId: String,
    val universityName: String,
    val issueDate: String,
    val degreeHash: String,
    val status: DegreeStatus
)