package com.veryphy.model

import jakarta.persistence.*

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
    val certificateUrl: String? = null,

    @Column
    val patternData: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DegreeStatus,

    @Column
    val blockchainTxId: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var updatedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "degree", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val verifications: MutableList<VerificationRequest> = mutableListOf()
)