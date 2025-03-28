package com.veryphy.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Degree entity - contains both on-chain and off-chain data
 */
@Entity
@Table(name = "degrees")
data class Degree(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val degreeId: String,

    @Column(nullable = false)
    val studentId: String,

    @Column(nullable = false)
    val studentName: String,

    @Column(nullable = false)
    val degreeName: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    val university: University,

    @Column(nullable = false)
    val issueDate: LocalDateTime,

    @Column(nullable = false, unique = true)
    val degreeHash: String,

    @Column
    var certificateUrl: String? = null,

    @Column
    var patternData: ByteArray? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DegreeStatus,

    @Column
    var blockchainTxId: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var updatedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "degree", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val verifications: MutableList<VerificationRequest> = mutableListOf()
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Degree

        // Compare all other fields first
        if (id != other.id) return false
        if (degreeId != other.degreeId) return false
        if (studentId != other.studentId) return false
        if (studentName != other.studentName) return false
        if (degreeName != other.degreeName) return false
        if (university != other.university) return false
        if (issueDate != other.issueDate) return false
        if (degreeHash != other.degreeHash) return false
        if (certificateUrl != other.certificateUrl) return false
        if (status != other.status) return false
        if (blockchainTxId != other.blockchainTxId) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        // Special handling for ByteArray comparison
        if (patternData != null) {
            if (other.patternData == null) return false
            if (!patternData.contentEquals(other.patternData)) return false
        } else if (other.patternData != null) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + degreeId.hashCode()
        result = 31 * result + studentId.hashCode()
        result = 31 * result + studentName.hashCode()
        result = 31 * result + degreeName.hashCode()
        result = 31 * result + university.hashCode()
        result = 31 * result + issueDate.hashCode()
        result = 31 * result + degreeHash.hashCode()
        result = 31 * result + (certificateUrl?.hashCode() ?: 0)

        // Special handling for ByteArray hashCode
        result = 31 * result + (patternData?.contentHashCode() ?: 0)

        result = 31 * result + status.hashCode()
        result = 31 * result + (blockchainTxId?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (updatedAt?.hashCode() ?: 0)
        return result
    }
}