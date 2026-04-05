package com.example.coloranalysis.data

import androidx.compose.ui.graphics.Color

data class SeasonPalette(
    val name: String,
    val uiColor: Color,
    val description: String
)

object SeasonData {
    val palettes = mapOf(
        "True Spring" to SeasonPalette(
            name = "True Spring",
            uiColor = Color(0xFFFFB713),
            description = "Tông màu ấm áp, tươi sáng và rực rỡ."
        ),
        "Light Spring" to SeasonPalette(
            name = "Light Spring",
            uiColor = Color(0xFFF5E077),
            description = "Nhẹ nhàng, sáng và ấm áp."
        ),
        "Bright Spring" to SeasonPalette(
            name = "Bright Spring",
            uiColor = Color(0xFFE7324B),
            description = "Rất rực rỡ, tương phản cao và ấm áp."
        ),
        "True Summer" to SeasonPalette(
            name = "True Summer",
            uiColor = Color(0xFF218BAB),
            description = "Tông màu dịu mát, thanh lịch và nhẹ nhàng."
        ),
        "Light Summer" to SeasonPalette(
            name = "Light Summer",
            uiColor = Color(0xFF83CADF),
            description = "Rất sáng, mát mẻ và tinh tế."
        ),
        "Soft Summer" to SeasonPalette(
            name = "Soft Summer",
            uiColor = Color(0xFFE1B0C6),
            description = "Mờ ảo, trung tính và mát mẻ."
        ),
        "True Autumn" to SeasonPalette(
            name = "True Autumn",
            uiColor = Color(0xFF983A06),
            description = "Trầm ấm, đậm đà và mộc mạc."
        ),
        "Dark Autumn" to SeasonPalette(
            name = "Dark Autumn",
            uiColor = Color(0xFF35450D),
            description = "Sâu lắng, ấm áp và huyền bí."
        ),
        "Soft Autumn" to SeasonPalette(
            name = "Soft Autumn",
            uiColor = Color(0xFFD6B009),
            description = "Dịu nhẹ, ấm trung tính và cổ điển."
        ),
        "True Winter" to SeasonPalette(
            name = "True Winter",
            uiColor = Color(0xFF005E73),
            description = "Lạnh giá, sắc nét và quyền lực."
        ),
        "Dark Winter" to SeasonPalette(
            name = "Dark Winter",
            uiColor = Color(0xFF332486),
            description = "Sâu, lạnh và đậm nét."
        ),
        "Bright Winter" to SeasonPalette(
            name = "Bright Winter",
            uiColor = Color(0xFFC60679),
            description = "Trong trẻo, lạnh và rực rỡ."
        )
    )
}