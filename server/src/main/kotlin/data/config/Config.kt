package main.kotlin.data.config

import io.ktor.http.HttpStatusCode
import main.kotlin.data.Result

/**
 * This class offers a static method for reading the configuration of database and server from environment variables
 * used for the docker container.
 * The parsed values are stored in a Map, mapping each [ConfigEntry] to their respective value.
 */
class Config private constructor() {
    private val values = mutableMapOf<ConfigEntry, String>()

    companion object{
        /**
         * Creates a config object with a filled Map.
         * Reads the environment variables for each [ConfigEntry], if they are not set, the [ConfigEntry.defaultValue]
         * is used.
         * @return [Result.Success] of the [Config] object
         */
        fun create() : Result<Config> {
            val cfg = Config()
            for (entry in ConfigEntry.entries) {
                cfg.values[entry] = System.getenv(entry.name) ?: entry.defaultValue
            }
            return Result.Success(cfg)
        }
    }

    /**
     * Returns the value in the map for a given [ConfigEntry].
     * Returns [Result.Failure] if there is no value present.
     * @param entry the config entry
     * @return [Result.Success] of the value, [Result.Failure] otherwise
     */
    fun getValue(entry : ConfigEntry) : Result<String> {
        val value : String = values[entry]
            ?: return Result.Failure(HttpStatusCode.BadRequest, "Invalid config entry.")
        return Result.Success(value)
    }
}