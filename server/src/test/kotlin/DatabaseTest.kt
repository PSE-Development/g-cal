import io.ktor.http.HttpStatusCode
import main.kotlin.data.Result
import main.kotlin.data.config.Config
import main.kotlin.data.config.ConfigEntry
import main.kotlin.database.DatabaseConnect
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension
import java.lang.reflect.Modifier

@ExtendWith(SystemStubsExtension::class)
class DatabaseTest {

    @SystemStub
    lateinit var variables : EnvironmentVariables

    @AfterEach
    fun resetDatabaseSingleton() {
        DatabaseConnect.resetForTesting()
    }

    // tests an invalid database connection by using a config with invalid values.
    @Test
    fun testInvalidDatabase() {
        val errorConfig = getInvalidConfig()
        val res = DatabaseConnect.connectDatabase(errorConfig)
        Assertions.assertTrue(res is Result.Failure)
        if (res is Result.Failure) Assertions.assertTrue(res.status == HttpStatusCode.BadRequest)
    }

    // test connecting to a valid database with a correct config object.
    @Test
    fun testValidDatabase() {
        val validConfig = getValidConfig()
        val res = DatabaseConnect.connectDatabase(validConfig)
        Assertions.assertTrue(res is Result.Success)
    }

    // asserts that the constructor is private and creates an instance
    @Test
    fun assertSingletonBehaviour() {
        val constructor = DatabaseConnect::class.java.getDeclaredConstructor()
        Assertions.assertTrue(Modifier.isPrivate(constructor.modifiers))

        constructor.isAccessible = true
        val instance = constructor.newInstance()
        Assertions.assertNotNull(instance)
        Assertions.assertTrue(instance is DatabaseConnect)
    }


    private fun getValidConfig() : Config {
        val dbUser = "sa"
        val dbPassword = ""
        val dbDriver = "org.h2.Driver"
        val dbUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=runscript from 'classpath:init.sql'"
        val serverUrl = "0.0.0.0"
        val serverPort = "8082"

        variables.set(ConfigEntry.DB_USER.name, dbUser)
        variables.set(ConfigEntry.DB_PASSWORD.name, dbPassword)
        variables.set(ConfigEntry.DB_DRIVER.name, dbDriver)
        variables.set(ConfigEntry.DB_URL.name, dbUrl)
        variables.set(ConfigEntry.SERVER_URL.name, serverUrl)
        variables.set(ConfigEntry.SERVER_PORT.name, serverPort)

        val res = Config.create()
        val validConfig = res.getOrElse { _, err -> Assertions.fail(err) }
        return validConfig
    }

    private fun getInvalidConfig() : Config {
        val res = Config.create()
        val errorConfig = res.getOrElse { _, err -> Assertions.fail(err) }
        return errorConfig
    }
}
