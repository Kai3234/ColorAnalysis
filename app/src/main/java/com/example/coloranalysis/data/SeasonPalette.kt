package com.example.coloranalysis.data

import androidx.compose.ui.graphics.Color

data class SeasonPalette(
    val name: String,
    val uiColor: Color,
    val wowColors: List<Color>, // Representative color for the season
    val description: String
)

object SeasonData {
    val palettes = mapOf(
        "True Spring" to SeasonPalette(
            name = "True Spring",
            uiColor = Color(0xFFFFB713),
            wowColors = listOf(
                Color(0xFFB5D165), Color(0xFFE5989B), Color(0xFFF9DC5C), Color(0xFFCB997E),
                Color(0xFFFFB7B2), Color(0xFFFFDAC1), Color(0xFFB2C2F2), Color(0xFF8FD3F4)
            ),
            description = "Tông màu ấm áp, tươi sáng và rực rỡ."
        ),
        "Light Spring" to SeasonPalette(
            name = "Light Spring",
            uiColor = Color(0xFFF5E077),
            wowColors = listOf(
                Color(0xFF5E548E), Color(0xFFE5383B), Color(0xFF9F86C0), Color(0xFFFF8B94),
                Color(0xFFD4A373), Color(0xFFE9C46A), Color(0xFF48CAE4), Color(0xFFF15BB5)
            ),
            description = "Nhẹ nhàng, sáng và ấm áp."
        ),
        "Bright Spring" to SeasonPalette(
            name = "Bright Spring",
            uiColor = Color(0xFFE7324B),
            wowColors = listOf(
                Color(0xFF9EF01A), Color(0xFFE9C46A), Color(0xFF38B000), Color(0xFFBC8A5F),
                Color(0xFF4361EE), Color(0xFF8338EC), Color(0xFF005F73), Color(0xFFF94144)
            ),
            description = "Rất rực rỡ, tương phản cao và ấm áp."
        ),
        "True Summer" to SeasonPalette(
            name = "True Summer",
            uiColor = Color(0xFF218BAB),
            wowColors = listOf(
                Color(0xFFD81B60), Color(0xFF7209B7), Color(0xFF9A8C98), Color(0xFF432818),
                Color(0xFF8ECAE6), Color(0xFF219EBC), Color(0xFFF48C06), Color(0xFF023047)
            ),
            description = "Tông màu dịu mát, thanh lịch và nhẹ nhàng."
        ),
        "Light Summer" to SeasonPalette(
            name = "Light Summer",
            uiColor = Color(0xFF83CADF),
            wowColors = listOf(
                Color(0xFFF497AD), Color(0xFFFAD2E1), Color(0xFFBDB2FF), Color(0xFFCCFFFE),
                Color(0xFFB7E4C7), Color(0xFFFDE2E4), Color(0xFF6495ED), Color(0xFF006D77)
            ),
            description = "Rất sáng, mát mẻ và tinh tế."
        ),
        "Soft Summer" to SeasonPalette(
            name = "Soft Summer",
            uiColor = Color(0xFFE1B0C6),
            wowColors = listOf(
                Color(0xFF6D597A), Color(0xFFB56576), Color(0xFFADB5BD), Color(0xFFCB997E),
                Color(0xFF83C5BE), Color(0xFF006D77), Color(0xFF3D5A80), Color(0xFF98C1D9)
            ),
            description = "Mờ ảo, trung tính và mát mẻ."
        ),
        "True Autumn" to SeasonPalette(
            name = "True Autumn",
            uiColor = Color(0xFF983A06),
            wowColors = listOf(
                Color(0xFF457B9D), Color(0xFF1D3557), Color(0xFF588157), Color(0xFFA44A3F),
                Color(0xFF606C38), Color(0xFFD4A373), Color(0xFFBC6C25), Color(0xFF8B4513)),
            description = "Trầm ấm, đậm đà và mộc mạc."
        ),
        "Dark Autumn" to SeasonPalette(
            name = "Dark Autumn",
            uiColor = Color(0xFF35450D),
            wowColors = listOf(
                Color(0xFF134E5E), Color(0xFFF1E3D3), Color(0xFF0D1B2A), Color(0xFF6F1D1B),
                Color(0xFF1B4332), Color(0xFF310D20), Color(0xFF2D6A4F), Color(0xFF432818)
            ),
            description = "Sâu lắng, ấm áp và huyền bí."
        ),
        "Soft Autumn" to SeasonPalette(
            name = "Soft Autumn",
            uiColor = Color(0xFFD6B009),
            wowColors = listOf(
                Color(0xFFF5EBE0), Color(0xFFD4A373), Color(0xFFBC8A5F), Color(0xFF003049),
                Color(0xFF6B705C), Color(0xFFA5A58D), Color(0xFF588157), Color(0xFF6D597A)
            ),
            description = "Dịu nhẹ, ấm trung tính và cổ điển."
        ),
        "True Winter" to SeasonPalette(
            name = "True Winter",
            uiColor = Color(0xFF005E73),
            wowColors = listOf(
                Color(0xFFD00000), Color(0xFF9D0208), Color(0xFF48CAE4), Color(0xFF0077B6),
                Color(0xFF5A189A), Color(0xFF1B4332), Color(0xFFADB5BD), Color(0xFF495057)),
            description = "Lạnh giá, sắc nét và quyền lực."
        ),
        "Dark Winter" to SeasonPalette(
            name = "Dark Winter",
            uiColor = Color(0xFF332486),
            wowColors = listOf(
                Color(0xFF660708), Color(0xFF370617), Color(0xFF2D0040), Color(0xFF03071E),
                Color(0xFF001219), Color(0xFF005F73), Color(0xFF0A9396), Color(0xFF212529)
            ),
            description = "Sâu, lạnh và đậm nét."
        ),
        "Bright Winter" to SeasonPalette(
            name = "Bright Winter",
            uiColor = Color(0xFFC60679),
            wowColors = listOf(
                Color(0xFF7209B7), Color(0xFF3A0CA3), Color(0xFF4895EF), Color(0xFF600020),
                Color(0xFFF72585), Color(0xFF00F5D4), Color(0xFFF15BB5), Color(0xFF00BBF9)),
            description = "Trong trẻo, lạnh và rực rỡ."
        )
    )
}