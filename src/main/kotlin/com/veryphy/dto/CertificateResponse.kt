package com.veryphy.dto

/**
 * Certificate response
 */
data class CertificateResponse(
    val degreeHash: String,
    val certificateImage: ByteArray,
    val studentName: String,
    val degreeName: String,
    val universityName: String,
    val issueDate: String
) {
    // Override equals and hashCode because of ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CertificateResponse

        if (degreeHash != other.degreeHash) return false
        if (!certificateImage.contentEquals(other.certificateImage)) return false
        if (studentName != other.studentName) return false
        if (degreeName != other.degreeName) return false
        if (universityName != other.universityName) return false
        if (issueDate != other.issueDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = degreeHash.hashCode()
        result = 31 * result + certificateImage.contentHashCode()
        result = 31 * result + studentName.hashCode()
        result = 31 * result + degreeName.hashCode()
        result = 31 * result + universityName.hashCode()
        result = 31 * result + issueDate.hashCode()
        return result
    }
}