package com.veryphy.controller

import com.veryphy.dto.UniversityRegistrationRequest
import com.veryphy.dto.UniversityResponse
import com.veryphy.model.University
import com.veryphy.model.UniversityStatus
import com.veryphy.service.UniversityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/universities")
class UniversityController(private val universityService: UniversityService) {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun registerUniversity(@RequestBody request: UniversityRegistrationRequest): ResponseEntity<UniversityResponse> {
        val university = University(
            registrationId = "UNI-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            name = request.name,
            email = request.email,
            address = request.address ?: "",
            stakeAmount = request.stakeAmount,
            status = UniversityStatus.PENDING,
            joinDate = LocalDateTime.now()
        )

        val registeredUniversity = universityService.registerUniversity(university)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(registeredUniversity))
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    fun activateUniversity(@PathVariable id: Long): ResponseEntity<UniversityResponse> {
        val activatedUniversity = universityService.activateUniversity(id)
        return ResponseEntity.ok(mapToResponse(activatedUniversity))
    }

    @PutMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    fun blacklistUniversity(
        @PathVariable id: Long,
        @RequestParam reason: String
    ): ResponseEntity<UniversityResponse> {
        val blacklistedUniversity = universityService.blacklistUniversity(id, reason)
        return ResponseEntity.ok(mapToResponse(blacklistedUniversity))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY')")
    fun getUniversity(@PathVariable id: Long): ResponseEntity<UniversityResponse> {
        val university = universityService.getUniversityById(id)
        return ResponseEntity.ok(mapToResponse(university))
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUniversities(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<UniversityResponse>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val universities = universityService.getAllUniversities(pageable)
        val response = universities.map { mapToResponse(it) }.content
        return ResponseEntity.ok(response)
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUniversitiesByStatus(@PathVariable status: UniversityStatus): ResponseEntity<List<UniversityResponse>> {
        val universities = universityService.getUniversitiesByStatus(status)
        val response = universities.map { mapToResponse(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    fun searchUniversities(@RequestParam keyword: String): ResponseEntity<List<UniversityResponse>> {
        val universities = universityService.searchUniversities(keyword)
        val response = universities.map { mapToResponse(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    fun getStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = universityService.getStatistics()
        val response = mapOf(
            "totalUniversities" to statistics.totalUniversities,
            "activeUniversities" to statistics.activeUniversities,
            "pendingUniversities" to statistics.pendingUniversities,
            "blacklistedUniversities" to statistics.blacklistedUniversities
        )
        return ResponseEntity.ok(response)
    }

    private fun mapToResponse(university: University): UniversityResponse {
        return UniversityResponse(
            id = university.registrationId,
            name = university.name,
            email = university.email,
            address = university.address,
            stakeAmount = university.stakeAmount,
            status = university.status,
            joinDate = university.joinDate.format(DateTimeFormatter.ISO_DATE)
        )
    }
}