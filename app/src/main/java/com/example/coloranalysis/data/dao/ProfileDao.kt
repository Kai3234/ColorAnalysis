package com.example.coloranalysis.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.coloranalysis.data.models.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    // Insert new profile
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    // Get all profiles sorted by newest first
    @Query("SELECT * FROM profiles ORDER BY dateCreated DESC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Query("SELECT * FROM profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: Int): Profile?

    @Query("UPDATE profiles SET imgOriginalUri = :uri WHERE id = :profileId")
    suspend fun updateImgOriginal(profileId: Int, uri: String)

    @Query("UPDATE profiles SET imgProcessedUri = :uri WHERE id = :profileId")
    suspend fun updateImgProcessed(profileId: Int, uri: String)


    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeProfile(id: Int): Flow<Profile?>

    @Query("""
        UPDATE profiles 
        SET skinColor = :skin, 
            hairColor = :hair, 
            eyeColor = :eye, 
            lipColor = :lip
        WHERE id = :id
    """)
    suspend fun updateColorResults(
        id: Int,
        skin: Int?,
        hair: Int?,
        eye: Int?,
        lip: Int?
    )
}