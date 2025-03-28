package com.veryphy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication
@EnableWebMvc
class VeryphyBlockchainApplication

fun main(args: Array<String>) {
    runApplication<VeryphyBlockchainApplication>(*args)
}
