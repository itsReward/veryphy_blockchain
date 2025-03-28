package com.veryphy.service

import com.veryphy.model.User
import com.veryphy.model.UserRole
import com.veryphy.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * Get user by username
     */
    fun getUserByUsername(username: String): User {
        val userEntity = userRepository.findByUsername(username)
            .orElseThrow { RuntimeException("User not found with username: $username") }

        return User(
            id = userEntity.id.toString(),
            username = userEntity.username,
            email = userEntity.email,
            role = userEntity.role,
            entityId = when(userEntity.role) {
                UserRole.UNIVERSITY -> userEntity.universityId?.toString()
                UserRole.EMPLOYER -> userEntity.employerId
                else -> null
            }
        )
    }

    /**
     * Create a new university user
     */
    @Transactional
    fun createUniversityUser(
        username: String,
        password: String,
        email: String,
        fullName: String,
        universityId: Long
    ): User {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent) {
            throw RuntimeException("Username already exists")
        }

        // Create new user
        val userEntity = com.veryphy.model.UserEntity(
            username = username,
            password = passwordEncoder.encode(password),
            email = email,
            fullName = fullName,
            role = UserRole.UNIVERSITY,
            universityId = universityId,
            accountStatus = "ACTIVE"
        )

        val savedUser = userRepository.save(userEntity)

        return User(
            id = savedUser.id.toString(),
            username = savedUser.username,
            email = savedUser.email,
            role = savedUser.role,
            entityId = savedUser.universityId.toString()
        )
    }

    /**
     * Create a new employer user
     */
    @Transactional
    fun createEmployerUser(
        username: String,
        password: String,
        email: String,
        fullName: String,
        employerId: String
    ): User {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent) {
            throw RuntimeException("Username already exists")
        }

        // Create new user
        val userEntity = com.veryphy.model.UserEntity(
            username = username,
            password = passwordEncoder.encode(password),
            email = email,
            fullName = fullName,
            role = UserRole.EMPLOYER,
            employerId = employerId,
            accountStatus = "ACTIVE"
        )

        val savedUser = userRepository.save(userEntity)

        return User(
            id = savedUser.id.toString(),
            username = savedUser.username,
            email = savedUser.email,
            role = savedUser.role,
            entityId = savedUser.employerId
        )
    }

    /**
     * Create a new admin user
     */
    @Transactional
    fun createAdminUser(
        username: String,
        password: String,
        email: String,
        fullName: String
    ): User {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent) {
            throw RuntimeException("Username already exists")
        }

        // Create new user
        val userEntity = com.veryphy.model.UserEntity(
            username = username,
            password = passwordEncoder.encode(password),
            email = email,
            fullName = fullName,
            role = UserRole.ADMIN,
            accountStatus = "ACTIVE"
        )

        val savedUser = userRepository.save(userEntity)

        return User(
            id = savedUser.id.toString(),
            username = savedUser.username,
            email = savedUser.email,
            role = savedUser.role,
            entityId = null
        )
    }

    /**
     * Update user password
     */
    @Transactional
    fun updatePassword(userId: Long, currentPassword: String, newPassword: String): Boolean {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found with id: $userId") }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, userEntity.password)) {
            return false
        }

        // Update password
        userEntity.password = passwordEncoder.encode(newPassword)
        userRepository.save(userEntity)

        return true
    }

    /**
     * Disable user account
     */
    @Transactional
    fun disableUser(userId: Long): Boolean {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found with id: $userId") }

        userEntity.accountStatus = "DISABLED"
        userRepository.save(userEntity)

        return true
    }
}