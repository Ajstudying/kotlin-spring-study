package com.example.myapp.post

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

//query-model, view-model
//domain-model(JPA entity)
data class PostResponse(val id : Long, val title : String, val content: String, val createdDate: String)

@RestController
@RequestMapping("posts")
class PostController {

    // exposed selectAll -> List<ResultRow>
    // ResultRow는 transaction {} 구문 밖에서 접근 불가능함
    //transaction 구분 외부로 보낼 때는 별도의 객체로 변환해서 내보낸다.
    @GetMapping
    fun fetch() = transaction(){
        Posts.selectAll().map { r -> PostResponse(
                r[Posts.id], r[Posts.title], r[Posts.content], r[Posts.createdDate].toString()
        ) }
    }
}