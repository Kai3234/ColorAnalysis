package com.example.coloranalysis.data.models

data class ColorPaletteItem(
    val hex: String,
    val name: String,
    val subseason: String,
    val personality: List<String>,
    val lifestyle: List<String>
)

