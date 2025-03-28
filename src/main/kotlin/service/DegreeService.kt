package com.veryphy.service

import com.veryphy.ai.AIService
import com.veryphy.blockchain.BlockchainService
import com.veryphy.blockchain.MockRealBlockchainService
import com.veryphy.blockchain.RealBlockchainService
import com.veryphy.certificate.CertificateGenerator
import com.veryphy.model.Degree
import com.veryphy.model.DegreeStatus
import com.veryphy.repository.DegreeRepository
import com.veryphy.repository.UniversityRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class DegreeService(
    private val degreeRepository: DegreeRepository,
    private val universityRepository: UniversityRepository,
    private val blockchainService: BlockchainService,
    private val certificateGenerator: CertificateGenerator,
    private val aiService: AIService
) {
    /**
     * Register a new degree
     */
    @Transactional
    fun registerDegree(degree: Degree): Degree {
        logger.info { "Registering new degree for student: ${degree.studentName}" }

        // Ensure university exists
        val university = universityRepository.findById(degree.university.id)
            .orElseThrow { ServiceException("University not found with ID: ${degree.university.id}") }

        // Generate unique degree ID if not provided
        val degreeId = degree.degreeId.ifEmpty {
            "DEG-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        }

        // Generate degree hash
        val degreeHash = generateDegreeHash(degree)

        // Create degree entity
        val newDegree = degree.copy(
            degreeId = degreeId,
            degreeHash = degreeHash,
            status = DegreeStatus.REGISTERED,
            createdAt = LocalDateTime.now()
        )

        // Save to database
        val savedDegree = degreeRepository.save(newDegree)

        try {
            // Register on blockchain
            val blockchainTxId = blockchainService.registerDegree(savedDegree)

            // Update blockchain transaction ID
            savedDegree.blockchainTxId = blockchainTxId

            // Generate certificate
            val certificateResult = certificateGenerator.generateCertificate(savedDegree, university)

            if (certificateResult.success) {
                // Update certificate URL and pattern data
                savedDegree.certificateUrl = certificateResult.filePath
                savedDegree.patternData = certificateResult.patternData

                // Update status to VERIFIED
                savedDegree.status = DegreeStatus.VERIFIED
                savedDegree.updatedAt = LocalDateTime.now()
            }

            degreeRepository.save(savedDegree)

            logger.info { "Degree registered successfully: ${savedDegree.degreeId}" }
            return savedDegree
        } catch (e: Exception) {
            logger.error(e) { "Failed to register degree: ${savedDegree.degreeId}" }
            throw ServiceException("Failed to register degree", e)
        }
    }

    /**
     * Revoke a degree
     */
    @Transactional
    fun revokeDegree(degreeId: String, reason: String): Degree {
        logger.info { "Revoking degree with ID: $degreeId" }

        val degree = degreeRepository.findByDegreeId(degreeId)
            .orElseThrow { ServiceException("Degree not found with ID: $degreeId") }

        degree.status = DegreeStatus.REVOKED
        degree.updatedAt = LocalDateTime.now()
        val updatedDegree = degreeRepository.save(degree)

        try {
            // Update blockchain
            blockchainService.revokeDegree(degree.degreeId, reason)

            logger.info { "Degree revoked successfully: ${degree.degreeId}" }
            return updatedDegree
        } catch (e: Exception) {
            logger.error(e) { "Failed to revoke degree on blockchain: ${degree.degreeId}" }
            throw ServiceException("Failed to revoke degree on blockchain", e)
        }
    }

    /**
     * Verify a degree by hash
     */
    fun verifyDegreeByHash(degreeHash: String): VerificationResult {
        logger.info { "Verifying degree with hash: $degreeHash" }

        try {
            // Check local database
            val degreeOptional = degreeRepository.findByDegreeHash(degreeHash)

            if (degreeOptional.isPresent) {
                val degree = degreeOptional.get()

                // Verify on blockchain
                val blockchainResultDto = blockchainService.verifyDegree(degreeHash)

                if (blockchainResultDto.isValid) {
                    return VerificationResult(
                        isValid = true,
                        degree = degree,
                        message = "Degree successfully verified"
                    )
                } else {
                    return VerificationResult(
                        isValid = false,
                        degree = degree,
                        message = "Degree verification failed on blockchain: ${blockchainResultDto.message}"
                    )
                }
            } else {
                // Check blockchain directly
                val blockchainResultDto = blockchainService.verifyDegree(degreeHash)

                return if (blockchainResultDto.isValid) {
                    VerificationResult(
                        isValid = true,
                        degree = null,
                        message = "Degree verified on blockchain but not found in local database"
                    )
                } else {
                    VerificationResult(
                        isValid = false,
                        degree = null,
                        message = "Degree not found in local database or blockchain"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error verifying degree: $degreeHash" }
            return VerificationResult(
                isValid = false,
                degree = null,
                message = "Error verifying degree: ${e.message}"
            )
        }
    }

    /**
     * Verify a degree by certificate image
     */
    fun verifyDegreeByCertificate(certificateImage: ByteArray): VerificationResult {
        logger.info { "Verifying degree from certificate image" }

        try {
            // Extract pattern from certificate using AI
            val extractedHash = aiService.extractAndVerifyPattern(certificateImage)

            return if (extractedHash != null) {
                // Verify the extracted hash
                verifyDegreeByHash(extractedHash)
            } else {
                VerificationResult(
                    isValid = false,
                    degree = null,
                    message = "Failed to extract verification pattern from certificate"
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error verifying certificate" }
            return VerificationResult(
                isValid = false,
                degree = null,
                message = "Error processing certificate: ${e.message}"
            )
        }
    }

    /**
     * Get a degree by ID
     */
    fun getDegreeById(degreeId: Long): Degree {
        return degreeRepository.findById(degreeId)
            .orElseThrow { ServiceException("Degree not found with ID: $degreeId") }
    }

    /**
     * Get a degree by degree ID
     */
    fun getDegreeByDegreeId(degreeId: String): Degree {
        return degreeRepository.findByDegreeId(degreeId)
            .orElseThrow { ServiceException("Degree not found with degree ID: $degreeId") }
    }

    /**
     * Get degrees by university ID with pagination
     */
    fun getDegreesByUniversity(universityId: Long, pageable: Pageable): Page<Degree> {
        return degreeRepository.findByUniversityId(universityId, pageable)
    }

    /**
     * Search degrees by keyword within a university
     */
    fun searchDegreesByUniversity(universityId: Long, keyword: String, pageable: Pageable): Page<Degree> {
        return degreeRepository.searchByUniversityAndKeyword(universityId, keyword, pageable)
    }

    /**
     * Get degrees by student ID
     */
    fun getDegreesByStudentId(studentId: String): List<Degree> {
        return degreeRepository.findByStudentId(studentId)
    }

    /**
     * Get degree history from blockchain
     */
    fun getDegreeHistory(degreeId: String): List<BlockchainService.DegreeHistoryDto> {
        logger.info { "Getting history for degree: $degreeId" }

        val degree = degreeRepository.findByDegreeId(degreeId)
            .orElseThrow { ServiceException("Degree not found with degree ID: $degreeId") }

        try {
            return blockchainService.queryDegreeHistory(degree.degreeId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get degree history from blockchain: ${degree.degreeId}" }
            throw ServiceException("Failed to get degree history from blockchain", e)
        }
    }

    /**
     * Generate a unique hash for a degree
     */
    private fun generateDegreeHash(degree: Degree): String {
        val data = "${degree.studentId}|${degree.studentName}|${degree.degreeName}|${degree.university.id}|${degree.issueDate}"
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(data.toByteArray())

        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get statistics about degrees
     */
    fun getStatistics(): DegreeStatistics {
        val totalDegrees = degreeRepository.count()
        val registeredDegrees = degreeRepository.countByStatus(DegreeStatus.REGISTERED)
        val verifiedDegrees = degreeRepository.countByStatus(DegreeStatus.VERIFIED)
        val revokedDegrees = degreeRepository.countByStatus(DegreeStatus.REVOKED)

        // Get degrees registered in the last 30 days
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        val recentDegrees = degreeRepository.countByDateRange(thirtyDaysAgo, LocalDateTime.now())

        return DegreeStatistics(
            totalDegrees = totalDegrees,
            registeredDegrees = registeredDegrees,
            verifiedDegrees = verifiedDegrees,
            revokedDegrees = revokedDegrees,
            recentDegrees = recentDegrees
        )
    }

    /**
     * Data class for degree statistics
     */
    data class DegreeStatistics(
        val totalDegrees: Long,
        val registeredDegrees: Long,
        val verifiedDegrees: Long,
        val revokedDegrees: Long,
        val recentDegrees: Long
    )

    /**
     * Data class for verification result
     */
    data class VerificationResult(
        val isValid: Boolean,
        val degree: Degree?,
        val message: String
    )

    /**
     * Service exception class
     */
    class ServiceException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }
}