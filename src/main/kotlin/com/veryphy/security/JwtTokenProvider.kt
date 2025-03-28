package com.veryphy.security

import com.veryphy.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${security.jwt.secret}") secretKey: String,
    @Value("\${security.jwt.expiration}") private val validityInMs: Long
) {
    private val key: SecretKey

    init {
        // Convert the secret string to a secure key
        val keyBytes = secretKey.toByteArray(Charsets.UTF_8)
        this.key = Keys.hmacShaKeyFor(keyBytes)
    }

    fun createToken(authentication: Authentication): String {
        val user = authentication.principal as org.springframework.security.core.userdetails.UserDetails
        val claims: Claims = Jwts.claims().setSubject(user.username)
        val authorities = user.authorities.map { it.authority }
        claims["roles"] = authorities

        val now = Date()
        val validity = Date(now.time + validityInMs)

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getAuthentication(token: String): Authentication {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        val username = claims.subject

        @Suppress("UNCHECKED_CAST")
        val roles = claims["roles"] as List<String>
        val authorities = roles.map { SimpleGrantedAuthority(it) }

        val userDetails = org.springframework.security.core.userdetails.User
            .withUsername(username)
            .password("") // Not needed for token authentication
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build()

        return UsernamePasswordAuthenticationToken(userDetails, "", authorities)
    }

    fun validateToken(token: String): Boolean {
        try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)

            // Check if token is expired
            return !claims.body.expiration.before(Date())
        } catch (e: JwtException) {
            return false
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    fun getUsername(token: String): String {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
    }
}