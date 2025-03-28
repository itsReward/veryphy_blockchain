package com.veryphy.repository

import com.veryphy.model.Degree
import com.veryphy.model.DegreeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface DegreeRepository : JpaRepository<Degree, Long> {

    fun findByDegreeId(degreeId: String): Optional<Degree>

    fun findByDegreeHash(degreeHash: String): Optional<Degree>

    fun findByUniversityIdAndStatus(universityId: Long, status: DegreeStatus, pageable: Pageable): Page<Degree>

    fun findByStudentId(studentId: String): List<Degree>

    fun findByStudentName(studentName: String): List<Degree>

    @Query("SELECT d FROM Degree d WHERE d.university.id = :universityId")
    fun findByUniversityId(@Param("universityId") universityId: Long, pageable: Pageable): Page<Degree>

    @Query("""
        SELECT d FROM Degree d 
        WHERE d.university.id = :universityId 
        AND (d.studentName LIKE %:keyword% OR d.studentId LIKE %:keyword% OR d.degreeName LIKE %:keyword%)
    """)
    fun searchByUniversityAndKeyword(
        @Param("universityId") universityId: Long,
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<Degree>

    @Query("""
        SELECT d FROM Degree d 
        WHERE (d.studentName LIKE %:keyword% OR d.studentId LIKE %:keyword% OR d.degreeName LIKE %:keyword%)
    """)
    fun searchByKeyword(@Param("keyword") keyword: String, pageable: Pageable): Page<Degree>

    @Query("""
        SELECT COUNT(d) FROM Degree d 
        WHERE d.status = :status
    """)
    fun countByStatus(@Param("status") status: DegreeStatus): Long

    @Query("""
        SELECT COUNT(d) FROM Degree d 
        WHERE d.createdAt BETWEEN :startDate AND :endDate
    """)
    fun countByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    @Query("""
        SELECT d FROM Degree d
        JOIN FETCH d.university
        LEFT JOIN FETCH d.verifications
        WHERE d.degreeId = :degreeId
    """)
    fun findByDegreeIdWithDetails(@Param("degreeId") degreeId: String): Optional<Degree>
}