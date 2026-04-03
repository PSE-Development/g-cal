package com.gcal.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.modelData.ModelData
import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.ui.GCalApp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Single-Activity entry point for the GCal application.
 *
 */
class MainActivity : AppCompatActivity() {


    private lateinit var model: ModelFacade

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


         // default language is German
        val currentLocales = AppCompatDelegate.getApplicationLocales()
             if (currentLocales.isEmpty) {
                      AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags("de")
                        )
                    }

        model = createModelFacade()


        enableEdgeToEdge()

        setContent {

            GCalApp(model = model)
        }
    }

    /**
     * Assembles the [ModelFacade] from its infrastructure components.
     *
     * @return The [ModelFacade] instance.
     */
    private fun createModelFacade(): ModelFacade {

        val db = LocalData.getDatabase(applicationContext)


        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }


        val api = ClientAPI(
            domain = "http://193.196.39.173:8080",
            client = httpClient
        )


        return ModelData(api, db)
    }
}