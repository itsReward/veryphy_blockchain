package com.veryphy.controller

import com.veryphy.dto.LoginRequest
import com.veryphy.dto.LoginResponse
import com.veryphy.dto.RegistrationRequest
import com.veryphy.dto.PasswordUpdateRequest
import com.veryphy.model.User
import com.veryphy.model.UserRole
import com.veryphy.security.JwtTokenProvider
import com.veryphy.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @PostMapping("/login")
    fun login(@RequestBody @Validated loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        return try {
            // Authenticate using Spring Security
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginRequest.username,
                    loginRequest.password
                )
            )

            // Set authentication in security context
            SecurityContextHolder.getContext().authentication = authentication

            // Get user details
            val user = userService.getUserByUsername(loginRequest.username)

            // Check if user's role matches the requested role
            if (user.role != loginRequest.role) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null)
            }

            // Generate JWT token
            val token = jwtTokenProvider.createToken(authentication)

            ResponseEntity.ok(LoginResponse(token, user))
        } catch (e: AuthenticationException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(null)
        }
    }

    @PostMapping("/register/university")
    @PreAuthorize("hasRole('ADMIN')")
    fun registerUniversityUser(
        @RequestBody @Validated request: RegistrationRequest
    ): ResponseEntity<User> {
        if (request.universityId == null) {
            return ResponseEntity.badRequest().body(null)
        }

        val user = userService.createUniversityUser(
            username = request.username,
            password = request.password,
            email = request.email,
            fullName = request.fullName,
            universityId = request.universityId
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PostMapping("/register/employer")
    @PreAuthorize("hasRole('ADMIN')")
    fun registerEmployerUser(
        @RequestBody @Validated request: RegistrationRequest
    ): ResponseEntity<User> {
        if (request.employerId == null) {
            return ResponseEntity.badRequest().body(null)
        }

        val user = userService.createEmployerUser(
            username = request.username,
            password = request.password,
            email = request.email,
            fullName = request.fullName,
            employerId = request.employerId
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun registerAdminUser(
        @RequestBody @Validated request: RegistrationRequest
    ): ResponseEntity<User> {
        val user = userService.createAdminUser(
            username = request.username,
            password = request.password,
            email = request.email,
            fullName = request.fullName
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    fun updatePassword(
        @RequestBody @Validated request: PasswordUpdateRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val user = authentication.principal as org.springframework.security.core.userdetails.UserDetails
        val userId = userService.getUserByUsername(user.username).id.toLong()

        val updated = userService.updatePassword(
            userId = userId,
            currentPassword = request.currentPassword,
            newPassword = request.newPassword
        )

        return if (updated) {
            ResponseEntity.ok(mapOf("success" to true, "message" to "Password updated successfully"))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("success" to false, "message" to "Current password is incorrect"))
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    fun getUserProfile(authentication: Authentication): ResponseEntity<User> {
        val user = authentication.principal as org.springframework.security.core.userdetails.UserDetails
        val userDetails = userService.getUserByUsername(user.username)
        return ResponseEntity.ok(userDetails)
    }

    @PutMapping("/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    fun disableUser(@PathVariable userId: Long): ResponseEntity<Map<String, Any>> {
        val success = userService.disableUser(userId)
        return if (success) {
            ResponseEntity.ok(mapOf("success" to true, "message" to "User disabled successfully"))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("success" to false, "message" to "User not found"))
        }
    }
}