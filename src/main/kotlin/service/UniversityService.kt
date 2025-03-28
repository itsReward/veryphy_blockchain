package com.veryphy.service

import com.veryphy.blockchain.BlockchainService
import com.veryphy.blockchain.RealBlockchainService
import com.veryphy.model.University
import com.veryphy.model.UniversityStatus
import com.veryphy.repository.UniversityRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class UniversityService(
    private val universityRepository: UniversityRepository,
    private val blockchainService: BlockchainService
) {
    /**
     * Register a new university
     */
    @Transactional
    fun registerUniversity(university: University): University {
        logger.info { "Registering new university: ${university.name}" }

        // Validate university data
        if (universityRepository.findByEmail(university.email).isPresent) {
            throw ServiceException("University with email ${university.email} already exists")
        }

        // Generate unique registration ID if not provided
        val registrationId = university.registrationId.ifEmpty {
            "UNI-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        }

        // Create university entity
        val newUniversity = university.copy(
            registrationId = registrationId,
            status = UniversityStatus.PENDING,
            joinDate = LocalDateTime.now()
        )

        // Save to database
        val savedUniversity = universityRepository.save(newUniversity)

        try {
            // Register on blockchain
            val blockchainId = blockchainService.registerUniversity(savedUniversity)

            // Update blockchain ID
            savedUniversity.blockchainId = blockchainId
            universityRepository.save(savedUniversity)

            logger.info { "University registered successfully: ${savedUniversity.registrationId}" }
            return savedUniversity
        } catch (e: Exception) {
            logger.error(e) { "Failed to register university on blockchain: ${savedUniversity.registrationId}" }
            throw ServiceException("Failed to register university on blockchain", e)
        }
    }

    /**
     * Activate a pending university
     */
    @Transactional
    fun activateUniversity(universityId: Long): University {
        logger.info { "Activating university with ID: $universityId" }

        val university = universityRepository.findById(universityId)
            .orElseThrow { ServiceException("University not found with ID: $universityId") }

        if (university.status != UniversityStatus.PENDING) {
            throw ServiceException("Cannot activate university with status: ${university.status}")
        }

        university.status = UniversityStatus.ACTIVE
        return universityRepository.save(university)
    }

    /**
     * Blacklist a university
     */
    @Transactional
    fun blacklistUniversity(universityId: Long, reason: String): University {
        logger.info { "Blacklisting university with ID: $universityId" }

        val university = universityRepository.findById(universityId)
            .orElseThrow { ServiceException("University not found with ID: $universityId") }

        university.status = UniversityStatus.BLACKLISTED
        val updatedUniversity = universityRepository.save(university)

        try {
            // Update blockchain
            blockchainService.blacklistUniversity(university.registrationId, reason)

            logger.info { "University blacklisted successfully: ${university.registrationId}" }
            return updatedUniversity
        } catch (e: Exception) {
            logger.error(e) { "Failed to blacklist university on blockchain: ${university.registrationId}" }
            throw ServiceException("Failed to blacklist university on blockchain", e)
        }
    }

    /**
     * Get a university by ID
     */
    fun getUniversityById(universityId: Long): University {
        return universityRepository.findById(universityId)
            .orElseThrow { ServiceException("University not found with ID: $universityId") }
    }

    /**
     * Get a university by registration ID
     */
    fun getUniversityByRegistrationId(registrationId: String): University {
        return universityRepository.findByRegistrationId(registrationId)
            .orElseThrow { ServiceException("University not found with registration ID: $registrationId") }
    }

    /**
     * Get all universities with pagination
     */
    fun getAllUniversities(pageable: Pageable): Page<University> {
        return universityRepository.findAll(pageable)
    }

    /**
     * Get universities by status
     */
    fun getUniversitiesByStatus(status: UniversityStatus): List<University> {
        return universityRepository.findByStatus(status)
    }

    /**
     * Search universities by name or email
     */
    fun searchUniversities(keyword: String): List<University> {
        return universityRepository.searchByNameOrEmail(keyword)
    }

    /**
     * Get statistics about universities
     */
    fun getStatistics(): UniversityStatistics {
        val totalUniversities = universityRepository.count()
        val activeUniversities = universityRepository.countByStatus(UniversityStatus.ACTIVE)
        val pendingUniversities = universityRepository.countByStatus(UniversityStatus.PENDING)
        val blacklistedUniversities = universityRepository.countByStatus(UniversityStatus.BLACKLISTED)

        return UniversityStatistics(
            totalUniversities = totalUniversities,
            activeUniversities = activeUniversities,
            pendingUniversities = pendingUniversities,
            blacklistedUniversities = blacklistedUniversities
        )
    }

    /**
     * Get top universities by degree count
     */
    fun getTopUniversitiesByDegreeCount(startDate: LocalDateTime): List<University> {
        return universityRepository.findTopUniversitiesByDegreeCount(startDate)
    }

    /**
     * Data class for university statistics
     */
    data class UniversityStatistics(
        val totalUniversities: Long,
        val activeUniversities: Long,
        val pendingUniversities: Long,
        val blacklistedUniversities: Long
    )

    /**
     * Service exception class
     */
    class ServiceException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }
}