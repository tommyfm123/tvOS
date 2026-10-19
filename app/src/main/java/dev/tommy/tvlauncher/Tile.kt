package dev.tommy.tvlauncher

import android.content.Intent
import android.graphics.drawable.Drawable

const val HIDDEN_ITEMS_KEY = "internal:hidden-items"

enum class TileKind { APP, PORT, SHORTCUT }

data class Tile(
    val key: String,
    val kind: TileKind,
    val label: String,
    val launchIntent: Intent?,
    val tileImage: Drawable,
    val hasBanner: Boolean,
    val packageName: String? = null,
    val canUninstall: Boolean = false,
)

data class HomeContent(
    val homeTiles: List<Tile>,
    val appTiles: List<Tile>,
    val shortcutTiles: List<Tile>,
    val hiddenTiles: List<Tile>,
)
