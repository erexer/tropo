package io.github.erexer.tropo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isSelected: Boolean = false
)