package com.example.myapp.board

import com.example.myapp.auth.Profiles
import jakarta.annotation.PostConstruct
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Configuration

object Boards : Table("board"){
    val id = long("id").autoIncrement()
    val title = varchar("title", 100)
    val content = text("content")
    val createdDate = datetime("created_date")
//    val profileId = reference("profile_id", Profiles)
//    val image = varchar("image", length = 1024)
    val image = registerColumn<String>("image_long_text", LongTextColumnType())


    override val primaryKey = PrimaryKey(id, name = "pk_board_id")
}

class LongTextColumnType : IColumnType {
    override var nullable: Boolean = true
    override fun sqlType(): String = "LONGTEXT"

//    override fun valueFromDB(value: Any): Any {
//        return value.toString()
//    }
//
//    override fun notNullValueToDB(value: Any): Any {
//        return value.toString()
//    }

    // If needed, you can also implement other methods.
}

object BoardComments : LongIdTable("board_comment") {
    val boardId = reference("board_id", Boards.id)
    val comment = text("comment")
    val createdDate = datetime("created_date")
    val profileId = reference("profile_id", Profiles)
    val nickname = text("nickname")
}

@Configuration
class BoardTableSetUp(private val database: Database){
    @PostConstruct
    fun migrateSchema() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Boards, BoardComments)
        }
    }
}