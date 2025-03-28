package com.veryphy.certificate

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.VerticalAlignment
import com.veryphy.ai.AIService
import com.veryphy.model.Degree
import com.veryphy.model.University
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

@Service
class CertificateGenerator(
    private val aiService: AIService,
    @Value("\${certificate.template.path}") private val templatePath: String,
    @Value("\${certificate.output.path}") private val outputPath: String
) {
    init {
        // Create output directory if it doesn't exist
        File(outputPath).mkdirs()
    }

    /**
     * Generate a certificate for a degree
     */
    fun generateCertificate(degree: Degree, university: University): CertificateResult {
        logger.info { "Generating certificate for degree: ${degree.degreeId}" }

        try {
            // Generate unique pattern for the degree
            val pattern = aiService.generatePattern(degree)

            // Load certificate template
            val templateResource = ClassPathResource(templatePath)
            val template = templateResource.inputStream.readBytes()

            // Create output PDF
            val baos = ByteArrayOutputStream()
            val pdfReader = PdfReader(ByteArrayInputStream(template))
            val pdfWriter = PdfWriter(baos)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)
            val document = Document(pdfDoc)

            // Add watermark
            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, WatermarkEventHandler(university.name))

            // Add certificate content
            addCertificateContent(document, degree, university)

            // Add QR code with degree hash
            addQRCode(document, degree.degreeHash)

            // Close document
            document.close()

            // Get certificate bytes
            val certificateBytes = baos.toByteArray()

            // Embed pattern in certificate
            val finalCertificate = aiService.embedPatternInCertificate(certificateBytes, pattern)

            // Save certificate to file
            val outputFile = File(outputPath, "${degree.degreeId}.pdf")
            FileOutputStream(outputFile).use { it.write(finalCertificate) }

            return CertificateResult(
                certificateBytes = finalCertificate,
                filePath = outputFile.absolutePath,
                patternData = pattern,
                success = true,
                message = "Certificate generated successfully"
            )
        } catch (e: Exception) {
            logger.error(e) { "Error generating certificate" }
            return CertificateResult(
                certificateBytes = null,
                filePath = null,
                patternData = null,
                success = false,
                message = "Failed to generate certificate: ${e.message}"
            )
        }
    }

    /**
     * Add certificate content to the document
     */
    private fun addCertificateContent(document: Document, degree: Degree, university: University) {
        // Add university name
        val universityFont = PdfFontFactory.createFont("fonts/times-bold.ttf")
        val universityPara = Paragraph()
            .setFont(universityFont)
            .setFontSize(24f)
            .setTextAlignment(TextAlignment.CENTER)
            .add(university.name)
        document.add(universityPara)

        // Add certificate title
        val titleFont = PdfFontFactory.createFont("fonts/times-bold.ttf")
        val titlePara = Paragraph()
            .setFont(titleFont)
            .setFontSize(30f)
            .setTextAlignment(TextAlignment.CENTER)
            .add("DEGREE CERTIFICATE")
        document.add(titlePara)

        // Add spacing
        document.add(Paragraph("\n\n"))

        // Add certificate text
        val textFont = PdfFontFactory.createFont("fonts/times.ttf")
        val textPara = Paragraph()
            .setFont(textFont)
            .setFontSize(16f)
            .setTextAlignment(TextAlignment.CENTER)
            .add("This is to certify that\n\n")
            .add(Text(degree.studentName).setFont(titleFont).setFontSize(22f))
            .add("\n\nhas successfully completed the requirements for the degree of\n\n")
            .add(Text(degree.degreeName).setFont(titleFont).setFontSize(22f))
            .add("\n\nwith all the rights, privileges, and responsibilities thereto appertaining.\n\n")
            .add("Awarded on: ")
            .add(Text(degree.issueDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .setFont(textFont).setFontSize(16f))
        document.add(textPara)

        // Add signature section
        document.add(Paragraph("\n\n\n"))
        val signaturePara = Paragraph()
            .setFont(textFont)
            .setFontSize(16f)
            .setTextAlignment(TextAlignment.CENTER)
            .add("_________________________\nUniversity Registrar")
        document.add(signaturePara)

        // Add degree ID and verification information
        document.add(Paragraph("\n\n"))
        val verificationPara = Paragraph()
            .setFont(textFont)
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
            .add("Degree ID: ${degree.degreeId}\nVerify this degree at: https://veryphy.com/verify")
        document.add(verificationPara)
    }

    /**
     * Add QR code with degree hash to the document
     */
    private fun addQRCode(document: Document, degreeHash: String) {
        // In a real implementation, you would generate a QR code
        // For simplicity, we'll add a placeholder
        val qrText = Paragraph()
            .setFont(PdfFontFactory.createFont("fonts/courier.ttf"))
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.RIGHT)
            .add("Verification Hash:\n$degreeHash")
        document.add(qrText)
    }

    /**
     * Watermark event handler for adding a diagonal watermark to each page
     */
    inner class WatermarkEventHandler(private val watermarkText: String) : IEventHandler {
        override fun handleEvent(event: Event) {
            val docEvent = event as PdfDocumentEvent
            val pdf = docEvent.document
            val page = docEvent.page
            val pageSize = page.pageSize

            // Create PdfCanvas for watermark
            val canvas = PdfCanvas(page)
            canvas.saveState()

            // Set transparency
            val gs1 = PdfExtGState()
            gs1.fillOpacity = 0.1f
            canvas.setExtGState(gs1)

            // Add watermark text diagonally
            val font = PdfFontFactory.createFont("fonts/times-bold.ttf")
            canvas.beginText()
                .setFontAndSize(font, 60f)
                .setColor(ColorConstants.LIGHT_GRAY, 0.1f)
                .setTextMatrix(1f, 0f, 0f, 1f, 0f, 0f)
                .showTextAligned(
                    watermarkText,
                    pageSize.width / 2,
                    pageSize.height / 2,
                    45f,
                    TextAlignment.CENTER,
                    VerticalAlignment.MIDDLE,
                    0f
                )
                .endText()

            canvas.restoreState()
        }
    }

    /**
     * Result class for certificate generation
     */
    data class CertificateResult(
        val certificateBytes: ByteArray?,
        val filePath: String?,
        val patternData: ByteArray?,
        val success: Boolean,
        val message: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CertificateResult

            if (certificateBytes != null) {
                if (other.certificateBytes == null) return false
                if (!certificateBytes.contentEquals(other.certificateBytes)) return false
            } else if (other.certificateBytes != null) return false
            if (filePath != other.filePath) return false
            if (patternData != null) {
                if (other.patternData == null) return false
                if (!patternData.contentEquals(other.patternData)) return false
            } else if (other.patternData != null) return false
            if (success != other.success) return false
            if (message != other.message) return false

            return true
        }

        override fun hashCode(): Int {
            var result = certificateBytes?.contentHashCode() ?: 0
            result = 31 * result + (filePath?.hashCode() ?: 0)
            result = 31 * result + (patternData?.contentHashCode() ?: 0)
            result = 31 * result + success.hashCode()
            result = 31 * result + message.hashCode()
            return result
        }
    }
}