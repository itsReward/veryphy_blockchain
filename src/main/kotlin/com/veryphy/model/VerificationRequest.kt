package com.veryphy.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Verification request entity
 */
@Entity
@Table(name = "verification_requests")
data class VerificationRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val requestId: String,

    @Column(nullable = false)
    val employerId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "degree_id", nullable = true)
    val degree: Degree? = null,

    @Column(nullable = false)
    val requestDate: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var result: VerificationResult = VerificationResult.PENDING,

    @Column
    var verificationDetails: String? = null,

    @Column
    var blockchainTxId: String? = null,

    @Column
    var completedAt: LocalDateTime? = null,

    @Column(nullable = false)
    val paymentAmount: BigDecimal,

    @Column
    var paymentStatus: String = "PENDING"
)