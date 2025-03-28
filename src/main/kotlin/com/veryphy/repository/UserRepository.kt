package com.veryphy.repository

import com.veryphy.model.UserEntity
import com.veryphy.model.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {

    fun findByUsername(username: String): Optional<UserEntity>

    fun findByEmail(email: String): Optional<UserEntity>

    @Query("SELECT u FROM UserEntity u WHERE u.role = :role")
    fun findByRole(@Param("role") role: UserRole): List<UserEntity>

    @Query("SELECT u FROM UserEntity u WHERE u.universityId = :universityId")
    fun findByUniversityId(@Param("universityId") universityId: Long): List<UserEntity>

    @Query("SELECT u FROM UserEntity u WHERE u.employerId = :employerId")
    fun findByEmployerId(@Param("employerId") String): List<UserEntity>
}