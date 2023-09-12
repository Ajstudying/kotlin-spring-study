package com.example.myapp.board

data class BoardResponse(
    val id: Long,
    val title : String,
    val content : String,
    val createdDate : String
)
data class BoardCommentCountResponse(
    val id : Long,
    val title : String,
    val createdDate: String,
    val profileId : Long,
    val nickname: String,
    val commentCount : Long
)

data class BoardCommentResponse(
    val content : String,
    val createdDate: String,
    val nickname: String
)

data class BoardCreateRequest(val title: String, val content: String)

fun BoardCreateRequest.validate() =
    !(this.title.isEmpty() || this.content.isEmpty())

data class BoardModifyRequest(val title: String?, val content: String?)

data class CommentCreateRequest(val content: String)
