package main.kotlin.database

import io.ktor.http.HttpStatusCode
import main.kotlin.data.Result
import main.kotlin.data.config.Config
import main.kotlin.data.config.ConfigEntry
import org.ktorm.database.Database


/**
 * This class offers a static method to create a [Database] Object,
 * while getting the parameters from a given [Config] Object.
 */


class DatabaseConnect private constructor() {
    companion object {
        private var database: Database? = null

        /**
         * Creates a Database from the given configuration.
         * @param config the config
         * @return the database
         */
        fun connectDatabase(config: Config): Result<Database> {
            database?.let { return Result.Success(it) }

            val dbUrl = config.getValue(ConfigEntry.DB_URL).getOrElse { status, err ->
                return Result.Failure(status, err)
            }

            val driver = config.getValue(ConfigEntry.DB_DRIVER).getOrElse { status, err ->
                return Result.Failure(status, err)
            }

            val user = config.getValue(ConfigEntry.DB_USER).getOrElse { status, err ->
                return Result.Failure(status, err)
            }

            val password = config.getValue(ConfigEntry.DB_PASSWORD).getOrElse { status, err ->
                return Result.Failure(status, err)
            }

            try {
                database = Database.connect(
                    url = dbUrl,
                    driver = driver,
                    user = user,
                    password = password
                )
            }
            catch (e : Exception) {
                return Result.Failure(HttpStatusCode.BadRequest,
                    "Could not connect to database because of ${e.message}")
            }
            return Result.Success(database!!)
        }

        fun resetForTesting() {
            database = null
        }
    }
}
