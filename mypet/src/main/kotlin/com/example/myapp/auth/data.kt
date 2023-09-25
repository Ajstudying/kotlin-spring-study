package com.example.myapp.auth


data class SignupRequest (
     val userId: String,
     val password: String,
     val nickname: String,
     val profileList: List<Profile>,
)
data class Profile (
        val petname : String,
        val species : String,
)

data class AuthUser (
     val id: Long = 0, // 프로필 id
     val username: String, // 로그인 사용자이름
     val nickname: String, // 프로필 별칭
)

