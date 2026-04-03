package com.gcal.app.model.modelData.data.achievements

/**
 * Defines the types of achievements available in the application by name and description.
 */
enum class AchievementType(val achievementName: String, val description: String) {
    EARLY_BIRD("Früher Vogel", "Erledige drei ToDo zwischen fünf und zehn Uhr morgens!"),
    NIGHT_OWL("Nachteule", "Erledige drei ToDo nach null und vor fünf Uhr!");

    companion object {
        /**
         * Returns the AchievementType matching the given display name.
         * @throws NoSuchElementException if no match is found.
         */
        fun fromDisplayName(name: String): AchievementType {
            return entries.first { it.achievementName == name }
        }
    }
}