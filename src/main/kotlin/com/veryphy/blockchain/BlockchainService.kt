package com.veryphy.blockchain

import com.veryphy.model.Degree
import com.veryphy.model.University
import com.veryphy.model.VerificationRequest
import java.math.BigDecimal

interface BlockchainService {
    fun registerUniversity(university: University): String
    fun registerDegree(degree: Degree): String
    fun verifyDegree(degreeHash: String): VerificationResultDto
    fun recordVerification(verificationRequest: VerificationRequest): String
    fun queryDegreeHistory(degreeId: String): List<DegreeHistoryDto>
    fun revokeDegree(degreeId: String, reason: String): String
    fun blacklistUniversity(universityId: String, reason: String): String

    // Data transfer objects
    data class UniversityDto(
        val id: String,
        val name: String,
        val email: String,
        val stakeAmount: BigDecimal,
        val status: String
    )

    data class DegreeDto(
        val id: String,
        val studentId: String,
        val studentName: String,
        val degreeName: String,
        val universityId: String,
        val issueDate: String,
        val degreeHash: String,
        val status: String
    )

    data class VerificationDto(
        val id: String,
        val degreeId: String,
        val employerId: String,
        val requestDate: String,
        val result: String,
        val paymentAmount: BigDecimal,
        val paymentStatus: String
    )

    data class VerificationResultDto(
        val isValid: Boolean,
        val degreeId: String?,
        val universityId: String?,
        val issueDate: String?,
        val status: String?,
        val message: String?
    )

    data class DegreeHistoryDto(
        val txId: String,
        val timestamp: String,
        val degreeId: String,
        val status: String,
        val action: String
    )
}