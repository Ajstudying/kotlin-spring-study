package com.example.myapp.board

import com.example.myapp.auth.Profile
import jakarta.websocket.Decoder.Binary
import jakarta.websocket.Encoder
import jdk.incubator.vector.VectorOperators


data class BoardResponse(
    val id: Long,
    val title : String,
    val content : String,
    val image : String,
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
    val id : Long,
    val content : String,
    val createdDate: String,
    val nickname: String
)

data class BoardCreateRequest(
        val request: String, val title: String, val content: String,
        val image: String, val petname: String, val species: String)

fun BoardCreateRequest.validate() =
    !(this.title.isEmpty() || this.content.isEmpty())

data class BoardModifyRequest(val title: String?, val content: String?)

data class CommentCreateRequest(val content: String)
