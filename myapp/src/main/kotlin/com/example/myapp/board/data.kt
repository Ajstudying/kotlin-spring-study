package com.example.myapp.board

data class BoardResponse(
    val id: Long,
    val title : String,
    val content : String,
    val createdDate : String
)