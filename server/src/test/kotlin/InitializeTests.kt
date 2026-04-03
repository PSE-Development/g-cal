import main.kotlin.controller.RequestDispatcher
import main.kotlin.data.EventCompletionDetail
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Group
import main.kotlin.data.events.Appointment
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.ToDo
import main.kotlin.event.ChallengeManager
import main.kotlin.event.EventManager
import main.kotlin.event.GroupManager
import main.kotlin.event.ResourceAuthorizer
import main.kotlin.user.UserAuthenticator
import main.kotlin.user.UserManager
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.ktorm.database.Database
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

abstract class InitializeTests {
    companion object {
        const val TOKEN_NON_EXISTENT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6InRlc3QiLCJhZG1pbiI6dHJ1ZSwiZW1haWwiOiJub25leGlzdGVudCIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxODcyNzI4MDc2fQ.CxeFUkZCFXWfVZ3KFi3e2ANw8QFV0NdzFyXNcGJ7tlA"
        const val TOKEN_EXPIRED = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6InRlc3QiLCJhZG1pbiI6dHJ1ZSwiZW1haWwiOiJub25leGlzdGVudCIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjM5MDIxfQ.sTurD1dW8Op_DE57LspoQO5DE2JzSr-5iXn0gAiJWKs"
        const val TOKEN_INVALID = "invalid"
        const val TOKEN_NO_EMAIL_CLAIM = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6InRlc3QiLCJhZG1pbiI6dHJ1ZSwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjF9.Gke2HAYhiGS-_weLC12v4TKnNDfhpJC-ik2XVD3Tcro"
        const val TOKEN_NO_EXP_CLAIM = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6InRlc3QiLCJhZG1pbiI6dHJ1ZSwiZW1haWwiOiJub25leGlzdGVudCIsImlhdCI6MTUxNjIzOTAyMiwiZXgiOjE1MTYyMzkwMjJ9.kXztWt09qL-KIYHGjeSDcFu5ec4nnwuEMOs3iaI2fTM"

        val logger : org.slf4j.Logger = LoggerFactory.getLogger(InitializeTests::class.java)

        lateinit var database: Database

        lateinit var groupManager: GroupManager
        lateinit var eventManager: EventManager
        lateinit var userManager: UserManager
        lateinit var challengeManager: ChallengeManager
        lateinit var userAuthenticator: UserAuthenticator
        lateinit var resourceAuthorizer: ResourceAuthorizer

        lateinit var dispatcher: RequestDispatcher


        @JvmStatic
        @BeforeAll
        fun initializeDatabaseConnection() {
            try {
                database = connectDatabase()
            } catch (e: Exception) {
                logger.error("Could not initialize database because of {}", e.message)
            }

            groupManager = GroupManager(database)
            eventManager = EventManager(database)
            userManager = UserManager(database)
            userAuthenticator = UserAuthenticator(database)
            challengeManager = ChallengeManager(database)
            resourceAuthorizer = ResourceAuthorizer(database)

            dispatcher = RequestDispatcher(userManager, eventManager, groupManager, challengeManager)

            println("Connected to database.")
        }

        fun connectDatabase(): Database {
            return Database.connect(
                url = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=runscript from 'classpath:init.sql'",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
        }
    }

    @BeforeEach
    fun clearDatabase() {
        // clearing the in-memory database so each test can run independently
        database.useConnection { conn ->
            val statement = conn.createStatement()
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE")

            statement.execute("TRUNCATE TABLE friends")
            statement.execute("TRUNCATE TABLE accounts")
            statement.execute("TRUNCATE TABLE events")
            statement.execute("TRUNCATE TABLE groups")

            statement.execute("SET REFERENTIAL_INTEGRITY TRUE")
        }

        // reset challenge manager to reset rotation index
        challengeManager = ChallengeManager(database)
    }

    fun generateGroup() : Group {
        val groupID = (0..1000).random().toLong()
        return Group(groupID, "test", 245)
    }

    fun generateAppointment() : Appointment {
        val eventID = (0..1000).random().toLong()
        val appointment = Appointment(eventID, "Meeting", "Besprechung der Aufgaben",
            LocalDateTime.of(2026, 2, 10, 14, 0),
            LocalDateTime.of(2026, 2, 10, 15, 30),
            Group(0, "None", 0), ExperiencePoints(50),
            EventCompletionDetail(eventID, false, null))
        return appointment
    }

    fun generateToDo() : ToDo {
        val eventID = (0..1000).random().toLong()
        val toDo = ToDo(eventID, "homework", "Do your homework",
            LocalDateTime.of(2026, 2, 10, 15, 30),
            ExperiencePoints(150),
            EventCompletionDetail(eventID, false, null))
        return toDo
    }

    fun generateSharedEvent() : SharedEvent {
        val eventID = (0..1000).random().toLong()
        val sharedEvent = SharedEvent(eventID, "Uni", "PSE",
            LocalDateTime.of(2026, 2, 11, 13, 0),
            LocalDateTime.of(2026, 2, 11, 16, 30),
            Group(0, "None", 0), ExperiencePoints(200),
            EventCompletionDetail(eventID, false, null))
        return sharedEvent
    }
}
