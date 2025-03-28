package com.veryphy.repository

import com.veryphy.model.University
import com.veryphy.model.UniversityStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UniversityRepository : JpaRepository<University, Long> {

    fun findByRegistrationId(registrationId: String): Optional<University>

    fun findByEmail(email: String): Optional<University>

    fun findByStatus(status: UniversityStatus): List<University>

    @Query("SELECT u FROM University u WHERE u.name LIKE %:keyword% OR u.email LIKE %:keyword%")
    fun searchByNameOrEmail(@Param("keyword") keyword: String): List<University>

    @Query("SELECT COUNT(u) FROM University u WHERE u.status = :status")
    fun countByStatus(@Param("status") status: UniversityStatus): Long

    @Query("""
        SELECT u FROM University u
        JOIN FETCH u.degrees d
        WHERE u.id = :universityId
    """)
    fun findByIdWithDegrees(@Param("universityId") universityId: Long): Optional<University>

    @Query("""
        SELECT DISTINCT u FROM University u
        JOIN u.degrees d
        WHERE d.createdAt >= :startDate
        ORDER BY SIZE(u.degrees) DESC
    """)
    fun findTopUniversitiesByDegreeCount(@Param("startDate") startDate: java.time.LocalDateTime): List<University>
}