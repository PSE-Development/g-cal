import io.ktor.http.HttpStatusCode
import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Result
import main.kotlin.data.config.Config
import main.kotlin.data.config.ConfigEntry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension

@ExtendWith(SystemStubsExtension::class)
class ConfigTest {

    @SystemStub
    lateinit var variables : EnvironmentVariables

    // tests that retrieving a config objects value for each key works by mocking the creation of the config
    // object since environment variables, which are normally used for configuring the values, can not be mocked.
    @Test
    fun testConfigCreation() {
        val dbUser = "owner"
        val dbPassword = "sd4c744"
        val dbDriver = "org.postgresql.Driver"
        val dbUrl = "jdbc:postgresql://server-database:5432/server-database"
        val serverUrl = "193.196.39.178"
        val serverPort = "8082"

        variables.set(ConfigEntry.DB_USER.name, dbUser)
        variables.set(ConfigEntry.DB_PASSWORD.name, dbPassword)
        variables.set(ConfigEntry.DB_DRIVER.name, dbDriver)
        variables.set(ConfigEntry.DB_URL.name, dbUrl)
        variables.set(ConfigEntry.SERVER_URL.name, serverUrl)
        variables.set(ConfigEntry.SERVER_PORT.name, serverPort)

        val res = Config.create()
        val config = res.getOrElse { _, err -> Assertions.fail(err) }

        Assertions.assertEquals(serverUrl, config.getValue(ConfigEntry.SERVER_URL).
        getOrElse { _, err -> Assertions.fail(err) })
        Assertions.assertEquals(serverPort, config.getValue(ConfigEntry.SERVER_PORT).
        getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(dbUrl, config.getValue(ConfigEntry.DB_URL).
        getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(dbDriver, config.getValue(ConfigEntry.DB_DRIVER).
        getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(dbUser, config.getValue(ConfigEntry.DB_USER).
        getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(dbPassword, config.getValue(ConfigEntry.DB_PASSWORD).
        getOrElse { _, err -> Assertions.fail(err)  })
    }

    // if no env variables are set, default values are used.
    @Test
    fun emptyVariableConfig() {
        val res = Config.create()
        val config = res.getOrElse { _, err -> Assertions.fail(err) }

        Assertions.assertEquals(ConfigEntry.SERVER_URL.defaultValue,
            config.getValue(ConfigEntry.SERVER_URL).getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(ConfigEntry.SERVER_PORT.defaultValue,
            config.getValue(ConfigEntry.SERVER_PORT).getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(ConfigEntry.DB_USER.defaultValue,
            config.getValue(ConfigEntry.DB_USER).getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(ConfigEntry.DB_PASSWORD.defaultValue,
            config.getValue(ConfigEntry.DB_PASSWORD).getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(ConfigEntry.DB_DRIVER.defaultValue,
            config.getValue(ConfigEntry.DB_DRIVER).getOrElse { _, err -> Assertions.fail(err)  })
        Assertions.assertEquals(ConfigEntry.DB_URL.defaultValue,
            config.getValue(ConfigEntry.DB_URL).getOrElse { _, err -> Assertions.fail(err)  })
    }


    // test onSuccess method of Result
    @Test
    fun testSuccessResult() {
        val res = Result.Success(20)
        var check = 0
        res
            .onFailure { _, _ -> Assertions.fail() }
            .onSuccess { value -> check = value }
        Assertions.assertEquals(20, check)
    }

    // test onFailure method of Result
    @Test
    fun testFailureResult() {
        val res = Result.Failure<String>(HttpStatusCode.BadRequest, "Invalid operation")
        var check = 0
        res
            .onSuccess { Assertions.fail() }
            .onFailure { status, _ -> check = status.value }
        Assertions.assertEquals(400, check)
    }

    // test getOrElse method of Result in case of Success
    @Test
    fun testSuccessGetOrElse() {
        val username = "testUser"
        val res = Result.Success(Account(username, "name", ExperiencePoints(0), ExperiencePoints(0)))
        val user = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertEquals(username, user.username)
        Assertions.assertEquals(0, user.experiencePoints.value)
    }

    // test getOrElse method of Result in case of Failure
    @Test
    fun testFailureGetOrElse() {
        val errorMessage = "No access"
        val res = Result.Failure<String>(HttpStatusCode.Forbidden, errorMessage)
        val user = res.getOrElse { _, err -> err }
        Assertions.assertEquals(errorMessage, user)
    }
}