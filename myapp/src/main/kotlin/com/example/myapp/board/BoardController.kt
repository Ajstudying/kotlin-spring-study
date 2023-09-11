package com.example.myapp.board

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Connection

@RestController
@RequestMapping("boards")
class BoardController {
    @GetMapping
    fun fetch() = transaction(){
        val b = Boards
        Boards.selectAll().map{ r -> BoardResponse(
            r[b.id], r[b.title], r[b.content], r[b.createdDate].toString()
        )}
    }
    @GetMapping("/paging")
    fun paging(@RequestParam size: Int, @RequestParam page: Int)
    : Page<BoardResponse> = transaction(Connection.TRANSACTION_READ_COMMITTED, readOnly = true)
    {
        val b = Boards
        val content = Boards.selectAll().orderBy(Boards.id to SortOrder.DESC)
            .limit(size, offset = (size * page).toLong())
            .map{
                r -> BoardResponse(
                    r[b.id],r[b.title],r[b.content],r[b.createdDate].toString()
                )
            }
        val totalCount = Boards.selectAll().count()
        return@transaction PageImpl(content, PageRequest.of(page, size), totalCount)
    }

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
                BoardResponse(r[b.id], r[b.title], r[b.content], r[b.createdDate].toString())
            }
        return@transaction PageImpl(content, PageRequest.of(page, size), totalCount)
    }

}