package dev.tommy.tvlauncher

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File
import kotlin.math.max

private const val ART_DIRECTORY = "art"
private const val PRESET_PREFIX = "preset:"
private const val FILE_PREFIX = "file:"
private const val PLAYSTATION_PRESET = "playstation"
private const val XBOX_PRESET = "xbox"
private const val HBO_MAX_PRESET = "hbomax"
private const val SPOTIFY_PRESET = "spotify"
private const val NETFLIX_PRESET = "netflix"
private const val NETFLIX_PACKAGE = "com.netflix.ninja"
private const val HBO_MAX_PACKAGE = "com.wbd.stream"
private const val SPOTIFY_PACKAGE = "com.spotify.tv.android"
private const val MAX_ART_WIDTH = 768
private const val MIN_SAMPLE_SIZE = 1
private val ART_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

data class ArtOption(val id: String, val label: String)

private data class ArtPreset(val id: String, val labelRes: Int, val drawableRes: Int)

class ArtRepository(private val context: Context) {
    private val presets = listOf(
        ArtPreset(PLAYSTATION_PRESET, R.string.art_playstation, R.drawable.art_playstation),
        ArtPreset(XBOX_PRESET, R.string.art_xbox, R.drawable.art_xbox),
        ArtPreset(HBO_MAX_PRESET, R.string.art_hbo_max, R.drawable.art_hbo_max),
        ArtPreset(SPOTIFY_PRESET, R.string.art_spotify, R.drawable.art_spotify),
        ArtPreset(NETFLIX_PRESET, R.string.art_netflix, R.drawable.art_netflix),
    )
    private val defaultArtByPackage = mapOf(
        NETFLIX_PACKAGE to PRESET_PREFIX + NETFLIX_PRESET,
        HBO_MAX_PACKAGE to PRESET_PREFIX + HBO_MAX_PRESET,
        SPOTIFY_PACKAGE to PRESET_PREFIX + SPOTIFY_PRESET,
    )

    fun options(): List<ArtOption> = presetOptions() + fileOptions()

    fun defaultArtIdFor(packageName: String): String? = defaultArtByPackage[packageName]

    fun load(artId: String): Drawable? = when {
        artId.startsWith(PRESET_PREFIX) -> loadPreset(artId.removePrefix(PRESET_PREFIX))
        artId.startsWith(FILE_PREFIX) -> loadFile(artId.removePrefix(FILE_PREFIX))
        else -> null
    }

    private fun presetOptions() = presets.map { ArtOption(PRESET_PREFIX + it.id, context.getString(it.labelRes)) }

    private fun fileOptions() = artFiles().map { ArtOption(FILE_PREFIX + it.name, it.nameWithoutExtension) }

    private fun artFiles(): List<File> = context.getExternalFilesDir(ART_DIRECTORY)
        ?.listFiles { file -> file.extension.lowercase() in ART_EXTENSIONS }
        .orEmpty()
        .sortedBy { it.name }

    private fun loadPreset(presetId: String): Drawable? =
        presets.firstOrNull { it.id == presetId }?.let { context.getDrawable(it.drawableRes) }

    private fun loadFile(fileName: String): Drawable? {
        val file = artFiles().firstOrNull { it.name == fileName } ?: return null
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, boundsOptions)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = max(MIN_SAMPLE_SIZE, boundsOptions.outWidth / MAX_ART_WIDTH)
        }
        val bitmap = BitmapFactory.decodeFile(file.path, decodeOptions) ?: return null
        return BitmapDrawable(context.resources, bitmap)
    }
}
