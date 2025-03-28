package com.veryphy.blockchain.chaincode

import com.fasterxml.jackson.databind.ObjectMapper
import org.hyperledger.fabric.contract.Context
import org.hyperledger.fabric.contract.ContractInterface
import org.hyperledger.fabric.contract.annotation.Contract
import org.hyperledger.fabric.contract.annotation.Default
import org.hyperledger.fabric.contract.annotation.Info
import org.hyperledger.fabric.contract.annotation.Transaction
import org.hyperledger.fabric.shim.ChaincodeException
import java.util.*

@Contract(
    name = "DegreeAttestationChaincode",
    info = Info(
        title = "Degree Attestation Chaincode",
        description = "Smart contract for attestation of academic degrees",
        version = "1.0.0"
    )
)
@Default
class DegreeAttestationChaincode : ContractInterface {
    private val objectMapper = ObjectMapper()

    @Transaction
    fun initLedger(ctx: Context): String {
        val stub = ctx.stub
        val systemInfo = SystemInfo(
            id = "system-info",
            registeredUniversities = 0,
            totalDegrees = 0,
            verificationCount = 0,
            successRate = 100.0,
            lastUpdated = Date().toString()
        )
        stub.putStringState(systemInfo.id, objectMapper.writeValueAsString(systemInfo))
        return "Blockchain initialized successfully"
    }

    @Transaction
    fun registerUniversity(ctx: Context, universityJson: String): String {
        val stub = ctx.stub
        val university = objectMapper.readValue(universityJson, University::class.java)

        // Check if university already exists
        val universityCompositeKey = stub.createCompositeKey("University", university.id)
        val existingUniversity = stub.getStringState(universityCompositeKey.toString())
        if (existingUniversity.isNotEmpty()) {
            throw ChaincodeException("University with ID ${university.id} already exists")
        }

        // Save university to ledger
        stub.putStringState(universityCompositeKey.toString(), universityJson)

        // Update system stats
        val systemInfoStr = stub.getStringState("system-info")
        if (systemInfoStr.isNotEmpty()) {
            val systemInfo = objectMapper.readValue(systemInfoStr, SystemInfo::class.java)
            systemInfo.registeredUniversities++
            systemInfo.lastUpdated = Date().toString()
            stub.putStringState(systemInfo.id, objectMapper.writeValueAsString(systemInfo))
        }

        return university.id
    }

    @Transaction
    fun registerDegree(ctx: Context, degreeJson: String): String {
        val stub = ctx.stub
        val degree = objectMapper.readValue(degreeJson, Degree::class.java)

        // Check if degree already exists
        val degreeCompositeKey = stub.createCompositeKey("Degree", degree.id)
        val existingDegree = stub.getStringState(degreeCompositeKey.toString())
        if (existingDegree.isNotEmpty()) {
            throw ChaincodeException("Degree with ID ${degree.id} already exists")
        }

        // Check if university exists
        val universityCompositeKey = stub.createCompositeKey("University", degree.universityId)
        val existingUniversity = stub.getStringState(universityCompositeKey.toString())
        if (existingUniversity.isEmpty()) {
            throw ChaincodeException("University with ID ${degree.universityId} does not exist")
        }

        // Save degree hash mapping for verification
        val hashKey = stub.createCompositeKey("Hash", degree.degreeHash)
        stub.putStringState(hashKey.toString(), degree.id)

        // Save degree to ledger
        stub.putStringState(degreeCompositeKey.toString(), degreeJson)

        // Update system stats
        val systemInfoStr = stub.getStringState("system-info")
        if (systemInfoStr.isNotEmpty()) {
            val systemInfo = objectMapper.readValue(systemInfoStr, SystemInfo::class.java)
            systemInfo.totalDegrees++
            systemInfo.lastUpdated = Date().toString()
            stub.putStringState(systemInfo.id, objectMapper.writeValueAsString(systemInfo))
        }

        return degree.id
    }

