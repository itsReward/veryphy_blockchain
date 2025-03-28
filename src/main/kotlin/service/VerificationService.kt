package com.veryphy.service

import com.veryphy.blockchain.BlockchainService
import com.veryphy.model.Degree
import com.veryphy.model.DegreeStatus
import com.veryphy.model.VerificationRequest
import com.veryphy.model.VerificationResult
import com.veryphy.repository.DegreeRepository
import com.veryphy.repository.VerificationRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class VerificationService(
    private val verificationRepository: VerificationRepository,
    private val degreeRepository: DegreeRepository,
    private val degreeService: DegreeService,
    private val blockchainService: BlockchainService
) {
    /**
     * Create a verification request for a degree by hash
     */
    @Transactional
    fun createVerificationByHash(employerId: String, degreeHash: String, paymentAmount: Double): VerificationRequest {
        logger.info { "Creating verification request for degree hash: $degreeHash by employer: $employerId" }

        // Verify the degree
        val verificationResult = degreeService.verifyDegreeByHash(degreeHash)

        // Get or create degree reference
        val degree = verificationResult.degree ?: degreeRepository.findByDegreeHash(degreeHash)
            .orElseThrow { ServiceException("Degree not found with hash: $degreeHash") }

        // Create verification request
        val verificationRequest = VerificationRequest(
            requestId = "VER-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            employerId = employerId,
            degree = degree,
            requestDate = LocalDateTime.now(),
            result = if (verificationResult.isValid) VerificationResult.AUTHENTIC else VerificationResult.FAILED,
            verificationDetails = verificationResult.message,
            paymentAmount = paymentAmount,
            paymentStatus = "PENDING"
        )

        // Save to database
        val savedRequest = verificationRepository.save(verificationRequest)

        try {
            // Record on blockchain
            val blockchainTxId = blockchainService.recordVerification(savedRequest)

            // Update blockchain transaction ID
            savedRequest.blockchainTxId = blockchainTxId
            savedRequest.completedAt = LocalDateTime.now()
            verificationRepository.save(savedRequest)

            logger.info { "Verification request created successfully: ${savedRequest.requestId}" }
            return savedRequest
        } catch (e: Exception) {
            logger.error(e) { "Failed to record verification on blockchain: ${savedRequest.requestId}" }
            throw ServiceException("Failed to record verification on blockchain", e)
        }
    }

    /**
     * Create a verification request for a degree by certificate image
     */
    @Transactional
    fun createVerificationByCertificate(employerId: String, certificateImage: ByteArray, paymentAmount: Double): VerificationRequest {
        logger.info { "Creating verification request from certificate by employer: $employerId" }

        // Verify the certificate
        val verificationResult = degreeService.verifyDegreeByCertificate(certificateImage)

        if (!verificationResult.isValid || verificationResult.degree == null) {
            // Create a failed verification request without degree reference
            val verificationRequest = VerificationRequest(
                requestId = "VER-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
                employerId = employerId,
                degree = null,
                requestDate = LocalDateTime.now(),
                result = VerificationResult.FAILED,
                verificationDetails = verificationResult.message,
                paymentAmount = paymentAmount,
                paymentStatus = "PENDING"
            )

            return verificationRepository.save(verificationRequest)
        }

        // Create verification request with the degree reference
        val verificationRequest = VerificationRequest(
            requestId = "VER-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            employerId = employerId,
            degree = verificationResult.degree,
            requestDate = LocalDateTime.now(),
            result = VerificationResult.AUTHENTIC,
            verificationDetails = verificationResult.message,
            paymentAmount = paymentAmount,
            paymentStatus = "PENDING"
        )

        // Save to database
        val savedRequest = verificationRepository.save(verificationRequest)

        try {
            // Record on blockchain
            val blockchainTxId = blockchainService.recordVerification(savedRequest)

            // Update blockchain transaction ID
            savedRequest.blockchainTxId = blockchainTxId
            savedRequest.completedAt = LocalDateTime.now()
            verificationRepository.save(savedRequest)

            logger.info { "Verification request created successfully: ${savedRequest.requestId}" }
            return savedRequest
        } catch (e: Exception) {
            logger.error(e) { "Failed to record verification on blockchain: ${savedRequest.requestId}" }
            throw ServiceException("Failed to record verification on blockchain", e)
        }
    }

    /**
     * Update payment status for a verification request
     */
    @Transactional
    fun updatePaymentStatus(requestId: String, paymentStatus: String): VerificationRequest {
        logger.info { "Updating payment status for verification request: $requestId to $paymentStatus" }

        val verification = verificationRepository.findByRequestId(requestId)
            .orElseThrow { ServiceException("Verification request not found with ID: $requestId") }

        verification.paymentStatus = paymentStatus
        return verificationRepository.save(verification)
    }

    /**
     * Get a verification request by ID
     */
    fun getVerificationById(requestId: String): VerificationRequest {
        return verificationRepository.findByRequestId(requestId)
            .orElseThrow { ServiceException("Verification request not found with ID: $requestId") }
    }

    /**
     * Get verification requests by employer ID with pagination
     */
    fun getVerificationsByEmployer(employerId: String, pageable: Pageable): Page<VerificationRequest> {
        return verificationRepository.findByEmployerId(employerId, pageable)
    }

    /**
     * Get recent verification requests by employer ID
     */
    fun getRecentVerificationsByEmployer(employerId: String, count: Int): List<VerificationRequest> {
        return verificationRepository.findRecentByEmployerId(employerId, Pageable.ofSize(count))
    }

    /**
     * Get verification requests by degree ID with pagination
     */
    fun getVerificationsByDegree(degreeId: Long, pageable: Pageable): Page<VerificationRequest> {
        return verificationRepository.findByDegreeId(degreeId, pageable)
    }

    /**
     * Get verification requests by university ID with pagination
     */
    fun getVerificationsByUniversity(universityId: Long, pageable: Pageable): Page<VerificationRequest> {
        return verificationRepository.findByUniversityId(universityId, pageable)
    }

    /**
     * Get verification requests by date range with pagination
     */
    fun getVerificationsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime, pageable: Pageable): Page<VerificationRequest> {
        return verificationRepository.findByDateRange(startDate, endDate, pageable)
    }

    /**
     * Get statistics about verifications
     */
    fun getStatistics(): VerificationStatistics {
        val totalVerifications = verificationRepository.count()
        val authenticVerifications = verificationRepository.countByResult(VerificationResult.AUTHENTIC)
        val failedVerifications = verificationRepository.countByResult(VerificationResult.FAILED)
        val pendingVerifications = verificationRepository.countByResult(VerificationResult.PENDING)

        val totalPaymentAmount = verificationRepository.sumPaymentAmountByResult(VerificationResult.AUTHENTIC)

        val successRate = if (totalVerifications > 0) {
            (authenticVerifications.toDouble() / (authenticVerifications + failedVerifications).toDouble()) * 100.0
        } else {
            0.0
        }

        return VerificationStatistics(
            totalVerifications = totalVerifications,
            authenticVerifications = authenticVerifications,
            failedVerifications = failedVerifications,
            pendingVerifications = pendingVerifications,
            totalPaymentAmount = totalPaymentAmount,
            successRate = successRate
        )
    }

    /**
     * Get verification statistics for a university
     */
    fun getUniversityStatistics(universityId: Long): UniversityVerificationStatistics {
        val totalVerifications = verificationRepository.countByUniversityIdAndResult(universityId, VerificationResult.AUTHENTIC) +
                verificationRepository.countByUniversityIdAndResult(universityId, VerificationResult.FAILED)
        val authenticVerifications = verificationRepository.countByUniversityIdAndResult(universityId, VerificationResult.AUTHENTIC)

        val successRate = if (totalVerifications > 0) {
            (authenticVerifications.toDouble() / totalVerifications.toDouble()) * 100.0
        } else {
            0.0
        }

        return UniversityVerificationStatistics(
            universityId = universityId,
            totalVerifications = totalVerifications,
            authenticVerifications = authenticVerifications,
            failedVerifications = totalVerifications - authenticVerifications,
            successRate = successRate
        )
    }

    /**
     * Data class for verification statistics
     */
    data class VerificationStatistics(
        val totalVerifications: Long,
        val authenticVerifications: Long,
        val failedVerifications: Long,
        val pendingVerifications: Long,
        val totalPaymentAmount: Double,
        val successRate: Double
    )

    /**
     * Data class for university verification statistics
     */
    data class UniversityVerificationStatistics(
        val universityId: Long,
        val totalVerifications: Long,
        val authenticVerifications: Long,
        val failedVerifications: Long,
        val successRate: Double
    )

    /**
     * Service exception class
     */
    class ServiceException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }
}