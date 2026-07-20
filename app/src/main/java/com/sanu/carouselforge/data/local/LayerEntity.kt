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
    @ColumnInfo(defaultValue = "4278190080")
    val textColor: Long = 0xFF000000L,
    @ColumnInfo(defaultValue = "32")
    val textSizeSp: Float = 32f,
    @ColumnInfo(defaultValue = "400")
    val fontWeight: Int = 400,
    @ColumnInfo(defaultValue = "CENTER")
    val textAlign: String = "CENTER",
    @ColumnInfo(defaultValue = "NULL")
    val fontFamily: String? = null,
    @ColumnInfo(defaultValue = "1")
    val alpha: Float = 1f,
    @ColumnInfo(defaultValue = "0")
    val cornerRadius: Float = 0f,
    @ColumnInfo(defaultValue = "0")
    val hasShadow: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val cropLeft: Float = 0f,
    @ColumnInfo(defaultValue = "0")
    val cropTop: Float = 0f,
    @ColumnInfo(defaultValue = "1")
    val cropRight: Float = 1f,
    @ColumnInfo(defaultValue = "1")
    val cropBottom: Float = 1f,
    @ColumnInfo(defaultValue = "0")
    val brightness: Float = 0f,
    @ColumnInfo(defaultValue = "1")
    val contrast: Float = 1f,
    @ColumnInfo(defaultValue = "1")
    val saturation: Float = 1f,
    @ColumnInfo(defaultValue = "NULL")
    val filterPreset: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val shapeKind: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val fillColor: Long? = null,
)
