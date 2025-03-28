package com.veryphy.client

import com.veryphy.exception.FabricConnectionException
import com.veryphy.exception.FabricTransactionException
import org.hyperledger.fabric.gateway.*
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.TimeoutException

/**
 * A wrapper class for interacting with the Hyperledger Fabric network
 */
class FabricClient(
    private val walletPath: Path,
    private val networkConfigPath: Path,
    private val channelName: String,
    private val contractName: String,
    private val userName: String
) {
    private val logger = LoggerFactory.getLogger(FabricClient::class.java)
    private var gateway: Gateway? = null
    private var network: Network? = null
    private var contract: Contract? = null

    /**
     * Connect to the Fabric network
     */
    fun connect() {
        try {
            // Load a wallet holding identities
            val wallet = Wallets.newFileSystemWallet(walletPath)

            // Configure the gateway connection
            val builder = Gateway.createBuilder()
                .identity(wallet, userName)
                .networkConfig(networkConfigPath)
                .discovery(true)

            // Connect to gateway
            gateway = builder.connect()

            // Get network and contract
            network = gateway?.getNetwork(channelName)
            contract = network?.getContract(contractName)

            logger.info("Connected to Hyperledger Fabric network")
        } catch (e: Exception) {
            logger.error("Failed to connect to Fabric network", e)
            throw FabricConnectionException("Failed to connect to Fabric network: ${e.message}")
        }
    }

    /**
     * Disconnect from the Fabric network
     */
    fun disconnect() {
        try {
            gateway?.close()
            logger.info("Disconnected from Hyperledger Fabric network")
        } catch (e: Exception) {
            logger.error("Error during disconnect", e)
        } finally {
            gateway = null
            network = null
            contract = null
        }
    }

    /**
     * Submit a transaction to the blockchain (write operation)
     */
    fun submitTransaction(functionName: String, vararg args: String): String {
        validateConnection()

        try {
            logger.info("Submitting transaction: $functionName with args: ${args.joinToString(", ")}")

            val result = contract?.submitTransaction(functionName, *args)
                ?: throw FabricTransactionException("Failed to submit transaction: null result")

            return String(result)
        } catch (e: ContractException) {
            logger.error("Contract exception during transaction submission", e)
            throw FabricTransactionException("Contract exception: ${e.message}")
        } catch (e: TimeoutException) {
            logger.error("Timeout during transaction submission", e)
            throw FabricTransactionException("Transaction timed out: ${e.message}")
        } catch (e: InterruptedException) {
            logger.error("Transaction interrupted", e)
            throw FabricTransactionException("Transaction interrupted: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error during transaction submission", e)
            throw FabricTransactionException("Transaction error: ${e.message}")
        }
    }

    /**
     * Evaluate a transaction on the blockchain (read operation)
     */
    fun evaluateTransaction(functionName: String, vararg args: String): String {
        validateConnection()

        try {
            logger.info("Evaluating transaction: $functionName with args: ${args.joinToString(", ")}")

            val result = contract?.evaluateTransaction(functionName, *args)
                ?: throw FabricTransactionException("Failed to evaluate transaction: null result")

            return String(result)
        } catch (e: ContractException) {
            logger.error("Contract exception during transaction evaluation", e)
            throw FabricTransactionException("Contract exception: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error during transaction evaluation", e)
            throw FabricTransactionException("Evaluation error: ${e.message}")
        }
    }

    /**
     * Register a new university on the blockchain
     */
    fun registerUniversity(
        universityId: String,
        name: String,
        email: String,
        stakeAmount: Double
    ): String {
        return submitTransaction(
            "registerUniversity",
            universityId,
            name,
            email,
            stakeAmount.toString()
        )
    }

    /**
     * Update university status
     */
    fun updateUniversityStatus(universityId: String, newStatus: String): String {
        return submitTransaction(
            "updateUniversityStatus",
            universityId,
            newStatus
        )
    }

    /**
     * Register a new degree on the blockchain
     */
    fun registerDegree(
        studentId: String,
        degreeName: String,
        universityId: String,
        issueDate: String
    ): String {
        return submitTransaction(
            "registerDegree",
            studentId,
            degreeName,
            universityId,
            issueDate
        )
    }

    /**
     * Verify a degree using its hash
     */
    fun verifyDegree(degreeHash: String): String {
        return evaluateTransaction("verifyDegree", degreeHash)
    }

    /**
     * Record a verification request
     */
    fun recordVerification(
        verificationId: String,
        employerId: String,
        degreeHash: String,
        paymentAmount: Double
    ): String {
        return submitTransaction(
            "recordVerification",
            verificationId,
            employerId,
            degreeHash,
            paymentAmount.toString()
        )
    }

    /**
     * Get all degrees registered by a university
     */
    fun getUniversityDegrees(universityId: String): String {
        return evaluateTransaction("getUniversityDegrees", universityId)
    }

    /**
     * Get all verification requests made by an employer
     */
    fun getEmployerVerifications(employerId: String): String {
        return evaluateTransaction("getEmployerVerifications", employerId)
    }

    /**
     * Get system statistics
     */
    fun getSystemStats(): String {
        return evaluateTransaction("getSystemStats")
    }

    private fun validateConnection() {
        if (contract == null) {
            connect()
            if (contract == null) {
                throw FabricConnectionException("Not connected to Fabric network")
            }
        }
    }
}
