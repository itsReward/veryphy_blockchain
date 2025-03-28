package com.veryphy.blockchain

import com.fasterxml.jackson.databind.ObjectMapper
import com.veryphy.model.Degree
import com.veryphy.model.University
import com.veryphy.model.VerificationRequest
import mu.KotlinLogging
import org.hyperledger.fabric.gateway.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger {}

@Service
class BlockchainService(
    private val objectMapper: ObjectMapper,
    @Value("\${blockchain.network.config.path}") private val networkConfigPath: Resource,
    @Value("\${blockchain.channel.name}") private val channelName: String,
    @Value("\${blockchain.chaincode.name}") private val chaincodeName: String,
    @Value("\${blockchain.admin.username}") private val adminUsername: String,
    @Value("\${blockchain.admin.password}") private val adminPassword: String
) {
    private lateinit var gateway: Gateway
    private lateinit var network: Network
    private lateinit var contract: Contract

    @PostConstruct
    fun initialize() {
        try {
            // Load connection profile
            val connectionProfilePath = Files.createTempFile("connection-profile", ".yaml")
            networkConfigPath.inputStream.use { input ->
                Files.copy(input, connectionProfilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }

            // Configure the gateway connection
            val wallet = Wallets.newInMemoryWallet()
            val adminIdentity = Identities.newX509Identity("Org1MSP", Files.newInputStream(Paths.get("admin.cert")), Files.newInputStream(Paths.get("admin.key")))
            wallet.put(adminUsername, adminIdentity)

            // Connect to the gateway
            val builder = Gateway.createBuilder()
                .identity(wallet, adminUsername)
                .networkConfig(connectionProfilePath)
                .discovery(true)

            gateway = builder.connect()
            network = gateway.getNetwork(channelName)
            contract = network.getContract(chaincodeName)

            logger.info { "Successfully connected to blockchain network" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize blockchain connection" }
            throw RuntimeException("Failed to initialize blockchain connection", e)
        }
    }

    @PreDestroy
    fun close() {
        gateway.close()
        logger.info { "Blockchain connection closed" }
    }

    /**
     * Register a university on the blockchain
     */
    fun registerUniversity(university: University): String {
        try {
            val universityJson = objectMapper.writeValueAsString(UniversityDto(
                id = university.registrationId,
                name = university.name,
                email = university.email,
                stakeAmount = university.stakeAmount,
                status = university.status.name
            ))

            val result = contract.submitTransaction("registerUniversity", universityJson)
            return String(result)
        } catch (e: Exception) {
            logger.error(e) { "Failed to register university on blockchain: ${university.registrationId}" }
            throw BlockchainException("Failed to register university on blockchain", e)
        }
    }

    /**
     * Register a degree on the blockchain
     */
    fun registerDegree(degree: Degree): String {
        try {
            val degreeJson = objectMapper.writeValueAsString(DegreeDto(
                id = degree.degreeId,
                studentId = degree.studentId,
                studentName = degree.studentName,
                degreeName = degree.degreeName,
                universityId = degree.university.registrationId,
                issueDate = degree.issueDate.toString(),
                degreeHash = degree.degreeHash,
                status = degree.status.name
            ))

            val result = contract.submitTransaction("registerDegree", degreeJson)
            return String(result)
        } catch (e: Exception) {
            logger.error(e) { "Failed to register degree on blockchain: ${degree.degreeId}" }
            throw BlockchainException("Failed to register degree on blockchain", e)
        }
    }

    /**
     * Verify a degree on the blockchain
     */
    fun verifyDegree(verificationRequest: VerificationRequest): VerificationResultDto {
        try {
            val degreeHash = verificationRequest.degree.degreeHash
            val result = contract.evaluateTransaction("verifyDegree", degreeHash)
            return objectMapper.readValue(result, VerificationResultDto::class.java)
        } catch (e: Exception) {
            logger.error(e) { "Failed to verify degree on blockchain: ${verificationRequest.degree.degreeId}" }
            throw BlockchainException("Failed to verify degree on blockchain", e)
        }
    }

    /**
     * Record a verification transaction on the blockchain
     */
    fun recordVerification(verificationRequest: VerificationRequest): String {
        try {
            val verificationJson = objectMapper.writeValueAsString(VerificationDto(
                id = verificationRequest.requestId,
                degreeId = verificationRequest.degree.degreeId,
                employerId = verificationRequest.employerId,
                requestDate = verificationRequest.requestDate.toString(),
                result = verificationRequest.result.name,
                paymentAmount = verificationRequest.paymentAmount,
                paymentStatus = verificationRequest.paymentStatus
            ))

            val result = contract.submitTransaction("recordVerification", verificationJson)
            return String(result)
        } catch (e: Exception) {
            logger.error(e) { "Failed to record verification on blockchain: ${verificationRequest.requestId}" }
            throw BlockchainException("Failed to record verification on blockchain", e)
        }
    }

    /**
     * Query degree history from the blockchain
     */
    fun queryDegreeHistory(degreeId: String): List<DegreeHistoryDto> {
        try {
            val result = contract.evaluateTransaction("getDegreeHistory", degreeId)
            return objectMapper.readValue(result, objectMapper.typeFactory.constructCollectionType(List::class.java, DegreeHistoryDto::class.java))
        } catch (e: Exception) {
            logger.error(e) { "Failed to query degree history from blockchain: $degreeId" }
            throw BlockchainException("Failed to query degree history from blockchain", e)
        }
    }

    // DTO classes for blockchain interaction
    data class UniversityDto(
        val id: String,
        val name: String,
        val email: String,
        val stakeAmount: Double,
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
        val paymentAmount: Double,
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

    class BlockchainException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }
}