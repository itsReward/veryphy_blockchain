package com.veryphy.repository

import com.veryphy.model.VerificationRequest
import com.veryphy.model.VerificationResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface VerificationRepository : JpaRepository<VerificationRequest, Long> {

    fun findByRequestId(requestId: String): Optional<VerificationRequest>

    fun findByEmployerId(employerId: String, pageable: Pageable): Page<VerificationRequest>

    fun findByDegreeId(degreeId: Long, pageable: Pageable): Page<VerificationRequest>

    fun findByResult(result: VerificationResult, pageable: Pageable): Page<VerificationRequest>

    @Query("""
        SELECT v FROM VerificationRequest v 
        WHERE v.requestDate BETWEEN :startDate AND :endDate
    """)
    fun findByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<VerificationRequest>

    @Query("""
        SELECT v FROM VerificationRequest v 
        JOIN v.degree d 
        WHERE d.university.id = :universityId
    """)
    fun findByUniversityId(@Param("universityId") universityId: Long, pageable: Pageable): Page<VerificationRequest>

    @Query("""
        SELECT COUNT(v) FROM VerificationRequest v 
        WHERE v.result = :result
    """)
    fun countByResult(@Param("result") result: VerificationResult): Long

    @Query("""
        SELECT SUM(v.paymentAmount) FROM VerificationRequest v 
        WHERE v.result = :result
    """)
    fun sumPaymentAmountByResult(@Param("result") result: VerificationResult): Double

    @Query("""
        SELECT COUNT(v) FROM VerificationRequest v 
        JOIN v.degree d 
        WHERE d.university.id = :universityId 
        AND v.result = :result
    """)
    fun countByUniversityIdAndResult(
        @Param("universityId") universityId: Long,
        @Param("result") result: VerificationResult
    ): Long

    @Query("""
        SELECT v FROM VerificationRequest v
        JOIN FETCH v.degree d
        JOIN FETCH d.university
        WHERE v.requestId = :requestId
    """)
    fun findByRequestIdWithDetails(@Param("requestId") requestId: String): Optional<VerificationRequest>

    @Query("""
        SELECT v FROM VerificationRequest v
        WHERE v.employerId = :employerId
        ORDER BY v.requestDate DESC
    """)
    fun findRecentByEmployerId(@Param("employerId") employerId: String, pageable: Pageable): List<VerificationRequest>
}