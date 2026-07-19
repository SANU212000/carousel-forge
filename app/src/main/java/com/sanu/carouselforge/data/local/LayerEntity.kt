package com.sanu.carouselforge.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "layers",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["projectId", "zIndex"], unique = true),
    ],
)
data class LayerEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val type: String,
    val imageUri: String?,
    val text: String?,
    val x: Float,
    val y: Float,
    @ColumnInfo(defaultValue = "486")
    val width: Float,
    @ColumnInfo(defaultValue = "486")
    val height: Float,
    val scale: Float,
    val rotation: Float,
    val zIndex: Int,
)
