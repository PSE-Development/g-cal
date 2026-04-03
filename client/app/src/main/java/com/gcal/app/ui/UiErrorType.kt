package com.gcal.app.ui.common

/**
 * UiErrorType
 *
 */
enum class UiErrorType(val message: String) {
    NETWORK_UNAVAILABLE("Keine Internetverbindung verfügbar"),
    TIMEOUT("Die Anfrage hat zu lange gedauert"),
    SERVER_ERROR("Ein Serverfehler ist aufgetreten"),
    VALIDATION("Ungültige Eingabe"),
    USER_NOT_FOUND("Benutzer nicht gefunden"),
    UNKNOWN("Ein unbekannter Fehler ist aufgetreten")
}