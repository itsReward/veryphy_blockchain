package com.veryphy.controller

import com.veryphy.dto.LoginRequest
import com.veryphy.dto.LoginResponse
import com.veryphy.security.JwtTokenProvider
import com.veryphy.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/*
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userService: UserService
)
{

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        try {
            // Authenticate using Spring Security
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginRequest.username,
                    loginRequest.password
                )
            )

            // Get user details
            val user = userService.getUserByUsername(loginRequest.username)

            // Generate JWT token
            val token = jwtTokenProvider.createToken(authentication)

            return ResponseEntity.ok(LoginResponse(token, user))
        } catch (e: AuthenticationException) {
            throw RuntimeException("Invalid username/password supplied")
        }
    }
}*/
