package com.maathisv.vigotrack.models

enum class ActivityType(val displayName: String, val category: ActivityCategory) {
    MARCHE("Marche", ActivityCategory.ACTIVITE),
    APA("APA", ActivityCategory.ACTIVITE),
    HIIT("HIIT", ActivityCategory.ACTIVITE),
    RENFORCEMENT("Renforcement", ActivityCategory.ACTIVITE),
    PISCINE("Piscine", ActivityCategory.ACTIVITE),
    TDM6("TDM6", ActivityCategory.BILAN),
    _10M("10m", ActivityCategory.BILAN),
    REPOS("Repos", ActivityCategory.BILAN)
}

enum class ActivityCategory(val displayName: String) {
    ACTIVITE("Activité"),
    BILAN("Bilan")
}
