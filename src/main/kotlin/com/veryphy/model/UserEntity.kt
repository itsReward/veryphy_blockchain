package com.veryphy.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User entity for persistence
 */
@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val username: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val fullName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole,

    @Column
    val universityId: Long? = null,

    @Column
    val employerId: String? = null,

    @Column(nullable = false)
    var accountStatus: String = "ACTIVE",

    @Column
    var lastLogin: LocalDateTime? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var updatedAt: LocalDateTime? = null
)