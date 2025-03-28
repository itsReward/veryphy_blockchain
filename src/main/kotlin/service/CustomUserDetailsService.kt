package com.veryphy.service

import com.veryphy.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User not found with username: $username") }

        // Create authorities based on user role
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))

        // Create Spring Security User (not to be confused with your domain User)
        return org.springframework.security.core.userdetails.User
            .withUsername(username)
            .password(user.password) // Assumed to be already encoded
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build()
    }
}