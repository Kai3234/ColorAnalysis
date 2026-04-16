package com.example.coloranalysis.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.coloranalysis.data.models.Outfit
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {
    @Insert
    suspend fun insertOutfit(outfit: Outfit): Long

    // UPDATE outfit
    @Update
    suspend fun updateOutfit(outfit: Outfit)

    @Delete
    suspend fun deleteOutfit(outfit: Outfit)

    // Lấy danh sách outfit của 1 profile, sắp xếp theo thứ tự hiển thị (displayOrder)
    @Query("SELECT * FROM outfits WHERE profileId = :profileId ORDER BY displayOrder ASC")
    fun getOutfitsForProfile(profileId: Int): Flow<List<Outfit>>
}