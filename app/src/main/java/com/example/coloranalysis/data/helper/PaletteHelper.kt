package com.example.coloranalysis.data.helper

import android.content.Context
import com.example.coloranalysis.data.models.AvoidColorItem
import com.example.coloranalysis.data.models.ColorPaletteItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PaletteHelper {
    // Map từ Season Name (từ DB) sang tên file trong assets/palette/
    fun getFileName(season: String?): String? = when (season) {
        "Bright Spring" -> "spring_bright.json"
        "True Spring" -> "spring_true.json"
        "Light Spring" -> "spring_light.json"
        "Bright Winter" -> "winter_bright.json"
        "True Winter" -> "winter_true.json"
        "Dark Winter" -> "winter_dark.json"
        "Light Summer" -> "summer_light.json"
        "True Summer" -> "summer_true.json"
        "Soft Summer" -> "summer_soft.json"
        "Dark Autumn" -> "autumn_dark.json"
        "True Autumn" -> "autumn_true.json"
        "Soft Autumn" -> "autumn_soft.json"
        else -> null
    }

    val personalityMap = mapOf(
        "Bold" to "Mạnh mẽ",
        "Calm" to "Điềm tĩnh",
        "Playful" to "Năng động",
        "Sophisticated" to "Sang trọng"
    )

    val lifestyleMap = mapOf(
        "Professional" to "Công sở",
        "Creative" to "Sáng tạo",
        "Minimalist" to "Tối giản",
        "Active" to "Thể thao"
    )

    val allSubSeasons = listOf(
        "Bright Spring", "True Spring", "Light Spring",
        "Light Summer", "True Summer", "Soft Summer",
        "Soft Autumn", "True Autumn", "Dark Autumn",
        "Dark Winter", "True Winter", "Bright Winter"
    )

    fun loadPalette(context: Context, fileName: String): List<ColorPaletteItem> {
        return try {
            val jsonString = context.assets.open("palette/$fileName").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<ColorPaletteItem>>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadAvoidColors(context: Context): List<AvoidColorItem> {
        return try {
            val jsonString = context.assets.open("palette/extends/colors_avoid.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<AvoidColorItem>>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }
}