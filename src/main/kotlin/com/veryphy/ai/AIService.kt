package com.veryphy.ai

import com.veryphy.model.Degree
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.tensorflow.Graph
import org.tensorflow.Session
import org.tensorflow.Tensor
import org.tensorflow.op.Ops
import org.tensorflow.types.TFloat32
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlin.experimental.and

private val logger = KotlinLogging.logger {}

@Service
class AIService(
    @Value("\${ai.model.path}") private val modelPath: String,
    @Value("\${ai.pattern.size}") private val patternSize: Int
) {
    private lateinit var graph: Graph
    private lateinit var session: Session

    @PostConstruct
    fun initialize() {
        try {
            // Load TensorFlow model
            graph = Graph()
            val modelBytes = Files.readAllBytes(Paths.get(modelPath))
            graph.importGraphDef(modelBytes)
            session = Session(graph)

            logger.info { "AI model loaded successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load AI model" }
            // Create default graph if model loading fails
            graph = Graph()
            val tf = Ops.create(graph)
            val placeholder = tf.placeholder(TFloat32.class)
                    tf.identity(placeholder)
                    session = Session(graph)
        }
    }

    @PreDestroy
    fun close() {
        session.close()
        graph.close()
        logger.info { "AI resources released" }
    }

    /**
     * Generate a unique visual pattern based on degree hash
     * This pattern can be embedded into a degree certificate
     */
    fun generatePattern(degree: Degree): ByteArray {
        logger.info { "Generating pattern for degree: ${degree.degreeId}" }

        try {
            // Generate pattern based on degree hash
            val hashBytes = degree.degreeHash.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val hashDigest = md.digest(hashBytes)

            // Create deterministic pattern from hash
            val pattern = BufferedImage(patternSize, patternSize, BufferedImage.TYPE_INT_ARGB)
            val g = pattern.createGraphics()

            // Set background
            g.color = java.awt.Color(255, 255, 255, 0) // Transparent
            g.fillRect(0, 0, patternSize, patternSize)

            // Create unique pattern based on hash
            val random = Random(byteArrayToLong(hashDigest))

            // Generate a set of shapes based on the hash
            val numShapes = 10 + (hashDigest[0].toInt() and 0x0F)
            for (i in 0 until numShapes) {
                // Use different bits from hash to determine shape properties
                val shapeType = hashDigest[i % hashDigest.size].toInt() and 0x03
                val red = hashDigest[(i + 1) % hashDigest.size].toInt() and 0xFF
                val green = hashDigest[(i + 2) % hashDigest.size].toInt() and 0xFF
                val blue = hashDigest[(i + 3) % hashDigest.size].toInt() and 0xFF
                val alpha = 128 + (hashDigest[(i + 4) % hashDigest.size].toInt() and 0x7F)

                g.color = java.awt.Color(red, green, blue, alpha)

                val x = random.nextInt(patternSize)
                val y = random.nextInt(patternSize)
                val size = 5 + random.nextInt(15)

                when (shapeType) {
                    0 -> g.fillRect(x, y, size, size) // Square
                    1 -> g.fillOval(x, y, size, size) // Circle
                    2 -> { // Triangle
                        val xPoints = intArrayOf(x, x + size, x + size / 2)
                        val yPoints = intArrayOf(y + size, y + size, y)
                        g.fillPolygon(xPoints, yPoints, 3)
                    }
                    3 -> { // Line
                        g.drawLine(x, y, x + size, y + size)
                    }
                }
            }

            g.dispose()

            // Convert to byte array
            val baos = ByteArrayOutputStream()
            ImageIO.write(pattern, "PNG", baos)

            return baos.toByteArray()
        } catch (e: Exception) {
            logger.error(e) { "Error generating pattern" }
            throw AIException("Failed to generate pattern", e)
        }
    }

    /**
     * Extract and verify a pattern from a certificate image
     */
    fun extractAndVerifyPattern(certificateImage: ByteArray): String? {
        logger.info { "Extracting pattern from certificate" }

        try {
            // Read the certificate image
            val image = ImageIO.read(ByteArrayInputStream(certificateImage))

            // Extract the embedded pattern
            // This is a simplified implementation - in a real system,
            // you would use more sophisticated image processing techniques

            // For demonstration, let's assume the pattern is embedded in a specific region
            // In a real implementation, you would use computer vision to locate the pattern
            val patternRegion = image.getSubimage(
                image.width - patternSize - 10,
                image.height - patternSize - 10,
                patternSize,
                patternSize
            )

            // Convert the extracted pattern to byte array
            val baos = ByteArrayOutputStream()
            ImageIO.write(patternRegion, "PNG", baos)
            val extractedPattern = baos.toByteArray()

            // Use TensorFlow to decode the pattern
            val tf = Ops.create(graph)
            val inputTensor = Tensor.create(extractedPattern)

            // In a real implementation, you would run inference using the loaded model
            // For demonstration, we'll use a simpler approach to extract the hash

            // Calculate hash of the extracted pattern
            val md = MessageDigest.getInstance("SHA-256")
            val patternHash = md.digest(extractedPattern)

            // Convert hash to hexadecimal string
            val hexString = StringBuilder()
            for (b in patternHash) {
                hexString.append(String.format("%02x", b and 0xff.toByte()))
            }

            return hexString.toString()
        } catch (e: Exception) {
            logger.error(e) { "Error extracting pattern" }
            throw AIException("Failed to extract pattern", e)
        }
    }

    /**
     * Embed a pattern into a certificate image
     */
    fun embedPatternInCertificate(certificateBytes: ByteArray, patternBytes: ByteArray): ByteArray {
        logger.info { "Embedding pattern in certificate" }

        try {
            // Read the certificate image
            val certificateImage = ImageIO.read(ByteArrayInputStream(certificateBytes))

            // Read the pattern image
            val patternImage = ImageIO.read(ByteArrayInputStream(patternBytes))

            // Create a new image with the pattern embedded
            val resultImage = BufferedImage(
                certificateImage.width,
                certificateImage.height,
                BufferedImage.TYPE_INT_ARGB
            )

            // Draw the certificate
            val g = resultImage.createGraphics()
            g.drawImage(certificateImage, 0, 0, null)

            // Draw the pattern in the bottom-right corner
            g.drawImage(
                patternImage,
                certificateImage.width - patternImage.width - 10,
                certificateImage.height - patternImage.height - 10,
                null
            )

            g.dispose()

            // Convert to byte array
            val baos = ByteArrayOutputStream()
            ImageIO.write(resultImage, "PNG", baos)

            return baos.toByteArray()
        } catch (e: Exception) {
            logger.error(e) { "Error embedding pattern" }
            throw AIException("Failed to embed pattern", e)
        }
    }

    /**
     * Convert a byte array to a long for use as a seed
     */
    private fun byteArrayToLong(bytes: ByteArray): Long {
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(bytes, 0, minOf(8, bytes.size))
        buffer.flip()
        return buffer.long
    }

    class AIException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }
}