package main.kotlin.data.config

/**
 * This class models the configuration entries of the [Config].
 */
enum class ConfigEntry (val identifier : String, val defaultValue : String) {
    /**
     * The url of the database.
     */
    DB_URL ("db-url", "jdbc:postgresql://database:5432/database"),
    /**
     * The driver of the database.
     */
    DB_DRIVER ("db-driver", "org.postgresql.Driver"),

    /**
     * The database user.
     */
    DB_USER ("db-user", ""),

    /**
     * The password for the database user.
     */
    DB_PASSWORD ("db-password", ""),

    /**
     * The port of the ktor server.
     */
    SERVER_PORT ("server-port", "8080"),

    /**
     * The url of the ktor server.
     */
    SERVER_URL ("server-url", "0.0.0.0");
}