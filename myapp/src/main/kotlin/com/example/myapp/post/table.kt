package com.example.myapp.post

import jakarta.annotation.PostConstruct
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Configuration

//exposed kotlin ORM(DSL, domain-specific language)
// exposed 테이블 객체명은 파스칼케이스에 복수로 쓰고 있음.
object Posts : Table("post") {
    //val 필드명 = 컬럼타입("컬럼명")
    val id = long("id").autoIncrement()
    val title = varchar("title", 100)
//    val content = text("content").nullable() // 널 가능
    val content = text("content") // 기본값이 null불가능.
    val createdDate  = datetime("created_date")

    //primary key 설정(제약조건)
    override val primaryKey = PrimaryKey(id, name = "pk_post_id")
}
//id가 long(mysql bigint)이고 primary key, auto_increment 필드/컬럼 생성
object PostComments : LongIdTable("post_comment") {
    // foreign key를 넣으면 됨.
    // val 필드명 = reference("컬럼명", 참조필드)
    val postId = reference("post_id", Posts.id)
    //아래처럼 이름을 붙일 수도 있음
//    val postId = reference("post_id", Posts.id, fkName = "fk_post_comment_post_id")
    val comment = text("comment")


}
//테이블 생성 코드
//database: Database 이 부분이 DatabaseConfiguration에 두번째 bean임. <<의존성 주입
@Configuration
class PostTableSetUp(private val database: Database) {
    //migrate(이주하다) : 코드 -> DB

    //의존성 객체 생성 및 주입이 완료된 후에 실행할 코드를 작성
    // 스프링 환경구성이 끝난 후에 실행
    @PostConstruct
    fun migrateSchema() {
        //expose 라이브러리에서는 모든 SQL처리는
        // transaction 함수의 statement람다함수 안에서 처리를 해야 함.
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Posts, PostComments)
        }
    }

}