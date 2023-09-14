package com.example.myapp.auth

import com.example.myapp.auth.util.HashUtil
import com.example.myapp.auth.util.JwtUtil
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthService(private val database: Database){
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun createIdentity(req: SignupRequest) : Long {
        // 기존에 있는 계정인지 확인
        val record = transaction {
            Identities.select(Identities.userid eq req.userId).singleOrNull()
        }
        if(record != null){
            return 0
        }

        val secret = HashUtil.createHash(req.password)

        val identityId = transaction {
            val identityId = Identities.insertAndGetId {
                it[this.userid] = req.userId
                it[this.secret] = secret
                it[this.nickname] = req.nickname
            }
            req.profileList.forEach { pro ->
                Profiles.insert {
                    it[this.petName] = pro.petName
                    it[this.species] = pro.species
                    it[this.identityId] = identityId.value
                }
            }
            return@transaction identityId.value
        }
        return identityId
    }

    fun authenticate(userid: String, password: String) : Pair<Boolean, String> {
        val (result, payload) = transaction(database.transactionManager.defaultIsolationLevel, readOnly = true) {
            val i = Identities;

            // 인증정보 조회
            val identityRecord = i.select(i.userid eq userid).singleOrNull()
                ?: return@transaction Pair(false, mapOf("message" to "Unauthorized"))

            return@transaction Pair(true, mapOf(
                "id" to identityRecord[i.id],
                "nickname" to identityRecord[i.nickname],
                "userid" to identityRecord[i.userid],
                "secret" to identityRecord[i.secret]
            ))
        }

        if(!result) {
            return Pair(false, payload["message"].toString());
        }

        //   password+salt -> 해시 -> secret 일치여부 확인
        val isVerified = HashUtil.verifyHash(password, payload["secret"].toString())
        if (!isVerified) {
            return Pair(false, "Unauthorized")
        }

        val token = JwtUtil.createToken(
            payload["id"].toString().toLong(),
            payload["userid"].toString(),
            payload["nickname"].toString()
        )

        return Pair(true, token)
    }
}