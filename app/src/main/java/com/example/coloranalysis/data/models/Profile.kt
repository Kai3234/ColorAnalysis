package com.example.coloranalysis.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val profileName: String,

    val dateCreated: Long,

    val imgOriginalUri: String? = null,

    val imgProcessedUri: String? = null,

    val skinColor: Int? = null,

    val hairColor: Int? = null,

    val eyeColor: Int? = null,

    val lipColor: Int? = null,


    val hueScore: Float? = null,
    val chromaScore: Float? = null,
    val valueScore: Float? = null,

    val seasonType: String? = null
)