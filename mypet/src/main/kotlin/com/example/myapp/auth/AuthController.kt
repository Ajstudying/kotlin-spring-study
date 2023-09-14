package com.example.myapp.auth

import com.example.myapp.auth.util.JwtUtil
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.jetbrains.exposed.sql.Database
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*
import com.example.myapp.auth.util.HashUtil
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMethod




@RestController
@RequestMapping("/auth")
class AuthController(private val service: AuthService) {
    @PostMapping(value = ["/signup"])
    fun signUp(@RequestBody req: SignupRequest): ResponseEntity<Any> {
        println("123")
        val identityId = service.createIdentity(req)

        if(identityId > 0) {
            // 3. Response
            // 201: created
            return ResponseEntity.status(HttpStatus.CREATED).build()
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PostMapping(value = ["/signin"])
    fun signIn(
        @RequestParam userid: String,
        @RequestParam password: String,
        res: HttpServletResponse,
    ): ResponseEntity<*> {

        val (result, message) = service.authenticate(userid, password)
        if(result) {
            // 3. cookie와 헤더를 생성한후 리다이렉트
            val cookie = Cookie("token", message)
            cookie.path = "/"
            cookie.maxAge = (JwtUtil.TOKEN_TIMEOUT / 1000L).toInt() // 만료시간
            cookie.domain = "localhost" // 쿠키를 사용할 수 있 도메인

            // 응답헤더에 쿠키 추가
            res.addCookie(cookie)

            // 웹 첫페이지로 리다이렉트
            return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(
                    ServletUriComponentsBuilder
                        .fromHttpUrl("http://localhost:5500")
                        .build().toUri()
                )
                .build<Any>()
        }

        // 오류 메시지 반환
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(
                ServletUriComponentsBuilder
                    .fromHttpUrl("http://localhost:5500/login.html?err=$message")
                    .build().toUri()
            )
            .build<Any>()
    }
}
