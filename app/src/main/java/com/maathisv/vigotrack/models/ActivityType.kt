package com.maathisv.vigotrack.models

data class ActivityType(
    val name: String,
    val displayName: String,
    val category: ActivityCategory
) {
    companion object {
        val MARCHE = ActivityType("MARCHE", "Marche", ActivityCategory.ACTIVITE)
        val APA = ActivityType("APA", "APA", ActivityCategory.ACTIVITE)
        val HIIT = ActivityType("HIIT", "HIIT", ActivityCategory.ACTIVITE)
        val RENFORCEMENT = ActivityType("RENFORCEMENT", "Renforcement", ActivityCategory.ACTIVITE)
        val PISCINE = ActivityType("PISCINE", "Piscine", ActivityCategory.ACTIVITE)
        val TDM6 = ActivityType("TDM6", "TDM6", ActivityCategory.BILAN)
        val _10M_1 = ActivityType("_10M_1", "10m 1", ActivityCategory.BILAN)
        val _10M_2 = ActivityType("_10M_2", "10m 2", ActivityCategory.BILAN)
        val _10M_3 = ActivityType("_10M_3", "10m 3", ActivityCategory.BILAN)
        val _10M_4 = ActivityType("_10M_4", "10m 4", ActivityCategory.BILAN)
        val REPOS_1 = ActivityType("REPOS_1", "Repos 1", ActivityCategory.BILAN)
        val REPOS_2 = ActivityType("REPOS_2", "Repos 2", ActivityCategory.BILAN)

        val entries: List<ActivityType> = listOf(
            MARCHE, APA, HIIT, RENFORCEMENT, PISCINE,
            TDM6, _10M_1, _10M_2, _10M_3, _10M_4, REPOS_1, REPOS_2
        )

        fun fromName(name: String): ActivityType? =
            entries.find { it.name == name }
    }
}

enum class ActivityCategory(val displayName: String) {
    ACTIVITE("Activité"),
    BILAN("Bilan")
}
