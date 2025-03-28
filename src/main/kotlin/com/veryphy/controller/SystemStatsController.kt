package com.veryphy.controller

import com.veryphy.dto.SystemStatsResponse
import com.veryphy.service.DegreeService
import com.veryphy.service.UniversityService
import com.veryphy.service.VerificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/system")
class SystemStatsController(
    private val universityService: UniversityService,
    private val degreeService: DegreeService,
    private val verificationService: VerificationService
) {

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    fun getSystemStats(): ResponseEntity<SystemStatsResponse> {
        // Get statistics from each service
        val universityStats = universityService.getStatistics()
        val degreeStats = degreeService.getStatistics()
        val verificationStats = verificationService.getStatistics()

        // Combine statistics into a single response
        val response = SystemStatsResponse(
            registeredUniversities = universityStats.totalUniversities.toInt(),
            activeUniversities = universityStats.activeUniversities.toInt(),
            totalDegrees = degreeStats.totalDegrees.toInt(),
            verificationCount = verificationStats.totalVerifications.toInt(),
            successRate = verificationStats.successRate,
            timestamp = Date()
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/stats/public")
    fun getPublicStats(): ResponseEntity<Map<String, Any>> {
        // Get statistics from each service
        val universityStats = universityService.getStatistics()
        val degreeStats = degreeService.getStatistics()
        val verificationStats = verificationService.getStatistics()

        // Create a simplified response with non-sensitive information
        val response = mapOf(
            "universities" to universityStats.activeUniversities,
            "degrees" to degreeStats.totalDegrees,
            "verifications" to verificationStats.totalVerifications,
            "successRate" to verificationStats.successRate,
            "lastUpdated" to Date()
        )

        return ResponseEntity.ok(response)
    }
}