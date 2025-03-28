package com.veryphy.controller

import com.veryphy.dto.VerificationResponse
import com.veryphy.model.VerificationResult
import com.veryphy.service.VerificationService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/verifications")
class VerificationController(private val verificationService: VerificationService) {

    @PostMapping("/hash")
    @PreAuthorize("hasRole('EMPLOYER')")
    fun verifyByHash(
        @RequestParam degreeHash: String,
        authentication: Authentication
    ): ResponseEntity<VerificationResponse> {
        val user = authentication.principal as com.veryphy.model.User
        val employerId = user.entityId ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        // Default payment amount - in a real system this would be based on pricing model
        val paymentAmount = BigDecimal(10.0)

        val verificationRequest = verificationService.createVerificationByHash(employerId, degreeHash, paymentAmount)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            verificationRequest.degree?.let {
                VerificationResponse(
                    id = verificationRequest.requestId,
                    degreeHash = it.degreeHash,
                    studentId = verificationRequest.degree.studentId,
                    degreeName = verificationRequest.degree.degreeName,
                    universityName = verificationRequest.degree.university.name,
                    issueDate = verificationRequest.degree.issueDate.toString(),
                    verified = verificationRequest.result == VerificationResult.AUTHENTIC,
                    message = verificationRequest.verificationDetails ?: "",
                    timestamp = Date()
                )
            }
        )
    }

    @PostMapping("/certificate", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasRole('EMPLOYER')")
    fun verifyByCertificate(
        @RequestParam("file") file: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<VerificationResponse> {
        val user = authentication.principal as com.veryphy.model.User
        val employerId = user.entityId ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        // Default payment amount
        val paymentAmount: BigDecimal = BigDecimal(10.0)

        val verificationRequest = verificationService.createVerificationByCertificate(
            employerId,
            file.bytes,
            paymentAmount
        )

        // If degree is null, it means verification failed
        if (verificationRequest.degree == null) {
            return ResponseEntity.ok(
                VerificationResponse(
                    id = verificationRequest.requestId,
                    degreeHash = "",
                    studentId = "",
                    degreeName = "",
                    universityName = "",
                    issueDate = "",
                    verified = false,
                    message = verificationRequest.verificationDetails ?: "Verification failed",
                    timestamp = Date()
                )
            )
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
            VerificationResponse(
                id = verificationRequest.requestId,
                degreeHash = verificationRequest.degree.degreeHash,
                studentId = verificationRequest.degree.studentId,
                degreeName = verificationRequest.degree.degreeName,
                universityName = verificationRequest.degree.university.name,
                issueDate = verificationRequest.degree.issueDate.toString(),
                verified = verificationRequest.result == VerificationResult.AUTHENTIC,
                message = verificationRequest.verificationDetails ?: "",
                timestamp = Date()
            )
        )
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    fun getVerification(@PathVariable requestId: String): ResponseEntity<VerificationResponse> {
        val verificationRequest = verificationService.getVerificationById(requestId)

        // If degree is null, it means verification failed
        if (verificationRequest.degree == null) {
            return ResponseEntity.ok(
                VerificationResponse(
                    id = verificationRequest.requestId,
                    degreeHash = "",
                    studentId = "",
                    degreeName = "",
                    universityName = "",
                    issueDate = "",
                    verified = false,
                    message = verificationRequest.verificationDetails ?: "Verification failed",
                    timestamp = Date()
                )
            )
        }

        return ResponseEntity.ok(
            VerificationResponse(
                id = verificationRequest.requestId,
                degreeHash = verificationRequest.degree.degreeHash,
                studentId = verificationRequest.degree.studentId,
                degreeName = verificationRequest.degree.degreeName,
                universityName = verificationRequest.degree.university.name,
                issueDate = verificationRequest.degree.issueDate.toString(),
                verified = verificationRequest.result == VerificationResult.AUTHENTIC,
                message = verificationRequest.verificationDetails ?: "",
                timestamp = Date(verificationRequest.requestDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
            )
        )
    }

    @GetMapping("/employer")
    @PreAuthorize("hasRole('EMPLOYER')")
    fun getEmployerVerifications(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<VerificationResponse>> {
        val user = authentication.principal as com.veryphy.model.User
        val employerId = user.entityId ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val verifications = verificationService.getVerificationsByEmployer(employerId, pageable)

        val response = verifications.map {
            mapToResponse(it)
        }.content

        return ResponseEntity.ok(response)
    }

    @GetMapping("/university/{universityId}")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN')")
    fun getUniversityVerifications(
        @PathVariable universityId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<VerificationResponse>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val verifications = verificationService.getVerificationsByUniversity(universityId, pageable)

        val response = verifications.map {
            mapToResponse(it)
        }.content

        return ResponseEntity.ok(response)
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    fun getStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = verificationService.getStatistics()
        val response = mapOf(
            "totalVerifications" to statistics.totalVerifications,
            "authenticVerifications" to statistics.authenticVerifications,
            "failedVerifications" to statistics.failedVerifications,
            "pendingVerifications" to statistics.pendingVerifications,
            "totalPaymentAmount" to statistics.totalPaymentAmount,
            "successRate" to statistics.successRate
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/statistics/university/{universityId}")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN')")
    fun getUniversityStatistics(@PathVariable universityId: Long): ResponseEntity<Map<String, Any>> {
        val statistics = verificationService.getUniversityStatistics(universityId)
        val response = mapOf(
            "universityId" to statistics.universityId,
            "totalVerifications" to statistics.totalVerifications,
            "authenticVerifications" to statistics.authenticVerifications,
            "failedVerifications" to statistics.failedVerifications,
            "successRate" to statistics.successRate
        )
        return ResponseEntity.ok(response)
    }

    // Public endpoint for verifying degrees without authentication
    @PostMapping("/public/verify")
    fun publicVerifyDegree(@RequestParam degreeHash: String): ResponseEntity<Map<String, Any>> {
        // Use a generic employer ID for public verifications
        val publicEmployerId = "PUBLIC-VERIFIER"
        val paymentAmount = BigDecimal(0.0 )// Free for public verifications

        val verificationRequest = verificationService.createVerificationByHash(publicEmployerId, degreeHash, paymentAmount)

        val result = mapOf(
            "verified" to (verificationRequest.result == VerificationResult.AUTHENTIC),
            "message" to (verificationRequest.verificationDetails ?: ""),
            "requestId" to verificationRequest.requestId
        )

        return ResponseEntity.ok(result)
    }

    private fun mapToResponse(verificationRequest: com.veryphy.model.VerificationRequest): VerificationResponse {
        // Handle case where degree might be null (failed verification)
        if (verificationRequest.degree == null) {
            return VerificationResponse(
                id = verificationRequest.requestId,
                degreeHash = "",
                studentId = "",
                degreeName = "",
                universityName = "",
                issueDate = "",
                verified = false,
                message = verificationRequest.verificationDetails ?: "Verification failed",
                timestamp = Date(verificationRequest.requestDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
            )
        }

        return VerificationResponse(
            id = verificationRequest.requestId,
            degreeHash = verificationRequest.degree.degreeHash,
            studentId = verificationRequest.degree.studentId,
            degreeName = verificationRequest.degree.degreeName,
            universityName = verificationRequest.degree.university.name,
            issueDate = verificationRequest.degree.issueDate.toString(),
            verified = verificationRequest.result == VerificationResult.AUTHENTIC,
            message = verificationRequest.verificationDetails ?: "",
            timestamp = Date(verificationRequest.requestDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
        )
    }
}