package com.maathisv.vigotrack.models

enum class ActivityType(val displayName: String, val category: ActivityCategory) {
    MARCHE("Marche", ActivityCategory.ACTIVITE),
    APA("APA", ActivityCategory.ACTIVITE),
    HIIT("HIIT", ActivityCategory.ACTIVITE),
    RENFORCEMENT("Renforcement", ActivityCategory.ACTIVITE),
    PISCINE("Piscine", ActivityCategory.ACTIVITE),
    TDM6("TDM6", ActivityCategory.BILAN),
    _10M_1("10m 1", ActivityCategory.BILAN),
    _10M_2("10m 2", ActivityCategory.BILAN),
    _10M_3("10m 3", ActivityCategory.BILAN),
    _10M_4("10m 4", ActivityCategory.BILAN),
    REPOS_1("Repos 1", ActivityCategory.BILAN),
    REPOS_2("Repos 2", ActivityCategory.BILAN)
}

enum class ActivityCategory(val displayName: String) {
    ACTIVITE("Activité"),
    BILAN("Bilan")
}
