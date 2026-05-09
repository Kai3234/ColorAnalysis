package com.example.coloranalysis.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outfits",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class Outfit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "profileId")
    val profileId: Int,

    val outfitName: String,

    val displayOrder: Int = 0,

    val mainColor: Int? = null,         // Màu chủ đạo
    val colorScheme: String? = null,    // Cách phối: "Monochrome", "Complementary", "Analogous", "Triadic"

    // Dữ liệu màu sắc bộ quần áo
    val topColor: Int? = null,          // Màu áo
    val bottomColor: Int? = null,       // Màu quần / Chân váy
    val outerwearColor: Int? = null,    // Màu áo khoác ngoài
    val shoesColor: Int? = null,        // Màu giày
    val accessoryColor: Int? = null     // Màu phụ kiện (túi xách, khăn, mũ...)
)