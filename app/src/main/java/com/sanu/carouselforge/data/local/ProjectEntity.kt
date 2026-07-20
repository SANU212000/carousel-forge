package com.sanu.carouselforge.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    @ColumnInfo(defaultValue = "1")
    val slideCount: Int = 1,
    @ColumnInfo(defaultValue = "4294967295")
    val bgColorStart: Long = 0xFFFFFFFFL,
    @ColumnInfo(defaultValue = "NULL")
    val bgColorEnd: Long? = null,
)
