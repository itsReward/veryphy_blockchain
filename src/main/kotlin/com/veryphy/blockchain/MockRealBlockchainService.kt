package com.veryphy.blockchain

import com.fasterxml.jackson.databind.ObjectMapper
import com.veryphy.model.Degree
import com.veryphy.model.University
import com.veryphy.model.VerificationRequest
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
@Profile("dev") // Use this service only in dev profile
class MockRealBlockchainService(private val objectMapper: ObjectMapper) : BlockchainService {

    private val universities = mutableMapOf<String, BlockchainService.UniversityDto>()
    private val degrees = mutableMapOf<String, BlockchainService.DegreeDto>()
    private val verifications = mutableMapOf<String, BlockchainService.VerificationDto>()
    private val degreeHashes = mutableMapOf<String, String>() // Maps hash to degreeId

    override fun registerUniversity(university: University): String {
        logger.info { "Mock: Registering university on blockchain: ${university.registrationId}" }

        val universityDto = BlockchainService.UniversityDto(
            id = university.registrationId,
            name = university.name,
            email = university.email,
            stakeAmount = university.stakeAmount,
            status = university.status.name
        )

        universities[university.registrationId] = universityDto
        return university.registrationId
    }

    override fun registerDegree(degree: Degree): String {
        logger.info { "Mock: Registering degree on blockchain: ${degree.degreeId}" }

        val degreeDto = BlockchainService.DegreeDto(
            id = degree.degreeId,
            studentId = degree.studentId,
            studentName = degree.studentName,
            degreeName = degree.degreeName,
            universityId = degree.university.registrationId,
            issueDate = degree.issueDate.toString(),
            degreeHash = degree.degreeHash,
            status = degree.status.name
        )

        degrees[degree.degreeId] = degreeDto
        degreeHashes[degree.degreeHash] = degree.degreeId
        return degree.degreeId
    }

    override fun verifyDegree(degreeHash: String): BlockchainService.VerificationResultDto {
        logger.info { "Mock: Verifying degree on blockchain: $degreeHash" }

        val degreeId = degreeHashes[degreeHash]

        return if (degreeId != null) {
            val degree = degrees[degreeId]
            if (degree != null) {
                BlockchainService.VerificationResultDto(
                    isValid = true,
                    degreeId = degree.id,
                    universityId = degree.universityId,
                    issueDate = degree.issueDate,
                    status = degree.status,
                    message = "Degree successfully verified"
                )
            } else {
                BlockchainService.VerificationResultDto(
                    isValid = false,
                    degreeId = degreeId,
                    universityId = null,
                    issueDate = null,
                    status = null,
                    message = "Degree not found in ledger"
                )
            }
        } else {
            BlockchainService.VerificationResultDto(
                isValid = false,
                degreeId = null,
                universityId = null,
                issueDate = null,
                status = null,
                message = "Degree hash not found"
            )
        }
    }

    override fun recordVerification(verificationRequest: VerificationRequest): String {
        logger.info { "Mock: Recording verification on blockchain: ${verificationRequest.requestId}" }

        val verificationDto = BlockchainService.VerificationDto(
            id = verificationRequest.requestId,
            degreeId = verificationRequest.degree!!.degreeId,
            employerId = verificationRequest.employerId,
            requestDate = verificationRequest.requestDate.toString(),
            result = verificationRequest.result.name,
            paymentAmount = verificationRequest.paymentAmount,
            paymentStatus = verificationRequest.paymentStatus
        )

        verifications[verificationRequest.requestId] = verificationDto
        return verificationRequest.requestId
    }

    override fun queryDegreeHistory(degreeId: String): List<BlockchainService.DegreeHistoryDto> {
        logger.info { "Mock: Querying degree history from blockchain: $degreeId" }

        return listOf(
            BlockchainService.DegreeHistoryDto(
                txId = UUID.randomUUID().toString(),
                timestamp = Date().toString(),
                degreeId = degreeId,
                status = "REGISTERED",
                action = "CREATE"
            ),
            BlockchainService.DegreeHistoryDto(
                txId = UUID.randomUUID().toString(),
                timestamp = Date().toString(),
                degreeId = degreeId,
                status = "VERIFIED",
                action = "UPDATE"
            )
        )
    }

    override fun revokeDegree(degreeId: String, reason: String): String {
        logger.info { "Mock: Revoking degree on blockchain: $degreeId" }

        val degree = degrees[degreeId]
        if (degree != null) {
            degrees[degreeId] = degree.copy(status = "REVOKED")
        }

        return degreeId
    }

    override fun blacklistUniversity(universityId: String, reason: String): String {
        logger.info { "Mock: Blacklisting university on blockchain: $universityId" }

        val university = universities[universityId]
        if (university != null) {
            universities[universityId] = university.copy(status = "BLACKLISTED")
        }

        return universityId
    }
}