package com.maathisv.vigotrack.data

import androidx.room.Embedded
import androidx.room.Relation
import com.maathisv.vigotrack.data.entities.ActivityLinkEntity
import com.maathisv.vigotrack.data.entities.ActivitySessionEntity


data class ActivityWithLinks(
    @Embedded val activity: ActivitySessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentActivityId"
    )
    val links: List<ActivityLinkEntity>
)