    @Transaction
    fun verifyDegree(ctx: Context, degreeHash: String): String {
        val stub = ctx.stub

        // Check if degree hash exists
        val hashKey = stub.createCompositeKey("Hash", degreeHash)
        val degreeId = stub.getStringState(hashKey.toString())

        val result = if (degreeId.isNotEmpty()) {
            // Get degree details
            val degreeCompositeKey = stub.createCompositeKey("Degree", degreeId)
            val degreeStr = stub.getStringState(degreeCompositeKey.toString())

            if (degreeStr.isNotEmpty()) {
                val degree = objectMapper.readValue(degreeStr, Degree::class.java)
                VerificationResult(
                    isValid = true,
                    degreeId = degree.id,
                    universityId = degree.universityId,
                    issueDate = degree.issueDate,
                    status = degree.status,
                    message = "Degree successfully verified"
                )
            } else {
                VerificationResult(
                    isValid = false,
                    degreeId = degreeId,
                    universityId = null,
                    issueDate = null,
                    status = null,
                    message = "Degree not found in ledger"
                )
            }
        } else {
            VerificationResult(
                isValid = false,
                degreeId = null,
                universityId = null,
                issueDate = null,
                status = null,
                message = "Degree hash not found"
            )
        }

        return objectMapper.writeValueAsString(result)
    }

    @Transaction
    fun recordVerification(ctx: Context, verificationJson: String): String {
        val stub = ctx.stub
        val verification = objectMapper.readValue(verificationJson, Verification::class.java)

        // Check if verification already exists
        val verificationCompositeKey = stub.createCompositeKey("Verification", verification.id)
        val existingVerification = stub.getStringState(verificationCompositeKey.toString())
        if (existingVerification.isNotEmpty()) {
            throw ChaincodeException("Verification with ID ${verification.id} already exists")
        }

        // Check if degree exists
        val degreeCompositeKey = stub.createCompositeKey("Degree", verification.degreeId)
        val degreeStr = stub.getStringState(degreeCompositeKey.toString())
        if (degreeStr.isEmpty()) {
            throw ChaincodeException("Degree with ID ${verification.degreeId} does not exist")
        }

        // Save verification to ledger
        stub.putStringState(verificationCompositeKey.toString(), verificationJson)

        // Update system stats
        val systemInfoStr = stub.getStringState("system-info")
        if (systemInfoStr.isNotEmpty()) {
            val systemInfo = objectMapper.readValue(systemInfoStr, SystemInfo::class.java)
            systemInfo.verificationCount++

            // Update success rate
            if (verification.result == "AUTHENTIC") {
                val successfulVerifications = systemInfo.verificationCount * systemInfo.successRate / 100.0
                val newSuccessfulVerifications = successfulVerifications + 1
                systemInfo.successRate = (newSuccessfulVerifications / systemInfo.verificationCount) * 100.0
            } else if (verification.result == "FAILED") {
                val successfulVerifications = systemInfo.verificationCount * systemInfo.successRate / 100.0
                systemInfo.successRate = (successfulVerifications / systemInfo.verificationCount) * 100.0
            }

            systemInfo.lastUpdated = Date().toString()
            stub.putStringState(systemInfo.id, objectMapper.writeValueAsString(systemInfo))
        }

        return verification.id
    }

    @Transaction
    fun revokeDegree(ctx: Context, degreeId: String, reason: String): String {
        val stub = ctx.stub

        // Check if degree exists
        val degreeCompositeKey = stub.createCompositeKey("Degree", degreeId)
        val degreeStr = stub.getStringState(degreeCompositeKey.toString())
        if (degreeStr.isEmpty()) {
            throw ChaincodeException("Degree with ID $degreeId does not exist")
        }

        // Update degree status
        val degree = objectMapper.readValue(degreeStr, Degree::class.java)
        degree.status = "REVOKED"
        stub.putStringState(degreeCompositeKey.toString(), objectMapper.writeValueAsString(degree))

        // Record revocation event
        val revocation = Revocation(
            id = UUID.randomUUID().toString(),
            degreeId = degreeId,
            reason = reason,
            timestamp = Date().toString()
        )
        val revocationCompositeKey = stub.createCompositeKey("Revocation", revocation.id)
        stub.putStringState(revocationCompositeKey.toString(), objectMapper.writeValueAsString(revocation))

        return degreeId
    }

