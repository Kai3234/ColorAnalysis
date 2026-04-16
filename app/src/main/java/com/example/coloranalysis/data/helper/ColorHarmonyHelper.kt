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

    // ---------- MONO ----------
    private fun monochrome(base: Int, o: Outfit): Outfit {

        return o.copy(
            topColor = boost(base, sat = 1.05f, value = 1.0f),      // hero
            bottomColor = neutralize(base, 0.75f),
            outerwearColor = neutralize(base, 0.55f),
            shoesColor = darken(base, 0.45f),
            accessoryColor = lighten(base, 1.25f)                   // accent
        )
    }

    // ---------- COMPLEMENTARY ----------
    private fun complementary(base: Int, o: Outfit): Outfit {

        val comp = rotateHue(base, 180f)

        return o.copy(
            topColor = base,
            bottomColor = neutralize(comp, 0.8f),
            outerwearColor = neutralize(base, 0.5f),
            shoesColor = darken(comp, 0.5f),
            accessoryColor = boost(comp, 1.1f, 1.1f)
        )
    }

    // ---------- ANALOGOUS ----------
    private fun analogous(base: Int, o: Outfit): Outfit {

        val left = rotateHue(base, -25f)
        val right = rotateHue(base, 25f)

        return o.copy(
            topColor = base,
            bottomColor = neutralize(left, 0.85f),
            outerwearColor = neutralize(right, 0.65f),
            shoesColor = darken(base, 0.45f),
            accessoryColor = boost(right, 1.15f, 1.1f)
        )
    }

    // ---------- TRIADIC ----------
    private fun triadic(base: Int, o: Outfit): Outfit {

        val c1 = rotateHue(base, 120f)
        val c2 = rotateHue(base, 240f)

        return o.copy(
            topColor = base,
            bottomColor = neutralize(c1, 0.8f),
            outerwearColor = neutralize(c2, 0.6f),
            shoesColor = darken(base, 0.45f),
            accessoryColor = boost(c1, 1.2f, 1.15f)
        )
    }

    // =====================================================
    // COLOR OPERATIONS (Fashion tuned)
    // =====================================================

    fun rotateHue(color: Int, degree: Float): Int {
        val hsv = hsv(color)
        hsv[0] = (hsv[0] + degree + 360f) % 360f
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun darken(color: Int, factor: Float): Int {
        val hsv = hsv(color)
        hsv[2] *= factor
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun lighten(color: Int, factor: Float): Int {
        val hsv = hsv(color)
        hsv[2] = (hsv[2] * factor).coerceIn(0f,1f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun neutralize(color: Int, satFactor: Float): Int {
        val hsv = hsv(color)
        hsv[1] *= satFactor   // giảm độ gắt
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun boost(color: Int, sat: Float, value: Float): Int {
        val hsv = hsv(color)
        hsv[1] = (hsv[1] * sat).coerceIn(0f,1f)
        hsv[2] = (hsv[2] * value).coerceIn(0f,1f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun hsv(color: Int): FloatArray {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        return hsv
    }
}