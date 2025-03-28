package com.veryphy.controller

import com.veryphy.dto.CertificateResponse
import com.veryphy.dto.DegreeRegistrationRequest
import com.veryphy.dto.DegreeResponse
import com.veryphy.model.Degree
import com.veryphy.model.DegreeStatus
import com.veryphy.repository.UniversityRepository
import com.veryphy.service.DegreeService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/degrees")
class DegreeController(
    private val degreeService: DegreeService,
    private val universityRepository: UniversityRepository
) {

    @PostMapping
    @PreAuthorize("hasRole('UNIVERSITY')")
    fun registerDegree(
        @RequestBody request: DegreeRegistrationRequest,
        authentication: Authentication
    ): ResponseEntity<DegreeResponse> {
        // Get university ID from authenticated user
        val user = authentication.principal as com.veryphy.model.User
        val universityId = user.entityId?.toLong()
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        val university = universityRepository.findById(universityId)
            .orElseThrow { RuntimeException("University not found") }

        val issueDate = LocalDateTime.parse(request.issueDate, DateTimeFormatter.ISO_DATE_TIME)

        val degree = Degree(
            degreeId = "DEG-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            studentId = request.studentId,
            studentName = request.studentName,
            degreeName = request.degreeName,
            university = university,
            issueDate = issueDate,
            degreeHash = "", // Will be generated by service
            status = DegreeStatus.REGISTERED
        )

        val registeredDegree = degreeService.registerDegree(degree)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(registeredDegree))
    }

    @PutMapping("/{degreeId}/revoke")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN')")
    fun revokeDegree(
        @PathVariable degreeId: String,
        @RequestParam reason: String
    ): ResponseEntity<DegreeResponse> {
        val revokedDegree = degreeService.revokeDegree(degreeId, reason)
        return ResponseEntity.ok(mapToResponse(revokedDegree))
    }

    @GetMapping("/{degreeId}")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN')")
    fun getDegree(@PathVariable degreeId: String): ResponseEntity<DegreeResponse> {
        val degree = degreeService.getDegreeByDegreeId(degreeId)
        return ResponseEntity.ok(mapToResponse(degree))
    }

    @GetMapping("/university/{universityId}")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN')")
    fun getDegreesByUniversity(
        @PathVariable universityId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<DegreeResponse>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val degrees = degreeService.getDegreesByUniversity(universityId, pageable)
        val response = degrees.map { mapToResponse(it) }.content
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN')")
    fun searchDegrees(
        @RequestParam universityId: Long,
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<DegreeResponse>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val degrees = degreeService.searchDegreesByUniversity(universityId, keyword, pageable)
        val response = degrees.map { mapToResponse(it) }.content
        return ResponseEntity.ok(response)
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN')")
    fun getDegreesByStudent(@PathVariable studentId: String): ResponseEntity<List<DegreeResponse>> {
        val degrees = degreeService.getDegreesByStudentId(studentId)
        val response = degrees.map { mapToResponse(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{degreeId}/certificate")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN', 'EMPLOYER')")
    fun getCertificate(@PathVariable degreeId: String): ResponseEntity<CertificateResponse> {
        val degree = degreeService.getDegreeByDegreeId(degreeId)

        // Load certificate from file system or generate on demand
        // This is a simplified implementation - in a real system you'd load the actual certificate
        val certificateImage = degree.certificateUrl?.let {
            java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(it))
        } ?: ByteArray(0)

        val response = CertificateResponse(
            degreeHash = degree.degreeHash,
            certificateImage = certificateImage,
            studentName = degree.studentName,
            degreeName = degree.degreeName,
            universityName = degree.university.name,
            issueDate = degree.issueDate.format(DateTimeFormatter.ISO_DATE)
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{degreeId}/history")
    @PreAuthorize("hasAnyRole('UNIVERSITY', 'ADMIN')")
    fun getDegreeHistory(@PathVariable degreeId: String): ResponseEntity<List<Map<String, String>>> {
        val history = degreeService.getDegreeHistory(degreeId)
        val response = history.map {
            mapOf(
                "txId" to it.txId,
                "timestamp" to it.timestamp,
                "degreeId" to it.degreeId,
                "status" to it.status,
                "action" to it.action
            )
        }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    fun getStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = degreeService.getStatistics()
        val response = mapOf(
            "totalDegrees" to statistics.totalDegrees,
            "registeredDegrees" to statistics.registeredDegrees,
            "verifiedDegrees" to statistics.verifiedDegrees,
            "revokedDegrees" to statistics.revokedDegrees,
            "recentDegrees" to statistics.recentDegrees
        )
        return ResponseEntity.ok(response)
    }

    private fun mapToResponse(degree: Degree): DegreeResponse {
        return DegreeResponse(
            id = degree.degreeId,
            studentId = degree.studentId,
            studentName = degree.studentName,
            degreeName = degree.degreeName,
            universityId = degree.university.registrationId,
            universityName = degree.university.name,
            issueDate = degree.issueDate.format(DateTimeFormatter.ISO_DATE),
            degreeHash = degree.degreeHash,
            status = degree.status
        )
    }
}