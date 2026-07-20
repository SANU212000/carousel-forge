package com.sanu.carouselforge.core.text

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.sanu.carouselforge.R

/**
 * Curated Google Fonts offered in the text editor. Fonts are downloaded on demand via
 * Play Services; if resolution fails (offline / no Play Services) callers fall back to
 * the system default so text always renders.
 */
object FontCatalog {
    val fonts: List<String> = listOf(
        "Inter", "Poppins", "Montserrat", "Roboto", "Lato",
        "Oswald", "Raleway", "Playfair Display", "Merriweather", "Bebas Neue",
        "Nunito", "Work Sans", "Rubik", "Quicksand", "Lora",
        "PT Sans", "Josefin Sans", "Archivo", "Anton", "Pacifico",
        "Dancing Script", "Bitter", "Karla", "Manrope", "DM Sans",
    )

    private val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

    /** A Compose [FontFamily] (normal + bold) for the given font name, or null on failure. */
    fun fontFamily(name: String?): FontFamily? {
        val fontName = name?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val googleFont = GoogleFont(fontName)
            FontFamily(
                Font(googleFont = googleFont, fontProvider = provider, weight = FontWeight.Normal),
                Font(googleFont = googleFont, fontProvider = provider, weight = FontWeight.Bold),
            )
        }.getOrNull()
    }

    /**
     * Best-effort resolution of a platform [Typeface] for the export path so
     * StaticLayout draws the same font. Falls back to a weight-styled default.
     */
    fun typeface(context: Context, name: String?, bold: Boolean): Typeface {
        val fallback = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        val family = fontFamily(name) ?: return fallback
        return runCatching {
            val resolver = createFontFamilyResolver(context)
            val result = resolver.resolve(
                fontFamily = family,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.All,
            ).value as? Typeface
            result ?: fallback
        }.getOrDefault(fallback)
    }
}
