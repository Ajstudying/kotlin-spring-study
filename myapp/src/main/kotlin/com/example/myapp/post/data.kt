package com.example.myapp.post

// query-model, view-model
// domain-model(JPA entity)
data class PostResponse(
    val id: Long,
    val title : String,
    val content : String,
    val createdDate : String
)

data class PostCommentCountResponse(
    val id : Long,
    val title : String,
    val createdDate: String,
    val profileId : Long,
    val nickname: String,
    val commentCount : Long
)
//java
//String str = null;

// Kotlin
// val str : String = null; // error 코틀린은 뒤에 ?이걸 붙여야 nullable임.

// 기존의 java, String이 nullable
// {"title": "", "content": ""}
// {"title": ""} -> content null

// 필드가 not-nullable
data class PostCreateRequest(val title : String, val content: String)
//포스트 생성 요청 데이트 검증하는 메서드
fun PostCreateRequest.validate() : Boolean{
    return !(this.title.isEmpty() || this.content.isEmpty())
}
//아래처럼도 쓸 수 있음.
//fun PostCreateRequest.validate() = !(this.title.isEmpty() || this.content.isEmpty())

//제목만 수정하거나 컨텐츠만 수정할 수 있게 둘다 nullable
data class PostModifyRequest(val title : String?, val content: String?)