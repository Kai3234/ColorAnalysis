package com.example.coloranalysis.data.helper

import com.example.coloranalysis.data.models.Outfit

object ColorHarmonyHelper {

    val SCHEMES = listOf(
        "Đơn sắc",
        "Tương phản",
        "Liền kề",
        "Tam giác"
    )

    fun autoFillOutfit(outfit: Outfit): Outfit {
        val base = outfit.mainColor ?: return outfit
        val scheme = outfit.colorScheme ?: return outfit

        return when (scheme) {
            "Đơn sắc" -> monochrome(base, outfit)
            "Tương phản" -> complementary(base, outfit)
            "Liền kề" -> analogous(base, outfit)
            "Tam giác" -> triadic(base, outfit)
            else -> outfit
        }
    }

    // 1. ĐƠN SẮC (MONOCHROME)
    private fun monochrome(base: Int, o: Outfit): Outfit {
        return o.copy(
            topColor = base,
            bottomColor = darkenHue(base),   // Quần: Đậm và tối nhất
            outerwearColor = softenHue(base),
            // Giày: Vì là đơn sắc (chỉ 1 màu), ta làm giày Sáng & Rực hơn quần (Tầm trung) để tách biệt rõ rệt.
            shoesColor = richHue(base),
            accessoryColor = popHue(base)
        )
    }

    // 2. TƯƠNG PHẢN (COMPLEMENTARY)
    private fun complementary(base: Int, o: Outfit): Outfit {
        val comp = rotateHue(base, 180f)

        return o.copy(
            topColor = base,
            bottomColor = darkenHue(comp),
            outerwearColor = softenHue(comp),
            shoesColor = darkenHue(base),
            accessoryColor = popHue(comp)
        )
    }

    // 3. LIỀN KỀ (ANALOGOUS)
    private fun analogous(base: Int, o: Outfit): Outfit {
        val left = rotateHue(base, -30f)
        val right = rotateHue(base, 30f)

        return o.copy(
            topColor = base,
            bottomColor = darkenHue(left),
            outerwearColor = softenHue(right),
            shoesColor = darkenHue(right),
            accessoryColor = popHue(right)
        )
    }

    // 4. TAM GIÁC (TRIADIC)
    private fun triadic(base: Int, o: Outfit): Outfit {
        val c1 = rotateHue(base, 120f)
        val c2 = rotateHue(base, 240f)

        return o.copy(
            topColor = base,
            bottomColor = darkenHue(c1),
            outerwearColor = softenHue(c2),
            shoesColor = darkenHue(c2),
            accessoryColor = popHue(c2)
        )
    }

    // =====================================================
    // Thuật toán
    // =====================================================

    private fun rotateHue(color: Int, degree: Float): Int {
        val hsv = hsv(color)
        hsv[0] = (hsv[0] + degree + 360f) % 360f
        return android.graphics.Color.HSVToColor(hsv)
    }

    // QUẦN: Tối thẫm
    private fun darkenHue(color: Int): Int {
        val hsv = hsv(color)
        hsv[1] = hsv[1].coerceAtLeast(0.5f)
        hsv[2] = (hsv[2] * 0.4f).coerceIn(0.2f, 0.4f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    // GIÀY ĐƠN SẮC: Tone màu tầm trung (Mid-tone), sáng hơn quần, tối hơn áo
    private fun richHue(color: Int): Int {
        val hsv = hsv(color)
        hsv[1] = hsv[1].coerceAtLeast(0.7f)
        hsv[2] = (hsv[2] * 0.6f).coerceIn(0.45f, 0.65f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    // ÁO KHOÁC: Phấn / Pastel
    private fun softenHue(color: Int): Int {
        val hsv = hsv(color)
        hsv[1] = (hsv[1] * 0.5f).coerceIn(0.2f, 0.5f)
        hsv[2] = (hsv[2] * 1.3f).coerceIn(0.8f, 1.0f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    // PHỤ KIỆN: Rực rỡ nhất
    private fun popHue(color: Int): Int {
        val hsv = hsv(color)
        hsv[1] = (hsv[1] * 1.3f).coerceIn(0.7f, 1.0f)
        hsv[2] = (hsv[2] * 1.2f).coerceIn(0.8f, 1.0f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun hsv(color: Int): FloatArray {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        return hsv
    }
}