    @Transaction
    fun blacklistUniversity(ctx: Context, universityId: String, reason: String): String {
        val stub = ctx.stub

        // Check if university exists
        val universityCompositeKey = stub.createCompositeKey("University", universityId)
        val universityStr = stub.getStringState(universityCompositeKey.toString())
        if (universityStr.isEmpty()) {
            throw ChaincodeException("University with ID $universityId does not exist")
        }

        // Update university status
        val university = objectMapper.readValue(universityStr, University::class.java)
        university.status = "BLACKLISTED"
        stub.putStringState(universityCompositeKey.toString(), objectMapper.writeValueAsString(university))

        // Record blacklisting event
        val blacklisting = Blacklisting(
            id = UUID.randomUUID().toString(),
            universityId = universityId,
            reason = reason,
            timestamp = Date().toString()
        )
        val blacklistingCompositeKey = stub.createCompositeKey("Blacklisting", blacklisting.id)
        stub.putStringState(blacklistingCompositeKey.toString(), objectMapper.writeValueAsString(blacklisting))

        return universityId
    }

    @Transaction
    fun getDegreeHistory(ctx: Context, degreeId: String): String {
        val stub = ctx.stub

        // Check if degree exists
        val degreeCompositeKey = stub.createCompositeKey("Degree", degreeId)
        val degreeStr = stub.getStringState(degreeCompositeKey.toString())
        if (degreeStr.isEmpty()) {
            throw ChaincodeException("Degree with ID $degreeId does not exist")
        }

        // Get degree history
        val historyIterator = stub.getHistoryForKey(degreeCompositeKey.toString())
        val history = mutableListOf<DegreeHistory>()

        while (historyIterator.hasNext()) {
            val modification = historyIterator.next()
            val timestamp = if (modification.timestamp != null) Date(modification.timestamp.seconds * 1000).toString() else "N/A"

            if (modification.isDeleted) {
                history.add(DegreeHistory(
                    txId = modification.txId,
                    timestamp = timestamp,
                    degreeId = degreeId,
                    status = "DELETED",
                    action = "DELETE"
                ))
            } else {
                val historyDegree = objectMapper.readValue(modification.value, Degree::class.java)
                history.add(DegreeHistory(
                    txId = modification.txId,
                    timestamp = timestamp,
                    degreeId = degreeId,
                    status = historyDegree.status,
                    action = "UPDATE"
                ))
            }
        }

        return objectMapper.writeValueAsString(history)
    }

    @Transaction
    fun getSystemStats(ctx: Context): String {
        val stub = ctx.stub
        val systemInfoStr = stub.getStringState("system-info")

        if (systemInfoStr.isEmpty()) {
            throw ChaincodeException("System info not found")
        }

        return systemInfoStr
    }

    // Data classes for chaincode
    data class University(
        val id: String,
        val name: String,
        val email: String,
        val stakeAmount: Double,
        var status: String
    )

    data class Degree(
        val id: String,
        val studentId: String,
        val studentName: String,
        val degreeName: String,
        val universityId: String,
        val issueDate: String,
        val degreeHash: String,
        var status: String
    )

    data class Verification(
        val id: String,
        val degreeId: String,
        val employerId: String,
        val requestDate: String,
        val result: String,
        val paymentAmount: Double,
        val paymentStatus: String
    )

    data class Revocation(
        val id: String,
        val degreeId: String,
        val reason: String,
        val timestamp: String
    )

    data class Blacklisting(
        val id: String,
        val universityId: String,
        val reason: String,
        val timestamp: String
    )

    data class SystemInfo(
        val id: String,
        var registeredUniversities: Int,
        var totalDegrees: Int,
        var verificationCount: Int,
        var successRate: Double,
        var lastUpdated: String
    )

    data class VerificationResult(
        val isValid: Boolean,
        val degreeId: String?,
        val universityId: String?,
        val issueDate: String?,
        val status: String?,
        val message: String
    )

    data class DegreeHistory(
        val txId: String,
        val timestamp: String,
        val degreeId: String,
        val status: String,
        val action: String
    )
}