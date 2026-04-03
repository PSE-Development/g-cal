package main.kotlin.database

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import java.time.LocalDateTime


/**
 * This file contains all the relational schemes used for the database.
 * Those are [Accounts], [Friends], [Events] and [Groups].
 */


const val ACCOUNT_TABLE = "accounts"
const val FRIEND_TABLE = "friends"
const val GROUP_TABLE = "groups"
const val EVENT_TABLE = "events"

interface AccountEntity : Entity<AccountEntity> {
    val username : String
    var name : String
    var experiencePoints : Int
    var experiencePointsToday : Int
}

object Accounts : Table<AccountEntity>(ACCOUNT_TABLE) {
    val username = varchar("username").primaryKey().bindTo { it.username }
    var name = varchar("name").bindTo { it.name }
    var experiencePoints = int("experiencePoints").bindTo { it.experiencePoints }
    var experiencePointsToday = int("experiencePointsToday").bindTo { it.experiencePointsToday }
}

interface FriendEntity : Entity<FriendEntity> {
    val initiatingUser : AccountEntity
    val befriendedUser : AccountEntity
}

object Friends : Table<FriendEntity>(FRIEND_TABLE) {
    val initiatingUser = varchar("initiatingUser").primaryKey().references(Accounts) { it.initiatingUser }
    val befriendedUser = varchar("befriendedUser").primaryKey().references(Accounts) { it.befriendedUser }
}

interface EventEntity : Entity<EventEntity> {
    val eventID : Long
    val eventType : String
    var name : String
    var description : String
    var end : LocalDateTime?
    var experiencePoints : Int
    val user : AccountEntity
    var completed : Boolean
    var completionDate : LocalDateTime?
    var groupID : Long
    var start : LocalDateTime?
}

object Events : Table<EventEntity>(EVENT_TABLE) {
    val eventID = long("eventID").primaryKey().bindTo { it.eventID}
    val eventType = varchar("eventType").bindTo { it.eventType }
    var name = varchar("name").bindTo { it.name }
    var description = varchar("description").bindTo { it.description }
    var end = datetime("end").bindTo { it.end }
    var experiencePoints = int("experiencePoints").bindTo { it.experiencePoints }
    val user = varchar("user").primaryKey().references(Accounts) { it.user }
    var completed = boolean("completed").bindTo { it.completed }
    var completionDate = datetime("completionDate").bindTo { it.completionDate }
    var groupID = long("groupID").bindTo { it.groupID }
    var start = datetime("start").bindTo { it.start }
}


interface GroupEntity : Entity<GroupEntity> {
    val groupID : Long
    val name : String
    val colour : Int
    val user : AccountEntity
}

object Groups : Table<GroupEntity>(GROUP_TABLE) {
    val groupID = long("groupID").primaryKey().bindTo { it.groupID }
    val name = varchar("name").bindTo { it.name }
    val colour = int("colour").bindTo { it.colour }
    val user = varchar("user").primaryKey().references(Accounts) { it.user }
}

