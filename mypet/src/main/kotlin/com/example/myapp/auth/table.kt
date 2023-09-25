package com.example.myapp.auth

import jakarta.annotation.PostConstruct
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Configuration

object Identities : LongIdTable("identity") {
    val secret = varchar("secret", 200)
    val userid = varchar("username", length = 100)
    val nickname = varchar("nickname", 100)
}

object Profiles : LongIdTable("profile") {
    val petname = varchar("petname", 100)
    val species = varchar("species", 100)
    val identityId = reference("identity_id", Identities ).nullable()
}

@Configuration
class AuthTableSetup(private val database: Database) {
    @PostConstruct
    fun migrateSchema() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Identities, Profiles)
        }
    }
}

