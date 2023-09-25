package com.example.myapp.board

import com.example.myapp.auth.Auth
import com.example.myapp.auth.AuthUser
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Connection
import java.time.LocalDateTime

@RestController
@RequestMapping("boards")
class BoardController {
    @Auth
    @GetMapping
    fun fetch() = transaction(){
        val b = Boards
        Boards.selectAll().map{ r -> BoardResponse(
            r[b.id], r[b.title], r[b.content], r[b.image], r[b.createdDate].toString()
        )}
    }
    @Auth
    @GetMapping("/paging")
    fun paging(@RequestParam size: Int, @RequestParam page: Int)
    : Page<BoardResponse> = transaction(Connection.TRANSACTION_READ_COMMITTED, readOnly = true)
    {
        val b = Boards
        val content = Boards.selectAll().orderBy(Boards.id to SortOrder.DESC)
            .limit(size, offset = (size * page).toLong())
            .map{
                r -> BoardResponse(
                    r[b.id],r[b.title],r[b.content], r[b.image], r[b.createdDate].toString()
                )
            }
        val totalCount = Boards.selectAll().count()
        return@transaction PageImpl(content, PageRequest.of(page, size), totalCount)
    }

    @Auth
    @GetMapping("/paging/search")
    fun searchPaging(
        @RequestParam size: Int, @RequestParam page: Int, @RequestParam keyword: String?)
    :Page<BoardResponse>
    = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {
        val b = Boards
        val query = when {
            keyword != null -> Boards.select {
                (b.title like "%${keyword}%") or (b.content like "%${keyword}%")
            }else -> b.selectAll()
        }
        val totalCount = query.count()

        val content = query.orderBy(b.id to SortOrder.DESC)
            .limit(size, offset = (size * page).toLong())
            .map { r ->
                BoardResponse(r[b.id], r[b.title], r[b.content], r[b.image], r[b.createdDate].toString())
            }
        return@transaction PageImpl(content, PageRequest.of(page, size), totalCount)
    }

//    @GetMapping("/commentCount")
//    fun fetchCommentCount
//                (@RequestParam size: Int, @RequestParam page: Int, @RequestParam keyword: String?)
//    :Page<BoardCommentCountResponse>
//    = transaction (Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {
//        val pf = Profiles
//        val b = Boards
//        val c = BoardComments
//
//        val commentCount = BoardComments.id.count()
//
//        val slices = ((b innerJoin pf) leftJoin c)
//            .slice(b.id, b.title, b.createdDate, pf.nickname, commentCount)
//        val query = when {
//            keyword != null ->
//                slices.select((b.title like "%${keyword}%") or (b.content like "%${keyword}%"))
//            else -> slices.selectAll()
//        }
//        val totalCount = query.count()
//        val content = query
//            .groupBy(b.id, b.title, b.createdDate,b.profileId, pf.nickname)
//            .orderBy(b.id to SortOrder.DESC)
//            .limit(size, offset = (size * page).toLong())
//            .map{ r -> BoardCommentCountResponse(
//                r[b.id], r[b.title], r[b.createdDate].toString(),
//                r[b.profileId].value, r[pf.nickname], r[commentCount]
//            )}
//        return@transaction PageImpl(content, PageRequest.of(size, page), totalCount)
//    }

    @GetMapping("/id/comment")
    fun fetchComment(@PathVariable id: Long) : ResponseEntity<Any>
    = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true){

        transaction { Boards.select(where = Boards.id eq id).firstOrNull() }
            ?: return@transaction ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val c = BoardComments
       val comments = c.select{c.boardId eq id}.map{
           r -> BoardCommentResponse(r[c.id].value, r[c.comment], r[c.createdDate].toString(), r[c.nickname])
       }.toList()

        return@transaction ResponseEntity.status(HttpStatus.OK).body(comments)
    }

    @Auth
    @PostMapping
    fun create(@RequestBody request: BoardCreateRequest): ResponseEntity<Any>{
        if(!request.validate()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "title and content fields are required"))
        }

        val result =  transaction {
            val result = Boards.insert{

                it[title] = request.title
                it[content] = request.content
//                it[image] = request.image.toByteArray(Charsets.UTF_8)
//                it[image] = Base64.getDecoder().decode(request.image)
                it[image] = request.image
                it[createdDate] = LocalDateTime.now()
            }.resultedValues ?: return@transaction false
            return@transaction true
        }
        if(result){
            return ResponseEntity.status(HttpStatus.CREATED).build()
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).build()
    }

    @Auth
    @PostMapping("/id/comment")
    fun createComment
                (@PathVariable id: Long, @RequestBody request: CommentCreateRequest, @RequestAttribute authUser: AuthUser)
    : ResponseEntity<Any?>{
        if(request.content.isNullOrEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        val c = BoardComments

        val foundB = transaction { Boards.select(where = Boards.id eq id).firstOrNull() }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val (result, response) = transaction {
            val result = BoardComments.insert {
                it[comment] = request.content
                it[createdDate] = LocalDateTime.now()
                it[boardId] = foundB[boardId]
                it[profileId] = authUser.id
                it[nickname] = authUser.nickname
            }.resultedValues
                ?: return@transaction Pair(false, null)

            val record = result.first()
            return@transaction Pair(true, BoardCommentResponse(
                record[c.id].value,
                record[c.comment],
                record[c.createdDate].toString(),
                record[c.nickname]
            ))
        }
        if(result){
            return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("data" to response))
        }

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(mapOf("data" to response, "error" to "conflict"))
    }

    @Auth
    @DeleteMapping("/{id}")
    fun remove(@PathVariable id: Long) : ResponseEntity<Any>{
        transaction { Boards.select(Boards.id eq id).firstOrNull() }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        transaction { Boards.deleteWhere { Boards.id eq id } }
        return ResponseEntity.ok().build()
    }

    @Auth
    @PutMapping("/id")
    fun modify(@PathVariable id: Long, @RequestBody request: BoardModifyRequest)
    : ResponseEntity<Any> {
        if(request.title.isNullOrEmpty() && request.content.isNullOrEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "title or content are required"))
        }

        transaction { Boards.select{ Boards.id eq id }.firstOrNull() }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        transaction { Boards.update({Boards.id eq id}) {
            if(!request.title.isNullOrEmpty()){
                it[title] = request.title
            }
            if(!request.content.isNullOrEmpty()){
                it[content] = request.content
            }
        }
        }
        return ResponseEntity.ok().build()
    }
}