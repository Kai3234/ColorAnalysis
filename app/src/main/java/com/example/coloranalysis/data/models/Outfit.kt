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
            parentColumns = ["id"],          // Trỏ đến cột 'id' của bảng 'profiles'
            childColumns = ["profileId"],    // Liên kết với cột 'profileId' của bảng 'outfits'
            onDelete = ForeignKey.CASCADE    // Quan trọng: Nếu xóa Profile, toàn bộ Outfit của Profile đó sẽ tự động bị xóa
        )
    ],
    // Tạo index cho khóa ngoại giúp truy vấn (lấy danh sách quần áo của 1 profile) nhanh hơn rất nhiều
    indices = [Index("profileId")]
)
data class Outfit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "profileId")
    val profileId: Int,             // Khóa ngoại (Foreign Key)

    val outfitName: String,         // Tên bộ đồ (VD: "Đồ đi làm mùa đông", "Váy dạ hội")

    val displayOrder: Int = 0,      // Thứ tự hiển thị bộ đồ (0, 1, 2...)

    val mainColor: Int? = null,         // Màu chủ đạo
    val colorScheme: String? = null,    // Cách phối: "Monochrome", "Complementary", "Analogous"...

    // --- DỮ LIỆU MÀU SẮC BỘ QUẦN ÁO ---
    // Lưu dưới dạng mã màu Int (giống như skinColor, hairColor của bạn)
    val topColor: Int? = null,          // Màu áo / Áo liền quần
    val bottomColor: Int? = null,       // Màu quần / Chân váy
    val outerwearColor: Int? = null,    // Màu áo khoác ngoài (nếu có)
    val shoesColor: Int? = null,        // Màu giày
    val accessoryColor: Int? = null     // Màu phụ kiện (túi xách, khăn, mũ...)
)