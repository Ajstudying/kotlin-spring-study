package com.example.myapp.auth

import com.example.myapp.auth.util.HashUtil
import com.example.myapp.auth.util.JwtUtil
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection

@Service
class AuthService(private val database: Database){
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun createIdentity(req: SignupRequest) : Long {
        // 기존에 있는 계정인지 확인
        val record = transaction {
            Identities.select{Identities.username eq req.username}.singleOrNull()
        }
        if(record != null){
            return 0;
        }

        // 300ms 걸렸음.. transaction에 묶는 처리 안 좋음.
        val secret = HashUtil.createHash(req.password)

        val profileId = transaction {
            //transaction 내부에서 예외처리 발생하면 자동 전체 rollback
            //기본적으로 auto-commit => transaction 내부 전체가 다 돌아갔을 때 commit이 됨.

            //try-catch구문이 transaction 내부에 있으면
            //예외처리 발생시에 catch로 가버림.
            //transaction 함수에서는 예외처리가 발생하지 않은 것으로 봄.
            //수동으로 catch구문에서 rollback()을 해줘야 함.
            try {
                // 1. identity 정보를 insert
                //identityId 이건 걍 컬럼. 그래서 아래 쪽에 long타입을 넣을 때 .value를 넣어야 값이 들어감.
                val identityId = Identities.insertAndGetId {
                    it[this.username] = req.username
                    it[this.secret] = secret
                }

                // 2. profile 정보를 insert(identity_id포함)
                val profileId = Profiles.insertAndGetId {
                    it[this.nickname] = req.nickname
                    it[this.email] = req.email
                    it[this.identityId] = identityId.value
//                    it[this.identityId] = 0
                }

                return@transaction profileId.value
            } catch (e: Exception) {
                rollback() //트라이 캐치 하면 이 롤백을 수동처리 해줘야 함. 트랜젝션 함수 안에서만 사용할 수 있음.
                //수동 롤백을 안해주면 그냥 진행이 돼서 db가 오염됨.
                logger.error(e.message)
                //현재 구문에서는 try-catch가 굳이 필요 없지만, logger로 어떤 에러메시지가 나오는지 보려면 써야 함.
                return@transaction 0
            }
        }

        return profileId
    }

//    @Transactional(readOnly = true, isolation = )
    fun authenticate(username: String, password: String) : Pair<Boolean, String> {

        //readOnly를 하게 되면 transaction id를 생성하지 않음.
        //MySQL 기본 격리수준, repeatable_read
        //다른 SQL DBMS는 기본 격리수준, read_committed

        //read_committed(병렬처리지만, 커밋된 것만 조회되게 함.)
        //txn = 1, select all- 오래걸림
        //txn = 2, insert - 빠르게 됨.
        //txn(2)번의 insert 결과가 txn(1)번의 select 결과에 반영이 됨.

        //repeatable_read(병렬처리지만, 요청한 순서대로 조회되게 함.)
        //txn = 1, select all- 오래걸림
        //txn = 2, insert - 빠르게 됨.
        //txn(2)번의 insert 결과가 txn(1)번의 select 결과에 반영이 안됨.
        val (result, payload) = transaction(database.transactionManager.defaultIsolationLevel, readOnly = true) {
            val i = Identities; //테이블 네임 별칭(alias)단축해서 쓸 수 있음.
            val p = Profiles;

            // 인증정보 조회
            val identityRecord = i.select{i.username eq username}.singleOrNull()
                ?: return@transaction Pair(false, mapOf("message" to "Unauthorized"))

            // 프로필정보 조회
            val profileRecord = p.select{p.identityId eq identityRecord[i.id].value}.singleOrNull()
                ?: return@transaction Pair(false, mapOf("message" to "Conflict"))

            return@transaction Pair(true, mapOf(
                "id" to profileRecord[p.id],
                "nickname" to profileRecord[p.nickname],
                "username" to identityRecord[i.username],
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
            payload["username"].toString(),
            payload["nickname"].toString()
        )

        return Pair(true, token)
    }
}
