package com.sanu.carouselforge.features.editor.components

import android.content.Context
import com.sanu.carouselforge.R

/** Bundled starter sticker pack, referenced via `android.resource://` URIs so both the
 *  editor (Coil) and the export renderer can load them from the same source. */
object StickerCatalog {
    val stickers: List<Int> = listOf(
        R.drawable.sticker_star,
        R.drawable.sticker_heart,
        R.drawable.sticker_bolt,
        R.drawable.sticker_check,
        R.drawable.sticker_bubble,
        R.drawable.sticker_sparkle,
    )

    fun uri(context: Context, resId: Int): String =
        "android.resource://${context.packageName}/$resId"
}
