package com.veryphy.model

data class Verification(
    val id: String,
    val employerId: String,
    val degreeHash: String,
    val universityId: String,
    val verificationDate: String,
    val paymentAmount: Double,
    val paymentStatus: String
)