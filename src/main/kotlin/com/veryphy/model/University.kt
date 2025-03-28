package com.veryphy.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * University entity
 */
@Entity
@Table(name = "universities")
data class University(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val registrationId: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column
    val address: String,

    @Column(nullable = false)
    val stakeAmount: Double,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UniversityStatus,

    @Column(nullable = false)
    val joinDate: LocalDateTime = LocalDateTime.now(),

    @Column
    var blockchainId: String? = null,

    @OneToMany(mappedBy = "university", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val degrees: MutableList<Degree> = mutableListOf()
)